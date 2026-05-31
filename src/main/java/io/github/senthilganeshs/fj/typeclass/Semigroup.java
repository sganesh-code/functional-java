package io.github.senthilganeshs.fj.typeclass;

/**
 * An algebraic structure consisting of a set together with a single 
 * associative binary operation.
 */
public interface Semigroup<T> {
    T combine(T a, T b);

    /**
     * Static utility for combine.
     */
    static <T> T combine(Semigroup<T> s, T a, T b) {
        return s.combine(a, b);
    }

    /**
     * Combines a value with itself n times.
     */
    static <T> T combineN(Semigroup<T> s, T a, int n) {
        if (n <= 0) throw new IllegalArgumentException("n must be positive for Semigroup.combineN");
        T res = a;
        for (int i = 1; i < n; i++) {
            res = s.combine(res, a);
        }
        return res;
    }
}
