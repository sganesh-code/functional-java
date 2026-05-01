package io.github.senthilganeshs.fj.typeclass;

import io.github.senthilganeshs.fj.hkt.Higher;
import java.util.function.Function;

/**
 * A typeclass for applicative functors that support sequential composition.
 * 
 * @param <W> The witness type of the monad.
 */
public interface Monad<W> extends Applicative<W> {
    /**
     * Sequentially composes two monadic actions.
     */
    <A, B> Higher<W, B> flatMap(Function<A, Higher<W, B>> fn, Higher<W, A> fa);

    @Override
    default <A, B> Higher<W, B> ap(Higher<W, Function<A, B>> ff, Higher<W, A> fa) {
        return flatMap(f -> map(f, fa), ff);
    }

    /**
     * Flattens a nested monadic structure.
     */
    default <A> Higher<W, A> flatten(Higher<W, Higher<W, A>> ffa) {
        return flatMap(Function.identity(), ffa);
    }
}
