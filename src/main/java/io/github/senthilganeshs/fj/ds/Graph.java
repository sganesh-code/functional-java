package io.github.senthilganeshs.fj.ds;

import java.util.function.BiFunction;

/**
 * A purely functional Directed Graph implementation.
 * Represented as an Adjacency Map: HashMap<V, Set<V>>.
 */
public interface Graph<V extends Comparable<V>> extends Collection<V> {

    Graph<V> addNode(V node);

    Graph<V> addEdge(V from, V to);

    Set<V> nodes();

    Set<V> successors(V node);

    default List<V> bfs(V start) {
        List<V> result = List.nil();
        Set<V> visited = Set.empty();
        Queue<V> queue = Queue.of(start);

        while (true) {
            Maybe<Tuple<V, Queue<V>>> dequeued = queue.dequeue();
            Tuple<V, Queue<V>> t = dequeued.fromMaybe(null);
            if (t == null) break;

            V current = t.getA().fromMaybe(null);
            queue = (Queue<V>) t.getB().fromMaybe(Queue.nil());

            if (visited.contains(current)) continue;

            visited = (Set<V>) visited.build(current);
            result = (List<V>) result.build(current);

            Set<V> neighbors = successors(current);
            final Queue<V> currentQueue = queue;
            queue = (Queue<V>) neighbors.foldl(currentQueue, (q, v) -> (Queue<V>) ((Queue<V>)q).build(v));
        }
        return result;
    }

    default List<V> dfs(V start) {
        List<V> result = List.nil();
        Set<V> visited = Set.empty();
        Stack<V> stack = (Stack<V>) Stack.<V>emptyStack().build(start);

        while (true) {
            Maybe<V> top = stack.head();
            V current = top.fromMaybe(null);
            if (current == null) break;

            stack = (Stack<V>) stack.tail().fromMaybe(Stack.emptyStack());

            if (visited.contains(current)) continue;

            visited = (Set<V>) visited.build(current);
            result = (List<V>) result.build(current);

            Set<V> neighbors = successors(current);
            final Stack<V> currentStack = stack;
            stack = (Stack<V>) neighbors.foldl(currentStack, (s, v) -> (Stack<V>) ((Stack<V>)s).build(v));
        }
        return result;
    }

    /**
     * Performs a topological sort on the graph.
     * Returns Maybe.nothing() if the graph contains a cycle.
     */
    default Maybe<List<V>> topologicalSort() {
        // Since pure functional DFS for topo sort is complex for the Java compiler,
        // we'll use Kahn's algorithm or a slightly more robust DFS state management.
        
        // 1. Calculate in-degrees
        final HashMap<V, Integer> initialInDegree = nodes().foldl(HashMap.nil(), (acc, n) -> 
            successors(n).foldl(((HashMap<V, Integer>)acc).put(n, acc.get(n).fromMaybe(0)), (accInner, succ) -> {
                int count = accInner.get(succ).fromMaybe(0);
                return accInner.put(succ, count + 1);
            })
        );

        // 2. Initial queue with 0 in-degree nodes
        Queue<V> initialQueue = nodes().foldl(Queue.nil(), (q, n) -> 
            initialInDegree.get(n).fromMaybe(0) == 0 ? (Queue<V>) q.build(n) : q
        );

        List<V> result = List.nil();
        int count = 0;
        HashMap<V, Integer> currentInDegree = initialInDegree;
        Queue<V> currentQueue = initialQueue;

        while (true) {
            Maybe<Tuple<V, Queue<V>>> dequeued = currentQueue.dequeue();
            Tuple<V, Queue<V>> t = dequeued.fromMaybe(null);
            if (t == null) break;

            V u = t.getA().fromMaybe(null);
            currentQueue = t.getB().fromMaybe(Queue.nil());
            result = (List<V>) result.build(u);
            count++;

            Set<V> neighbors = successors(u);
            final HashMap<V, Integer> loopInDegree = currentInDegree;
            final Queue<V> loopQueue = currentQueue;
            
            // This is tricky to do purely inside a loop without mutation of the outer local variables.
            // We'll use a custom record or Tuple to carry the state through foldl.
            Tuple<HashMap<V, Integer>, Queue<V>> state = neighbors.foldl(Tuple.of(loopInDegree, loopQueue), (acc, v) -> {
                HashMap<V, Integer> d = (HashMap<V, Integer>) acc.getA().fromMaybe(null);
                Queue<V> q = (Queue<V>) acc.getB().fromMaybe(null);
                int newDeg = d.get(v).fromMaybe(0) - 1;
                HashMap<V, Integer> nextD = d.put(v, newDeg);
                Queue<V> nextQ = (newDeg == 0) ? (Queue<V>) q.build(v) : q;
                return Tuple.of(nextD, nextQ);
            });
            
            currentInDegree = state.getA().fromMaybe(null);
            currentQueue = state.getB().fromMaybe(null);
        }

        if (count != nodes().length()) {
            return Maybe.nothing(); // Cycle!
        }
        return Maybe.some(result);
    }

    static <V extends Comparable<V>> Graph<V> nil() {
        return new AdjacencyMapGraph<V>(HashMap.nil());
    }

    final class AdjacencyMapGraph<V extends Comparable<V>> implements Graph<V> {
        private final HashMap<V, Set<V>> adjacencyMap;

        AdjacencyMapGraph(HashMap<V, Set<V>> adjacencyMap) {
            this.adjacencyMap = adjacencyMap;
        }

        @Override
        public Graph<V> addNode(V node) {
            return ((Maybe<Graph<V>>) adjacencyMap.get(node).map(__ -> (Graph<V>) this))
                .fromMaybe(new AdjacencyMapGraph<>(adjacencyMap.put(node, Set.empty())));
        }

        @Override
        public Graph<V> addEdge(V from, V to) {
            Graph<V> g = addNode(from).addNode(to);
            HashMap<V, Set<V>> map = ((AdjacencyMapGraph<V>) g).adjacencyMap;
            Set<V> neighbors = map.get(from).fromMaybe(Set.empty());
            return new AdjacencyMapGraph<>(map.put(from, (Set<V>) neighbors.build(to)));
        }

        @Override
        public Set<V> nodes() {
            // Correct way to get all keys as a Set
            Collection<V> emptySet = (Collection<V>) Set.<V>empty();
            return (Set<V>) adjacencyMap.foldl(emptySet, (acc, entry) -> (Collection<V>) ((Set<V>)acc).build(entry.key()));
        }

        @Override
        public Set<V> successors(V node) {
            return adjacencyMap.get(node).fromMaybe(Set.empty());
        }

        @Override
        public <R> Collection<R> empty() {
            return (Collection<R>) Graph.nil();
        }

        @Override
        public Collection<V> build(V input) {
            return addNode(input);
        }

        @Override
        public <R> R foldl(R seed, BiFunction<R, V, R> fn) {
            return adjacencyMap.foldl(seed, (acc, entry) -> fn.apply(acc, entry.key()));
        }

        @Override
        public String toString() {
            return adjacencyMap.foldl("Graph{", (acc, entry) -> 
                acc + (acc.equals("Graph{") ? "" : ", ") + entry.key() + "->" + entry.value()
            ) + "}";
        }
    }
}
