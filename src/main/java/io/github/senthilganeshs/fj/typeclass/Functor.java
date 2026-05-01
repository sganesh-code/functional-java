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
}
