package io.github.senthilganeshs.fj.typeclass;

import io.github.senthilganeshs.fj.hkt.Higher;
import java.util.function.Function;

/**
 * A typeclass for type constructors that support contravariant mapping.
 * 
 * @param <W> The witness type of the contravariant functor.
 */
public interface Contravariant<W> {
    /**
     * Maps a function over the values contained within the contravariant functor.
     */
    <A, B> Higher<W, A> contramap(Function<A, B> fn, Higher<W, B> fb);

    /**
     * Static utility for contramap.
     */
    static <W, A, B> Higher<W, A> contramap(Contravariant<W> c, Function<A, B> fn, Higher<W, B> fb) {
        return c.contramap(fn, fb);
    }
}
