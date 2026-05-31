package io.github.senthilganeshs.fj.typeclass;

import java.util.function.BiFunction;

/**
 * An algebraic structure consisting of a set together with a single 
 * binary operation and an identity element.
 */
public interface Monoid<T> extends Semigroup<T> {
    T empty();
    @Override T combine(T a, T b);

    static <R> Monoid<R> of(R empty, BiFunction<R, R, R> combine) {
        return new Monoid<R>() {
            @Override public R empty() { return empty; }
            @Override public R combine(R a, R b) { return combine.apply(a, b); }
        };
    }

    // Common Monoids
    Monoid<Integer> INTEGER_SUM = of(0, Integer::sum);
    Monoid<Integer> INTEGER_PRODUCT = of(1, (a, b) -> a * b);
    Monoid<String> STRING_CONCAT = of("", (a, b) -> a + b);
    Monoid<Double> DOUBLE_SUM = of(0.0, Double::sum);

    /**
     * Combines all elements of a collection using the Monoid.
     */
    static <T> T combineAll(Monoid<T> m, Iterable<T> ts) {
        T res = m.empty();
        for (T t : ts) {
            res = m.combine(res, t);
        }
        return res;
    }

    /**
     * Maps each element of the collection to a Monoid, and combines the results.
     */
    static <A, B> B foldMap(Monoid<B> m, java.util.function.Function<A, B> f, Iterable<A> as) {
        B res = m.empty();
        for (A a : as) {
            res = m.combine(res, f.apply(a));
        }
        return res;
    }

    /**
     * Combines all elements of a collection using the Monoid, with a separator between them.
     */
    static <T> T intercalate(Monoid<T> m, T sep, Iterable<T> ts) {
        java.util.Iterator<T> it = ts.iterator();
        if (!it.hasNext()) return m.empty();
        T res = it.next();
        while (it.hasNext()) {
            res = m.combine(res, m.combine(sep, it.next()));
        }
        return res;
    }
}
