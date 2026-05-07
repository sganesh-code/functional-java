package io.github.senthilganeshs.fj.ds;

/**
 * Helper functions that lift a store port into ReaderTaskEither so DI stays explicit.
 */
public final class GraphStores {
    private GraphStores() {}

    public static <V extends Comparable<V>> ReaderTaskEither<GraphStore<V>, GraphStoreError, Graph<V>> load(GraphQuery<V> query) {
        return new ReaderTaskEither<>(new Reader<>(store -> store.load(query)));
    }

    public static <V extends Comparable<V>> ReaderTaskEither<GraphStore<V>, GraphStoreError, Void> save(Graph<V> graph) {
        return new ReaderTaskEither<>(new Reader<>(store -> store.save(graph)));
    }
}
