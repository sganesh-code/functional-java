package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.typeclass.Monoid;
import java.util.function.Function;

/**
 * Represent a function from a type to itself.
 * Forms a monoid under function composition.
 */
public record Endo<A>(Function<A, A> run) {
    public static <A> Monoid<Endo<A>> monoid() {
        return new Monoid<>() {
            @Override
            public Endo<A> empty() {
                return new Endo<>(Function.identity());
            }

            @Override
            public Endo<A> combine(Endo<A> a, Endo<A> b) {
                return new Endo<>(a.run().compose(b.run()));
            }
        };
    }
}
