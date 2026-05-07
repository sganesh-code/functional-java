package io.github.senthilganeshs.fj.ds;

/**
 * Store-agnostic graph port.
 * Implementations can be backed by a database, an API, or an in-memory cache.
 */
public interface GraphStore<V extends Comparable<V>> {
    TaskEither<GraphStoreError, Graph<V>> load(GraphQuery<V> query);

    TaskEither<GraphStoreError, Void> save(Graph<V> graph);
}

