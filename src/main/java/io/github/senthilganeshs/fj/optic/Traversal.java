package io.github.senthilganeshs.fj.optic;

import io.github.senthilganeshs.fj.ds.Collection;
import io.github.senthilganeshs.fj.ds.List;
import java.util.function.Function;

/**
 * An optic that focuses on 0 to N elements within a collection.
 */
public interface Traversal<S, A> {
    Collection<A> getAll(S source);
    S modify(S source, Function<A, A> fn);

    default S set(A value, S source) {
        return modify(source, __ -> value);
    }

    /**
     * Composes this traversal with a lens.
     */
    default <B> Traversal<S, B> compose(Lens<A, B> other) {
        return new Traversal<S, B>() {
            @Override public Collection<B> getAll(S s) {
                return Traversal.this.getAll(s).map(other::get);
            }
            @Override public S modify(S s, Function<B, B> fn) {
                return Traversal.this.modify(s, a -> other.modify(a, fn));
            }
        };
    }

    /**
     * Composes this traversal with a prism.
     */
    default <B> Traversal<S, B> compose(Prism<A, B> other) {
        return new Traversal<S, B>() {
            @Override public Collection<B> getAll(S s) {
                return Traversal.this.getAll(s).mapMaybe(other::getMaybe);
            }
            @Override public S modify(S s, Function<B, B> fn) {
                return Traversal.this.modify(s, a -> other.modify(a, fn));
            }
        };
    }

    /**
     * Composes this traversal with another traversal.
     */
    default <B> Traversal<S, B> compose(Traversal<A, B> other) {
        return new Traversal<S, B>() {
            @Override public Collection<B> getAll(S s) {
                return Traversal.this.getAll(s).flatMap(other::getAll);
            }
            @Override public S modify(S s, Function<B, B> fn) {
                return Traversal.this.modify(s, a -> other.modify(a, fn));
            }
        };
    }

    /**
     * Standard Traversal for any FJ Collection.
     */
    static <T> Traversal<Collection<T>, T> fromCollection() {
        return new Traversal<Collection<T>, T>() {
            @Override public Collection<T> getAll(Collection<T> s) { return s; }
            @Override public Collection<T> modify(Collection<T> s, Function<T, T> fn) {
                return s.map(fn);
            }
        };
    }
}
