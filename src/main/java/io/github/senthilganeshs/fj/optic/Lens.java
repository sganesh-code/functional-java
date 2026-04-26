package io.github.senthilganeshs.fj.optic;

import io.github.senthilganeshs.fj.ds.Maybe;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * An optic that focuses on a single mandatory field within a structure.
 * S is the Source, A is the Attribute.
 */
public interface Lens<S, A> {
    A get(S source);
    S set(A value, S source);

    default S modify(S source, Function<A, A> fn) {
        return set(fn.apply(get(source)), source);
    }

    /**
     * Composes this lens with another lens.
     */
    default <B> Lens<S, B> compose(Lens<A, B> other) {
        return new Lens<S, B>() {
            @Override public B get(S s) { return other.get(Lens.this.get(s)); }
            @Override public S set(B b, S s) {
                return Lens.this.set(other.set(b, Lens.this.get(s)), s);
            }
        };
    }

    /**
     * Composes this lens with a prism, resulting in an AffineTraversal.
     */
    default <B> AffineTraversal<S, B> compose(Prism<A, B> other) {
        return new AffineTraversal<S, B>() {
            @Override public Maybe<B> getMaybe(S s) {
                return other.getMaybe(Lens.this.get(s));
            }
            @Override public S set(B b, S s) {
                return Lens.this.modify(s, a -> other.set(b, a));
            }
        };
    }

    /**
     * Views this lens as a read-only Getter.
     */
    default Getter<S, A> asGetter() {
        return this::get;
    }

    /**
     * Composes this lens with a getter.
     */
    default <B> Getter<S, B> compose(Getter<A, B> other) {
        return s -> other.get(get(s));
    }

    /**
     * Composes this lens with a fold.
     */
    default <B> Fold<S, B> compose(Fold<A, B> other) {
        return s -> other.getAll(get(s));
    }

    /**
     * Helper to create a lens from a getter and a setter.
     */
    static <S, A> Lens<S, A> of(Function<S, A> getter, BiFunction<A, S, S> setter) {
        return new Lens<S, A>() {
            @Override public A get(S s) { return getter.apply(s); }
            @Override public S set(A a, S s) { return setter.apply(a, s); }
        };
    }
}
