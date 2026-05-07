package io.github.senthilganeshs.fj.ds;

/**
 * Store-agnostic request for materializing a graph view.
 * The library keeps this intentionally small so adapters stay backend-neutral.
 */
public record GraphQuery<V extends Comparable<V>>(Maybe<V> root, Maybe<Integer> depth) {
    public static <V extends Comparable<V>> GraphQuery<V> all() {
        return new GraphQuery<>(Maybe.<V>nothing(), Maybe.<Integer>nothing());
    }

    public static <V extends Comparable<V>> GraphQuery<V> root(V root) {
        return new GraphQuery<>(Maybe.some(root), Maybe.<Integer>nothing());
    }

    public static <V extends Comparable<V>> GraphQuery<V> root(V root, int depth) {
        return new GraphQuery<>(Maybe.some(root), Maybe.some(depth));
    }
}
