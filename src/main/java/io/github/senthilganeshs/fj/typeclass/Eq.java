package io.github.senthilganeshs.fj.typeclass;

/**
 * A typeclass for types that support equality.
 */
public interface Eq<T> {
    boolean eq(T a, T b);

    default <A> Eq<A> contramap(java.util.function.Function<A, T> f) {
        return (a1, a2) -> eq(f.apply(a1), f.apply(a2));
    }

    static <R> Eq<R> fromEquals() {
        return (a, b) -> (a == b) || (a != null && a.equals(b));
    }

    static <A, B> Eq<A> contramap(Eq<B> eq, java.util.function.Function<A, B> f) {
        return eq.contramap(f);
    }
}
