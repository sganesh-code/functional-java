package io.github.senthilganeshs.fj.typeclass;

import io.github.senthilganeshs.fj.hkt.Higher;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A typeclass for functors that support functional application.
 * 
 * @param <W> The witness type of the applicative.
 */
public interface Applicative<W> extends Functor<W> {
    /**
     * Lifts a value into the applicative context.
     */
    <A> Higher<W, A> pure(A a);

    /**
     * Applies a function within an applicative context to a value within an applicative context.
     */
    <A, B> Higher<W, B> ap(Higher<W, Function<A, B>> ff, Higher<W, A> fa);

    @Override
    default <A, B> Higher<W, B> map(Function<A, B> fn, Higher<W, A> fa) {
        return ap(pure(fn), fa);
    }

    /**
     * Combines two applicative values using a binary function.
     */
    default <A, B, C> Higher<W, C> liftA2(BiFunction<A, B, C> fn, Higher<W, A> fa, Higher<W, B> fb) {
        return ap(map(a -> b -> fn.apply(a, b), fa), fb);
    }
}
