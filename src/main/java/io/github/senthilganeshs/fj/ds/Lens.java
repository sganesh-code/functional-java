package io.github.senthilganeshs.fj.ds;

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
     * Helper to create a lens from a getter and a setter.
     */
    static <S, A> Lens<S, A> of(Function<S, A> getter, BiFunction<A, S, S> setter) {
        return new Lens<S, A>() {
            @Override public A get(S s) { return getter.apply(s); }
            @Override public S set(A a, S s) { return setter.apply(a, s); }
        };
    }
}
