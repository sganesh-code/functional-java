package io.github.senthilganeshs.fj.ds;

import java.util.function.BiFunction;

/**
 * A purely functional Graph interface using an Adjacency Map representation.
 * 
 * @param <V> The type of nodes in the graph.
 */
public interface Graph<V extends Comparable<V>> extends Collection<V> {

    Graph<V> addNode(V node);
    Graph<V> addEdge(V from, V to);
    Set<V> nodes();
    Set<V> successors(V node);

    static <R extends Comparable<R>> Graph<R> nil() {
        return new AdjacencyMapGraph<>(HashMap.<R, Set<R>>nil());
    }

    @SuppressWarnings("unchecked")
    default List<V> bfs(V start) {
        List<V> result = List.nil();
        Set<V> visited = Set.emptyNatural();
        Queue<V> queue = Queue.of(start);

        while (true) {
            Maybe<Tuple<V, Queue<V>>> dequeued = queue.dequeue();
            Tuple<V, Queue<V>> t = dequeued.orElse(null);
            if (t == null) break;

            V current = t.getA().orElse(null);
            queue = t.getB().orElse(Queue.nil());

            if (visited.contains(current)) continue;

            visited = visited.build(current);
            result = result.build(current);

            Set<V> neighbors = successors(current);
            final Queue<V> currentQueue = queue;
            queue = (Queue<V>) neighbors.foldl(currentQueue, (q, v) -> (Queue<V>) ((Queue<V>)q).build(v));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    default List<V> dfs(V start) {
        List<V> result = List.nil();
        Set<V> visited = Set.emptyNatural();
        Stack<V> stack = (Stack<V>) Stack.<V>emptyStack().build(start);

        while (true) {
            Maybe<V> top = stack.head();
            V current = top.orElse(null);
            if (current == null) break;

            stack = stack.tail().orElse(Stack.emptyStack());

            if (visited.contains(current)) continue;

            visited = visited.build(current);
            result = result.build(current);

            Set<V> neighbors = successors(current);
            final Stack<V> currentStack = stack;
            stack = (Stack<V>) neighbors.foldl(currentStack, (s, v) -> (Stack<V>) ((Stack<V>)s).build(v));
        }
        return result;
    }

    /**
     * Performs a topological sort on the graph.
     * Returns Maybe.nothing() if the graph contains a cycle.
     * 
     * @return Some(list) of nodes in topological order, or Nothing if cycle found.
     */
    @SuppressWarnings("unchecked")
    default Maybe<List<V>> topologicalSort() {
        HashMap<V, Integer> inDegree = nodes().foldl(HashMap.<V, Integer>nil(), (acc, n) -> {
            HashMap<V, Integer> next = acc.put(n, acc.get(n).orElse(0));
            return successors(n).foldl(next, (accInner, succ) -> 
                accInner.put(succ, accInner.get(succ).orElse(0) + 1));
        });

        Queue<V> queue = nodes().foldl(Queue.<V>nil(), (q, n) -> 
            inDegree.get(n).orElse(0) == 0 ? (Queue<V>) q.build(n) : q);

        List<V> result = List.nil();
        int count = 0;
        
        HashMap<V, Integer> currentInDegree = inDegree;
        Queue<V> currentQueue = queue;

        while (true) {
            Maybe<Tuple<V, Queue<V>>> dequeued = currentQueue.dequeue();
            Tuple<V, Queue<V>> t = dequeued.orElse(null);
            if (t == null) break;

            V u = t.getA().orElse(null);
            currentQueue = t.getB().orElse(Queue.nil());
            result = result.build(u);
            count++;

            Set<V> neighbors = successors(u);
            final HashMap<V, Integer> loopInDegree = currentInDegree;
            final Queue<V> loopQueue = currentQueue;
            
            Tuple<HashMap<V, Integer>, Queue<V>> state = neighbors.foldl(Tuple.of(loopInDegree, loopQueue), (acc, v) -> {
                HashMap<V, Integer> d = (HashMap<V, Integer>) acc.getA().orElse(null);
                Queue<V> q = (Queue<V>) acc.getB().orElse(null);
                int newDeg = d.get(v).orElse(0) - 1;
                HashMap<V, Integer> nextD = d.put(v, newDeg);
                Queue<V> nextQ = (newDeg == 0) ? (Queue<V>) q.build(v) : q;
                return Tuple.of(nextD, nextQ);
            });
            
            currentInDegree = state.getA().orElse(null);
            currentQueue = state.getB().orElse(null);
        }

        return count == nodes().length() ? Maybe.some(result) : Maybe.nothing();
    }

    final class AdjacencyMapGraph<V extends Comparable<V>> implements Graph<V> {
        private final HashMap<V, Set<V>> adjacencyMap;

        AdjacencyMapGraph(HashMap<V, Set<V>> adjacencyMap) {
            this.adjacencyMap = adjacencyMap;
        }

        @Override
        public Graph<V> addNode(V node) {
            if (adjacencyMap.get(node).isSome()) return this;
            return new AdjacencyMapGraph<>(adjacencyMap.put(node, Set.emptyNatural()));
        }

        @Override
        public Graph<V> addEdge(V from, V to) {
            Graph<V> g = addNode(from).addNode(to);
            HashMap<V, Set<V>> map = ((AdjacencyMapGraph<V>) g).adjacencyMap;
            Set<V> neighbors = map.get(from).orElse(Set.emptyNatural());
            return new AdjacencyMapGraph<>(map.put(from, neighbors.build(to)));
        }

        @Override
        public Set<V> nodes() {
            return adjacencyMap.foldl(Set.<V>emptyNatural(), (acc, entry) -> acc.build(entry.key()));
        }

        @Override
        public Set<V> successors(V node) {
            return adjacencyMap.get(node).orElse(Set.emptyNatural());
        }

        @Override
        public <R> Collection<R> empty() {
            return List.nil();
        }

        @Override
        public Collection<V> build(V input) {
            return addNode(input);
        }

        @Override
        public <R> R foldl(R seed, BiFunction<R, V, R> fn) {
            return nodes().foldl(seed, fn);
        }

        @Override
        public String toString() {
            return "Graph" + adjacencyMap.toString();
        }

        @Override
        public int length() {
            return nodes().length();
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(Object other) {
            if (other == null) return false;
            if (other == this) return true;
            if (!(other instanceof Graph)) return false;
            Graph<?> o = (Graph<?>) other;
            return adjacencyMap.equals(((AdjacencyMapGraph<?>) o).adjacencyMap);
        }

        @Override
        public int hashCode() {
            return adjacencyMap.hashCode();
        }
    }
}
