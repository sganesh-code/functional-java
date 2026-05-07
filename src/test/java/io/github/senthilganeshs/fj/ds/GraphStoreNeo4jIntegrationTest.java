package io.github.senthilganeshs.fj.ds;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.utility.DockerImageName;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.SkipException;

public class GraphStoreNeo4jIntegrationTest {
    private static final DockerImageName NEO4J_IMAGE = DockerImageName.parse("neo4j:5.23-community");

    private Neo4jContainer<?> neo4j;
    private Driver driver;
    private GraphStore<String> store;

    @BeforeClass
    public void setUp() {
        if (!DockerClientFactory.instance().isDockerAvailable()) {
            throw new SkipException("Docker is not available; skipping Neo4j integration test.");
        }
        neo4j = new Neo4jContainer<>(NEO4J_IMAGE).withoutAuthentication();
        neo4j.start();
        driver = GraphDatabase.driver(neo4j.getBoltUrl(), AuthTokens.none());
        store = new Neo4jGraphStore(driver);
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        if (driver != null) {
            driver.close();
        }
        if (neo4j != null) {
            neo4j.stop();
        }
    }

    @Test
    public void storeLoadsGraphAsPureFunctionalValue() {
        Graph<String> source = Graph.<String>nil()
            .addEdge("Clean", "Compile")
            .addEdge("Compile", "Test")
            .addEdge("Compile", "Package")
            .addEdge("Test", "Deploy")
            .addEdge("Package", "Deploy")
            .addEdge("Deploy", "Notify");

        Either<GraphStoreError, Void> saved = GraphStores.save(source)
            .run(store)
            .run();
        Assert.assertTrue(saved.isRight(), saved.toString());

        Either<GraphStoreError, Graph<String>> loaded = GraphStores.load(GraphQuery.root("Compile", 2))
            .run(store)
            .run();

        Assert.assertTrue(loaded.isRight(), loaded.toString());

        Graph<String> graph = loaded.orElse(Graph.nil());
        Assert.assertTrue(graph.nodes().contains("Compile"));
        Assert.assertTrue(graph.nodes().contains("Test"));
        Assert.assertTrue(graph.nodes().contains("Package"));
        Assert.assertTrue(graph.nodes().contains("Deploy"));
        Assert.assertFalse(graph.nodes().contains("Notify"));

        Maybe<List<String>> topo = graph.topologicalSort();
        Assert.assertTrue(topo.isSome());
        Assert.assertEquals(topo.orElse(List.nil()).head().orElse(""), "Compile");

        List<String> bfs = List.from(graph.bfs("Compile"));
        Assert.assertTrue(bfs.contains("Compile"));
        Assert.assertTrue(bfs.contains("Test"));
        Assert.assertTrue(bfs.contains("Package"));
    }

    private static final class Neo4jGraphStore implements GraphStore<String> {
        private static final String NODE_LABEL = "GraphNode";
        private static final String EDGE_TYPE = "LINKS_TO";
        private final Driver driver;

        private Neo4jGraphStore(Driver driver) {
            this.driver = driver;
        }

        @Override
        public TaskEither<GraphStoreError, Graph<String>> load(GraphQuery<String> query) {
            return TaskEither.of(Task.of(() -> {
                try {
                    return Either.right(loadInternal(query));
                } catch (Throwable t) {
                    return Either.left(GraphStoreError.of("failed to load graph", t));
                }
            }));
        }

        @Override
        public TaskEither<GraphStoreError, Void> save(Graph<String> graph) {
            return TaskEither.of(Task.of(() -> {
                try {
                    saveInternal(graph);
                    return Either.right(null);
                } catch (Throwable t) {
                    return Either.left(GraphStoreError.of("failed to save graph", t));
                }
            }));
        }

        private void saveInternal(Graph<String> graph) {
            try (Session session = driver.session()) {
                session.executeWrite(tx -> {
                    tx.run("MATCH (n:" + NODE_LABEL + ") DETACH DELETE n");

                    graph.vertices().forEach(v ->
                        tx.run("MERGE (n:" + NODE_LABEL + " {id: $id})",
                            java.util.Map.of("id", v))
                    );

                    graph.vertices().forEach(source ->
                        graph.neighbors(source).forEach(target ->
                            tx.run(
                                "MERGE (a:" + NODE_LABEL + " {id: $source}) " +
                                "MERGE (b:" + NODE_LABEL + " {id: $target}) " +
                                "MERGE (a)-[:" + EDGE_TYPE + "]->(b)",
                                java.util.Map.of("source", source, "target", target))
                        )
                    );
                    return null;
                });
            }
        }

        private Graph<String> loadInternal(GraphQuery<String> query) {
            Graph<String> graph = loadAll();
            if (query.root().isNothing()) {
                return graph;
            }

            String root = query.root().orElse(null);
            int depth = query.depth().orElse(Integer.MAX_VALUE);
            java.util.Set<String> allowed = reachableWithinDepth(graph, root, depth);
            return inducedSubgraph(graph, allowed);
        }

        private Graph<String> loadAll() {
            AtomicReference<Graph<String>> graphRef = new AtomicReference<>(Graph.nil());
            try (Session session = driver.session()) {
                session.executeRead(tx -> {
                    tx.run("MATCH (n:" + NODE_LABEL + ") RETURN n.id AS id")
                        .forEachRemaining(record -> {
                            String id = record.get("id").asString();
                            graphRef.set(graphRef.get().addVertex(id));
                        });

                    tx.run(
                        "MATCH (a:" + NODE_LABEL + ")-[:" + EDGE_TYPE + "]->(b:" + NODE_LABEL + ") " +
                        "RETURN a.id AS source, b.id AS target"
                    ).forEachRemaining(record -> {
                        String source = record.get("source").asString();
                        String target = record.get("target").asString();
                        graphRef.set(graphRef.get().addEdge(source, target));
                    });
                    return null;
                });
            }
            return graphRef.get();
        }

        private java.util.Set<String> reachableWithinDepth(Graph<String> graph, String root, int maxDepth) {
            java.util.Set<String> visited = new HashSet<>();
            ArrayDeque<Tuple<String, Integer>> queue = new ArrayDeque<>();
            queue.add(Tuple.of(root, 0));

            while (!queue.isEmpty()) {
                Tuple<String, Integer> current = queue.removeFirst();
                String vertex = current.getA().orElse(null);
                int depth = current.getB().orElse(0);

                if (!visited.add(vertex) || depth > maxDepth) {
                    continue;
                }

                if (depth == maxDepth) {
                    continue;
                }

                graph.neighbors(vertex).forEach(next -> queue.add(Tuple.of(next, depth + 1)));
            }

            return visited;
        }

        private Graph<String> inducedSubgraph(Graph<String> graph, java.util.Set<String> allowed) {
            Graph<String> result = Graph.nil();
            for (String vertex : allowed) {
                result = result.addVertex(vertex);
            }

            for (String source : allowed) {
                for (String target : graph.neighbors(source)) {
                    if (allowed.contains(target)) {
                        result = result.addEdge(source, target);
                    }
                }
            }

            return result;
        }
    }
}
