package io.github.senthilganeshs.fj.test;

import io.github.senthilganeshs.fj.ds.Tuple;
import io.github.senthilganeshs.fj.typeclass.Monoid;

/**
 * Utility to verify Monoid laws.
 */
public final class MonoidLaws {

    public static <A> void check(Monoid<A> monoid, Gen<A> gen, int trials) {
        // Identity: empty <> a == a
        Property.forAll(gen, a -> monoid.combine(monoid.empty(), a).equals(a)).assertTrue(trials);
        
        // Identity: a <> empty == a
        Property.forAll(gen, a -> monoid.combine(a, monoid.empty()).equals(a)).assertTrue(trials);

        // Associativity: (a <> b) <> c == a <> (b <> c)
        Property.forAll(
            gen.flatMap(a -> gen.flatMap(b -> gen.map(c -> Tuple.of(a, Tuple.of(b, c))))),
            t -> {
                A a = t.getA().orElse(null);
                A b = t.getB().orElse(null).getA().orElse(null);
                A c = t.getB().orElse(null).getB().orElse(null);
                A left = monoid.combine(monoid.combine(a, b), c);
                A right = monoid.combine(a, monoid.combine(b, c));
                return left.equals(right);
            }
        ).assertTrue(trials);
    }
}
