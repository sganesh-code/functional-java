package io.github.senthilganeshs.fj.ds;

import org.testng.Assert;
import org.testng.annotations.Test;

public class GraphTest {

    @Test
    public void testGraphBasic() {
        Graph<String> g = Graph.<String>nil()
            .addEdge("A", "B")
            .addEdge("B", "C");
            
        Assert.assertEquals(g.nodes().length(), 3);
        Assert.assertTrue(g.nodes().contains("A"));
        Assert.assertTrue(g.nodes().contains("B"));
        Assert.assertTrue(g.nodes().contains("C"));
        
        Assert.assertTrue(g.successors("A").contains("B"));
        Assert.assertTrue(g.successors("B").contains("C"));
        Assert.assertTrue(g.successors("C").length() == 0);
    }

    @Test
    public void testGraphDisconnected() {
        Graph<String> g = Graph.<String>nil()
            .addEdge("A", "B")
            .addEdge("C", "D");
            
        Assert.assertEquals(g.nodes().length(), 4);
        Assert.assertEquals(g.bfs("A").length(), 2);
        Assert.assertEquals(g.bfs("C").length(), 2);
    }

    @Test
    public void testGraphBFS() {
        Graph<String> g = Graph.<String>nil()
            .addEdge("A", "B")
            .addEdge("A", "C")
            .addEdge("B", "D")
            .addEdge("C", "D");
            
        List<String> bfs = g.bfs("A");
        Assert.assertEquals(bfs.length(), 4);
        // BFS order should have A first, then B or C, then D
        Assert.assertEquals(bfs.find(i -> true).fromMaybe(""), "A");
    }

    @Test
    public void testGraphDFS() {
        Graph<String> g = Graph.<String>nil()
            .addEdge("A", "B")
            .addEdge("A", "C")
            .addEdge("B", "D")
            .addEdge("C", "D");
            
        List<String> dfs = g.dfs("A");
        Assert.assertEquals(dfs.length(), 4);
        Assert.assertEquals(dfs.find(i -> true).fromMaybe(""), "A");
    }

    @Test
    public void testGraphTopologicalSort() {
        Graph<String> g = Graph.<String>nil()
            .addEdge("A", "B")
            .addEdge("A", "C")
            .addEdge("B", "D")
            .addEdge("C", "D");
            
        Maybe<List<String>> topo = g.topologicalSort();
        Assert.assertTrue(topo.isSome());
        List<String> sorted = topo.fromMaybe(List.nil());
        Assert.assertEquals(sorted.length(), 4);
        Assert.assertEquals(sorted.find(i -> true).fromMaybe(""), "A");
    }

    @Test
    public void testGraphCyclic() {
        Graph<String> g = Graph.<String>nil()
            .addEdge("A", "B")
            .addEdge("B", "C")
            .addEdge("C", "A");
            
        Assert.assertTrue(g.topologicalSort().isNothing());
        Assert.assertEquals(g.bfs("A").length(), 3);
    }

    @Test
    public void testGraphEmptyNew() {
        Graph<String> emptyGraph = Graph.nil();
        Assert.assertEquals(emptyGraph.toString(), "Graph{}");
    }

    @Test
    public void testGraphSelfLoop() {
        Graph<String> g = Graph.<String>nil().addEdge("A", "A");
        Assert.assertEquals(g.nodes().length(), 1);
        Assert.assertTrue(g.topologicalSort().isNothing());
    }

    @Test
    public void testGraphCollectionAPIs() {
        Graph<String> g = Graph.<String>nil().addNode("A").addNode("B");
        // foldl on Graph yields nodes
        int count = g.foldl(0, (acc, v) -> acc + 1);
        Assert.assertEquals(count, 2);
    }
}
