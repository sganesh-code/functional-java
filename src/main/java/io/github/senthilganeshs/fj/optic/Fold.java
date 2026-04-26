package io.github.senthilganeshs.fj.optic;

import io.github.senthilganeshs.fj.ds.Collection;
import io.github.senthilganeshs.fj.ds.List;
import java.util.function.Function;

/**
 * A read-only optic that focuses on zero to many elements.
 */
public interface Fold<S, A> {
    Collection<A> getAll(S s);

    /**
     * Composes this fold with a getter.
     */
    default <B> Fold<S, B> compose(Getter<A, B> other) {
        return s -> getAll(s).map(other::get);
    }

    /**
     * Composes this fold with another fold.
     */
    default <B> Fold<S, B> compose(Fold<A, B> other) {
        return s -> getAll(s).flatMap(other::getAll);
    }

    static <S, A> Fold<S, A> of(Function<S, Collection<A>> f) {
        return f::apply;
    }
}
