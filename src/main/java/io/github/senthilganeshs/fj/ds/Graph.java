package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.typeclass.Ord;
import java.util.function.BiFunction;

/**
 * Functional Graph implementation using adjacency maps.
 */
public interface Graph<V extends Comparable<V>> {

    static <V extends Comparable<V>> Graph<V> nil() {
        return new AdjacencyGraph<V>(HashMap.<V, Set<V>>nil());
    }

    Graph<V> addVertex(V vertex);
    
    default Graph<V> addNode(V vertex) {
        return addVertex(vertex);
    }

    Graph<V> addEdge(V from, V to);
    
    Collection<V> neighbors(V vertex);
    
    default Collection<V> successors(V vertex) {
        return neighbors(vertex);
    }

    Collection<V> vertices();
    
    default Collection<V> nodes() {
        return vertices();
    }

    default int length() {
        return vertices().length();
    }

    default int count() {
        return length();
    }

    /**
     * Breadth-First Search (BFS) traversal.
     */
    default Collection<V> bfs(V start) {
        Collection<V> result = List.nil();
        Queue<V> queue = Queue.of(start);
        Set<V> visited = Set.of(Ord.natural(), start);

        while (!queue.isEmpty()) {
            V current = queue.head().orElse(null);
            queue = queue.tail().orElse(Queue.nil());
            result = result.build(current);

            Collection<V> neighbors = neighbors(current);
            final Set<V>[] visitedRef = new Set[]{visited};
            final Queue<V>[] queueRef = new Queue[]{queue};
            neighbors.forEach(neighbor -> {
                if (!visitedRef[0].contains(neighbor)) {
                    visitedRef[0] = visitedRef[0].add(neighbor);
                    queueRef[0] = (Queue<V>) queueRef[0].build(neighbor);
                }
            });
            visited = visitedRef[0];
            queue = queueRef[0];
        }
        return result;
    }

    /**
     * Depth-First Search (DFS) traversal.
     */
    default Collection<V> dfs(V start) {
        Collection<V> result = List.nil();
        Stack<V> stack = Stack.of(start);
        Set<V> visited = Set.of(Ord.natural(), start);

        while (!stack.isEmpty()) {
            V current = stack.head().orElse(null);
            stack = stack.tail().orElse(Stack.emptyStack());
            result = result.build(current);

            Collection<V> neighbors = neighbors(current);
            final Set<V>[] visitedRef = new Set[]{visited};
            final Stack<V>[] stackRef = new Stack[]{stack};
            neighbors.forEach(neighbor -> {
                if (!visitedRef[0].contains(neighbor)) {
                    visitedRef[0] = visitedRef[0].add(neighbor);
                    stackRef[0] = (Stack<V>) stackRef[0].build(neighbor);
                }
            });
            visited = visitedRef[0];
            stack = stackRef[0];
        }
        return result;
    }

    /**
     * Performs a topological sort of the graph.
     * Only applicable to Directed Acyclic Graphs (DAGs).
     * 
     * @return Some(Sorted vertices) if no cycles, Nothing otherwise.
     */
    default Maybe<Collection<V>> topologicalSort() {
        final HashMap<V, Integer>[] inDegree = new HashMap[]{ (HashMap<V, Integer>) vertices().foldl(HashMap.<V, Integer>nil(), (acc, v) -> acc.put(v, 0)) };
        vertices().forEach(u -> 
            neighbors(u).forEach(v -> 
                inDegree[0] = inDegree[0].put(v, inDegree[0].get(v).orElse(0) + 1)));

        final Queue<V>[] queue = new Queue[]{ (Queue<V>) vertices().foldl(Queue.<V>nil(), (acc, v) -> 
            inDegree[0].get(v).orElse(0) == 0 ? (Queue<V>) acc.build(v) : acc) };

        Collection<V> result = List.nil();
        final int[] count = {0};

        while (!queue[0].isEmpty()) {
            V u = queue[0].head().orElse(null);
            queue[0] = queue[0].tail().orElse(Queue.nil());
            result = result.build(u);
            count[0]++;

            final HashMap<V, Integer>[] inDegreeRef = inDegree;
            neighbors(u).forEach(v -> {
                int deg = inDegreeRef[0].get(v).orElse(0) - 1;
                inDegreeRef[0] = inDegreeRef[0].put(v, deg);
                if (deg == 0) {
                    queue[0] = (Queue<V>) queue[0].build(v);
                }
            });
        }

        return count[0] == vertices().length() ? Maybe.some(result) : Maybe.nothing();
    }

    /**
     * Dijkstra's algorithm for shortest paths from a source vertex.
     * 
     * @param source The starting vertex.
     * @return A map of vertices to their shortest distance from source.
     */
    default HashMap<V, Integer> dijkstra(V source) {
        final HashMap<V, Integer>[] distances = new HashMap[]{ (HashMap<V, Integer>) vertices().foldl(HashMap.<V, Integer>nil(), (acc, v) -> 
            acc.put(v, v.equals(source) ? 0 : Integer.MAX_VALUE)) };
        
        PriorityQueue<V> pq = PriorityQueue.of(Ord.natural(), source);

        while (!pq.isEmpty()) {
            V u = pq.peek().orElse(null);
            pq = pq.pop().orElse(PriorityQueue.<V>nilWithOrd(Ord.natural()));

            int distU = distances[0].get(u).orElse(Integer.MAX_VALUE);
            
            final PriorityQueue<V>[] pqRef = new PriorityQueue[]{pq};
            neighbors(u).forEach(v -> {
                int newDist = distU + 1; // Assuming weight 1 for all edges
                if (newDist < distances[0].get(v).orElse(Integer.MAX_VALUE)) {
                    distances[0] = distances[0].put(v, newDist);
                    pqRef[0] = (PriorityQueue<V>) pqRef[0].build(v);
                }
            });
            pq = pqRef[0];
        }
        return distances[0];
    }

    final class AdjacencyGraph<V extends Comparable<V>> implements Graph<V> {
        private final HashMap<V, Set<V>> adj;

        AdjacencyGraph(HashMap<V, Set<V>> adj) {
            this.adj = adj;
        }

        @Override
        public Graph<V> addVertex(V vertex) {
            return new AdjacencyGraph<V>(adj.put(vertex, adj.get(vertex).orElse(Set.<V>empty(Ord.natural()))));
        }

        @Override
        public Graph<V> addEdge(V from, V to) {
            Set<V> neighbors = adj.get(from).orElse(Set.<V>empty(Ord.natural())).add(to);
            return new AdjacencyGraph<V>(adj.put(from, neighbors).put(to, adj.get(to).orElse(Set.<V>empty(Ord.natural()))));
        }

        @Override
        public Collection<V> neighbors(V vertex) {
            return adj.get(vertex).orElse(Set.<V>empty(Ord.natural()));
        }

        @Override
        public Collection<V> vertices() {
            return adj.map(HashMap.Entry::key);
        }
    }
}
