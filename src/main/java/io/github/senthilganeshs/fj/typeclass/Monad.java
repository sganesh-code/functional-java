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

    /**
     * Static utility for flatMap.
     */
    static <W, A, B> Higher<W, B> flatMap(Monad<W> m, Function<A, Higher<W, B>> fn, Higher<W, A> fa) {
        return m.flatMap(fn, fa);
    }

    /**
     * Static utility for flatten.
     */
    static <W, A> Higher<W, A> flatten(Monad<W> m, Higher<W, Higher<W, A>> ffa) {
        return m.flatten(ffa);
    }

    /**
     * Conditional execution of monadic actions.
     */
    static <W, A> Higher<W, A> ifM(Monad<W> m, Higher<W, Boolean> cond, Higher<W, A> ifTrue, Higher<W, A> ifFalse) {
        return m.flatMap(b -> b ? ifTrue : ifFalse, cond);
    }

    @Override
    default <A, B> Higher<W, B> map(Function<A, B> fn, Higher<W, A> fa) {
        return flatMap(a -> pure(fn.apply(a)), fa);
    }

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
