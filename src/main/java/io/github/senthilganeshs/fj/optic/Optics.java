package io.github.senthilganeshs.fj.optic;

import io.github.senthilganeshs.fj.ds.Collection;
import io.github.senthilganeshs.fj.ds.HashMap;
import io.github.senthilganeshs.fj.ds.List;
import io.github.senthilganeshs.fj.ds.Maybe;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Entry points for functional optics (Lens, Prism, Traversal).
 * Enables composition of functional APIs over nested structures.
 */
public final class Optics {
    private Optics() {}

    /**
     * Focuses on each element of any functional-java Collection (Maybe, List, etc.).
     * Equivalent to 'traverse' in Haskell.
     */
    public static <T> Traversal<Collection<T>, T> each() {
        return Traversal.fromCollection();
    }

    /**
     * Focuses on each element of a List.
     */
    @SuppressWarnings("unchecked")
    public static <T> Traversal<List<T>, T> list() {
        return (Traversal<List<T>, T>) (Traversal<?, ?>) Traversal.fromCollection();
    }

    /**
     * Focuses on the value inside a Maybe, if it exists.
     */
    @SuppressWarnings("unchecked")
    public static <T> Traversal<Maybe<T>, T> maybe() {
        return (Traversal<Maybe<T>, T>) (Traversal<?, ?>) Traversal.fromCollection();
    }

    /**
     * Focuses on each value within a HashMap.
     */
    public static <K, V> Traversal<HashMap<K, V>, V> mapValues() {
        return new Traversal<HashMap<K, V>, V>() {
            @Override public List<V> getAll(HashMap<K, V> s) {
                return List.from(s.foldl(List.<V>nil(), (acc, e) -> (List<V>) acc.build(e.value())));
            }
            @Override public HashMap<K, V> modify(HashMap<K, V> s, UnaryOperator<V> fn) {
                return s.foldl(HashMap.<K, V>nil(), (acc, e) -> acc.put(e.key(), fn.apply(e.value())));
            }
        };
    }
}
