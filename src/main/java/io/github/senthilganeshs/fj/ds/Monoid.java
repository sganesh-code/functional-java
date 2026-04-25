package io.github.senthilganeshs.fj.ds;

import java.util.function.BiFunction;

/**
 * An algebraic structure consisting of a set together with a single 
 * binary operation and an identity element.
 */
public interface Monoid<T> {
    T empty();
    T combine(T a, T b);

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
}
