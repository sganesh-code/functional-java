package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.typeclass.Ord;
import java.util.function.BiFunction;

/**
 * Functional Graph implementation using adjacency maps.
 */
public interface Graph<V extends Comparable<V>> {

    static <V extends Comparable<V>> Graph<V> empty() {
        return nil();
    }

    static <V extends Comparable<V>> Graph<V> nil() {
        return new AdjacencyGraph<V>(HashMap.<V, Set<V>>nil());
    }

    Graph<V> addVertex(V vertex);
    default Graph<V> addNode(V vertex) { return addVertex(vertex); }
    
    Graph<V> addEdge(V source, V target);
    
    Collection<V> neighbors(V vertex);
    default Collection<V> successors(V vertex) { return neighbors(vertex); }
    
    Collection<V> vertices();
    default Collection<V> nodes() { return vertices(); }
    
    default int length() { return vertices().length(); }
    default int count() { return vertices().count(); }

    /**
     * Breadth-First Search. Returns a collection of vertices in BFS order.
     */
    default Collection<V> bfs(V start) {
        java.util.List<V> result = new java.util.ArrayList<>();
        bfs(start, result::add);
        return Collection.from(result);
    }

    /**
     * Breadth-First Search with consumer.
     */
    default void bfs(V start, java.util.function.Consumer<V> action) {
        final Set<V>[] visitedRef = new Set[]{Set.empty(Ord.<V>natural())};
        final Queue<V>[] queueRef = new Queue[]{Queue.of(start)};
        
        while (!queueRef[0].isEmpty()) {
            V current = queueRef[0].head().orElse(null);
            queueRef[0] = (Queue<V>) queueRef[0].drop(1);
            
            if (visitedRef[0].contains(current)) continue;
            
            action.accept(current);
            visitedRef[0] = (Set<V>) visitedRef[0].build(current);
            
            neighbors(current).forEach(neighbor -> {
                if (!visitedRef[0].contains(neighbor)) {
                    queueRef[0] = queueRef[0].enqueue(neighbor);
                }
            });
        }
    }

    /**
     * Depth-First Search. Returns a collection of vertices in DFS order.
     */
    default Collection<V> dfs(V start) {
        java.util.List<V> result = new java.util.ArrayList<>();
        dfs(start, result::add);
        return Collection.from(result);
    }

    /**
     * Depth-First Search with consumer.
     */
    default void dfs(V start, java.util.function.Consumer<V> action) {
        final Set<V>[] visitedRef = new Set[]{Set.empty(Ord.<V>natural())};
        final Stack<V>[] stackRef = new Stack[]{Stack.of(start)};
        
        while (!stackRef[0].isEmpty()) {
            V current = stackRef[0].head().orElse(null);
            stackRef[0] = (Stack<V>) stackRef[0].drop(1);
            
            if (visitedRef[0].contains(current)) continue;
            
            action.accept(current);
            visitedRef[0] = (Set<V>) visitedRef[0].build(current);
            
            neighbors(current).forEach(neighbor -> {
                if (!visitedRef[0].contains(neighbor)) {
                    stackRef[0] = stackRef[0].push(neighbor);
                }
            });
        }
    }

    /**
     * Topological Sort using Kahn's algorithm.
     */
    @SuppressWarnings("unchecked")
    default Maybe<List<V>> topologicalSort() {
        final HashMap<V, Integer>[] inDegree = new HashMap[]{ (HashMap<V, Integer>) vertices().foldl(HashMap.<V, Integer>nil(), (acc, v) -> acc.put(v, 0)) };
        
        vertices().forEach(v -> {
            neighbors(v).forEach(neighbor -> {
                int degree = inDegree[0].get(neighbor).orElse(0);
                inDegree[0] = inDegree[0].put(neighbor, degree + 1);
            });
        });

        final Queue<V>[] queue = new Queue[]{ (Queue<V>) vertices().foldl(Queue.<V>nil(), (acc, v) -> 
            inDegree[0].get(v).orElse(0) == 0 ? acc.enqueue(v) : acc) };

        final List<V>[] result = new List[]{ List.nil() };
        int count = 0;

        while (!queue[0].isEmpty()) {
            V v = queue[0].head().orElse(null);
            queue[0] = (Queue<V>) queue[0].drop(1);
            result[0] = (List<V>) result[0].build(v);
            count++;

            neighbors(v).forEach(neighbor -> {
                int degree = inDegree[0].get(neighbor).orElse(0) - 1;
                inDegree[0] = inDegree[0].put(neighbor, degree);
                if (degree == 0) queue[0] = queue[0].enqueue(neighbor);
            });
        }

        return count == vertices().length() ? Maybe.some(result[0]) : Maybe.nothing();
    }

    /**
     * Dijkstra's Algorithm for shortest paths (unit weights).
     */
    @SuppressWarnings("unchecked")
    default HashMap<V, Integer> dijkstra(V source) {
        final HashMap<V, Integer>[] distances = new HashMap[]{ (HashMap<V, Integer>) vertices().foldl(HashMap.<V, Integer>nil(), (acc, v) -> 
            acc.put(v, v.equals(source) ? 0 : Integer.MAX_VALUE)) };
        
        PriorityQueue<V> pq = PriorityQueue.of(Ord.<V>natural(), source);

        while (!pq.isEmpty()) {
            V u = pq.peek().orElse(null);
            pq = pq.pop().orElse((PriorityQueue<V>) PriorityQueue.nil());

            int distU = distances[0].get(u).orElse(Integer.MAX_VALUE);

            neighbors(u).forEach(v -> {
                int newDist = distU + 1;
                if (newDist < distances[0].get(v).orElse(Integer.MAX_VALUE)) {
                    distances[0] = distances[0].put(v, newDist);
                }
            });
        }
        return distances[0];
    }

    class AdjacencyGraph<V extends Comparable<V>> implements Graph<V> {
        private final HashMap<V, Set<V>> adj;

        AdjacencyGraph(HashMap<V, Set<V>> adj) {
            this.adj = adj;
        }

        @Override
        public Graph<V> addVertex(V vertex) {
            return new AdjacencyGraph<V>(adj.put(vertex, adj.get(vertex).orElse(Set.<V>empty(Ord.<V>natural()))));
        }

        @Override
        public Graph<V> addEdge(V source, V target) {
            Graph<V> g = addVertex(source).addVertex(target);
            HashMap<V, Set<V>> nextAdj = ((AdjacencyGraph<V>) g).adj;
            Set<V> neighbors = nextAdj.get(source).orElse(Set.empty(Ord.<V>natural()));
            return new AdjacencyGraph<V>(nextAdj.put(source, (Set<V>) neighbors.build(target)));
        }

        @Override
        public Collection<V> neighbors(V vertex) {
            return adj.get(vertex).orElse(Set.<V>empty(Ord.<V>natural()));
        }

        @Override
        public Collection<V> vertices() {
            return adj.map(HashMap.Entry::key);
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof AdjacencyGraph) {
                return adj.equals(((AdjacencyGraph<?>) other).adj);
            }
            return false;
        }

        @Override
        public int hashCode() { return adj.hashCode(); }

        @Override
        public String toString() {
            return "Graph{" + adj.map(HashMap.Entry::key).mkString(",") + "}";
        }
    }
}
