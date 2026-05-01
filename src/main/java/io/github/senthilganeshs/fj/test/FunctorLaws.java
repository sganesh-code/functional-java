package io.github.senthilganeshs.fj.test;

import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.typeclass.Functor;
import java.util.function.Function;

/**
 * Utility to verify Functor laws.
 */
public final class FunctorLaws {

    public static <W, A> void check(Functor<W> functor, Gen<Higher<W, A>> gen, int trials) {
        // Identity: fmap id == id
        Property.forAll(gen, fa -> 
            functor.map(Function.identity(), fa).equals(fa)
        ).assertTrue(trials);

        // Composition: fmap (f . g) == fmap f . fmap g
        // This is harder to check generally without Gen for functions, 
        // but we can check with specific functions.
        Function<Integer, Integer> f = i -> i + 1;
        Function<Integer, Integer> g = i -> i * 2;
        Property.forAll((Gen<Higher<W, Integer>>) (Gen) gen, fa -> {
            Higher<W, Integer> left = functor.map(f.compose(g), fa);
            Higher<W, Integer> right = functor.map(f, functor.map(g, fa));
            return left.equals(right);
        }).assertTrue(trials);
    }
}
