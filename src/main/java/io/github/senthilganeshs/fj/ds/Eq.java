package io.github.senthilganeshs.fj.ds;

/**
 * A typeclass for types that support equality.
 */
public interface Eq<T> {
    boolean eq(T a, T b);

    static <R> Eq<R> fromEquals() {
        return (a, b) -> (a == b) || (a != null && a.equals(b));
    }
}
