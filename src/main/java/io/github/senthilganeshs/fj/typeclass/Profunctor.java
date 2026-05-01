package io.github.senthilganeshs.fj.typeclass;

import io.github.senthilganeshs.fj.hkt.Higher;
import java.util.function.Function;

/**
 * A typeclass for type constructors with two type arguments that are contravariant in the first 
 * and covariant in the second.
 * 
 * @param <W> The witness type of the profunctor.
 */
public interface Profunctor<W> {
    /**
     * Maps functions over both type arguments simultaneously.
     * Contravariant in the first, covariant in the second.
     * 
     * Haskell: dimap :: (a -> b) -> (c -> d) -> p b c -> p a d
     */
    <A, B, C, D> Higher<Higher<W, A>, D> dimap(Function<A, B> f, Function<C, D> g, Higher<Higher<W, B>, C> pbc);

    /**
     * Maps a function over the first type argument (contravariantly).
     */
    default <A, B, C> Higher<Higher<W, A>, C> lmap(Function<A, B> f, Higher<Higher<W, B>, C> pbc) {
        return dimap(f, Function.identity(), pbc);
    }

    /**
     * Maps a function over the second type argument (covariantly).
     */
    default <A, B, C> Higher<Higher<W, A>, C> rmap(Function<B, C> g, Higher<Higher<W, A>, B> pab) {
        return dimap(Function.identity(), g, pab);
    }
}
