package io.github.senthilganeshs.fj.test;

import io.github.senthilganeshs.fj.ds.Maybe;
import io.github.senthilganeshs.fj.ds.Tuple;
import io.github.senthilganeshs.fj.ds.List;
import java.util.Random;
import java.util.function.Predicate;

/**
 * A property that can be verified against generated data.
 */
public record Property<A>(Gen<A> gen, Predicate<A> predicate, Maybe<Shrink<A>> shrinker) {

    public static <A> Property<A> forAll(Gen<A> gen, Predicate<A> predicate) {
        return new Property<>(gen, predicate, Maybe.nothing());
    }

    public static <A> Property<A> forAll(Gen<A> gen, Shrink<A> shrinker, Predicate<A> predicate) {
        return new Property<>(gen, predicate, Maybe.some(shrinker));
    }

    /**
     * Checks the property against a specified number of trials.
     * 
     * @param trials Number of random tests to perform.
     * @return Maybe containing the failing value if any, otherwise Nothing.
     */
    public Maybe<A> check(int trials) {
        Random rnd = new Random();
        for (int i = 0; i < trials; i++) {
            A sample = gen.sample().apply(rnd);
            if (!predicate.test(sample)) {
                // If it fails, try to shrink it
                return Maybe.some(shrink(sample));
            }
        }
        return Maybe.nothing();
    }

    private A shrink(A value) {
        return shrinker.map(s -> {
            List<A> shrunk = s.apply(value);
            return shrunk.find(v -> !predicate.test(v))
                .map(this::shrink)
                .orElse(value);
        }).orElse(value);
    }

    /**
     * Asserts that the property holds. Throws an AssertionError if it fails.
     */
    public void assertTrue(int trials) {
        check(trials).forEach(failed -> {
            throw new AssertionError("Property failed for value: " + failed);
        });
    }
}
