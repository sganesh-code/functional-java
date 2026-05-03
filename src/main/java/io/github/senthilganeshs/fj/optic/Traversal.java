package io.github.senthilganeshs.fj.optic;

import io.github.senthilganeshs.fj.ds.Collection;
import java.util.function.UnaryOperator;

/**
 * An optic that focuses on 0 to N elements within a collection.
 */
public interface Traversal<S, A> {
    Collection<A> getAll(S source);
    S modify(S source, UnaryOperator<A> fn);

    default S set(A value, S source) {
        return modify(source, __ -> value);
    }

    /**
     * Performs an action for each element focused by this traversal.
     */
    default void forEach(S source, java.util.function.Consumer<A> action) {
        getAll(source).forEach(action);
    }

    /**
     * Composes this traversal with a lens.
     */
    default <B> Traversal<S, B> compose(Lens<A, B> other) {
        return new Traversal<S, B>() {
            @Override public Collection<B> getAll(S s) {
                return Traversal.this.getAll(s).map(other::get);
            }
            @Override public S modify(S s, UnaryOperator<B> fn) {
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
            @Override public S modify(S s, UnaryOperator<B> fn) {
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
            @Override public S modify(S s, UnaryOperator<B> fn) {
                return Traversal.this.modify(s, a -> other.modify(a, fn));
            }
        };
    }

    /**
     * Views this traversal as a read-only Fold.
     */
    default Fold<S, A> asFold() {
        return this::getAll;
    }

    /**
     * Composes this traversal with a getter.
     */
    default <B> Fold<S, B> compose(Getter<A, B> other) {
        return s -> getAll(s).map(other::get);
    }

    /**
     * Composes this traversal with a fold.
     */
    default <B> Fold<S, B> compose(Fold<A, B> other) {
        return s -> getAll(s).flatMap(other::getAll);
    }

    /**
     * Standard Traversal for any FJ Collection.
     */
    public static <W extends Collection<T>, T> Traversal<W, T> fromCollection() {
        return new Traversal<W, T>() {
            @Override public W getAll(W s) { return s; }
            @Override public W modify(W s, UnaryOperator<T> fn) {
                return (W) s.map(fn);
            }
        };
    }
}
