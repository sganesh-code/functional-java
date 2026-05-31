package io.github.senthilganeshs.fj.typeclass;

import io.github.senthilganeshs.fj.hkt.Higher;
import java.util.function.Function;

/**
 * A typeclass for types that support a mapping operation.
 * 
 * @param <W> The witness type of the functor.
 */
public interface Functor<W> {
    /**
     * Maps a function over the values contained within the functor.
     */
    <A, B> Higher<W, B> map(Function<A, B> fn, Higher<W, A> fa);

    /**
     * Static utility for map.
     */
    static <W, A, B> Higher<W, B> map(Functor<W> f, Function<A, B> fn, Higher<W, A> fa) {
        return f.map(fn, fa);
    }

    /**
     * Replaces the values in the functor with a constant value.
     */
    static <W, A, B> Higher<W, B> as(Functor<W> f, B b, Higher<W, A> fa) {
        return f.map(__ -> b, fa);
    }

    /**
     * Replaces the values in the functor with Void.
     */
    static <W, A> Higher<W, Void> voidF(Functor<W> f, Higher<W, A> fa) {
        return f.map(__ -> (Void) null, fa);
    }

    /**
     * Tuples the values in the functor with themselves.
     */
    static <W, A> Higher<W, io.github.senthilganeshs.fj.ds.Tuple<A, A>> tupled(Functor<W> f, Higher<W, A> fa) {
        return f.map(a -> io.github.senthilganeshs.fj.ds.Tuple.of(a, a), fa);
    }
}
