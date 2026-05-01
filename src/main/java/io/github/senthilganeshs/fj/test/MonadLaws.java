package io.github.senthilganeshs.fj.test;

import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.typeclass.Monad;
import java.util.function.Function;

/**
 * Utility to verify Monad laws.
 */
public final class MonadLaws {

    public static <W, A> void check(Monad<W> monad, Gen<A> genA, int trials) {
        // Left Identity: pure a >>= f == f a
        Function<Integer, Higher<W, Integer>> f = i -> monad.pure(i + 1);
        Property.forAll((Gen<Integer>) (Gen) genA, a -> {
            Higher<W, Integer> left = monad.flatMap(f, monad.pure(a));
            Higher<W, Integer> right = f.apply(a);
            return left.equals(right);
        }).assertTrue(trials);

        // Right Identity: m >>= pure == m
        Property.forAll((Gen<Higher<W, A>>) (Gen) genA.map(monad::pure), m -> {
            Higher<W, A> left = monad.flatMap(monad::pure, m);
            return left.equals(m);
        }).assertTrue(trials);

        // Associativity: (m >>= f) >>= g == m >>= (\x -> f x >>= g)
        Function<Integer, Higher<W, Integer>> g = i -> monad.pure(i * 2);
        Property.forAll((Gen<Higher<W, Integer>>) (Gen) genA.map(monad::pure), m -> {
            Higher<W, Integer> left = monad.flatMap(g, monad.flatMap(f, m));
            Higher<W, Integer> right = monad.flatMap(x -> monad.flatMap(g, f.apply(x)), m);
            return left.equals(right);
        }).assertTrue(trials);
    }
}
