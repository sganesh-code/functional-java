package io.github.senthilganeshs.fj.typeclass;

import io.github.senthilganeshs.fj.hkt.Higher;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A typeclass for data structures that can be folded to a summary value.
 * 
 * @param <W> The witness type of the foldable structure.
 */
public interface Foldable<W> {
    /**
     * Left-associative fold of a structure.
     */
    <A, B> B foldl(BiFunction<B, A, B> f, B seed, Higher<W, A> fa);

    /**
     * Right-associative fold of a structure.
     */
    <A, B> B foldr(BiFunction<A, B, B> f, B seed, Higher<W, A> fa);

    /**
     * Maps each element of the structure to a Monoid, and combines the results.
     */
    default <A, B> B foldMap(Monoid<B> m, Function<A, B> fn, Higher<W, A> fa) {
        return foldl((b, a) -> m.combine(b, fn.apply(a)), m.empty(), fa);
    }

    /**
     * Combine the elements of a structure using a Monoid.
     */
    default <A> A fold(Monoid<A> m, Higher<W, A> fa) {
        return foldMap(m, Function.identity(), fa);
    }

    /**
     * Static utility for foldMap.
     */
    static <W, A, B> B foldMap(Foldable<W> f, Monoid<B> m, Function<A, B> fn, Higher<W, A> fa) {
        return f.foldMap(m, fn, fa);
    }

    /**
     * Static utility for fold.
     */
    static <W, A> A fold(Foldable<W> f, Monoid<A> m, Higher<W, A> fa) {
        return f.fold(m, fa);
    }
}
