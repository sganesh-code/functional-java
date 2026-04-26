package io.github.senthilganeshs.fj.optic;

import java.util.function.Function;

/**
 * A read-only optic that focuses on exactly one element.
 */
public interface Getter<S, A> {
    A get(S s);

    /**
     * Composes this getter with another getter.
     */
    default <B> Getter<S, B> compose(Getter<A, B> other) {
        return s -> other.get(get(s));
    }

    /**
     * Composes this getter with a fold.
     */
    default <B> Fold<S, B> compose(Fold<A, B> other) {
        return s -> other.getAll(get(s));
    }

    static <S, A> Getter<S, A> of(Function<S, A> f) {
        return f::apply;
    }
}
