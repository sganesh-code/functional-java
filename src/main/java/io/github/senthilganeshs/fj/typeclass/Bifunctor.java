package io.github.senthilganeshs.fj.typeclass;

import io.github.senthilganeshs.fj.hkt.Higher;
import java.util.function.Function;

/**
 * A typeclass for type constructors with two type arguments that support mapping over both.
 * 
 * @param <W> The witness type of the bifunctor.
 */
public interface Bifunctor<W> {
    /**
     * Maps functions over both type arguments simultaneously.
     */
    <A, B, C, D> Higher<Higher<W, C>, D> bimap(Function<A, C> fa, Function<B, D> fb, Higher<Higher<W, A>, B> fab);

    /**
     * Maps a function over the first type argument.
     */
    default <A, B, C> Higher<Higher<W, C>, B> first(Function<A, C> fn, Higher<Higher<W, A>, B> fab) {
        return bimap(fn, Function.identity(), fab);
    }

    /**
     * Maps a function over the second type argument.
     */
    default <A, B, D> Higher<Higher<W, A>, D> second(Function<B, D> fn, Higher<Higher<W, A>, B> fab) {
        return bimap(Function.identity(), fn, fab);
    }
}
