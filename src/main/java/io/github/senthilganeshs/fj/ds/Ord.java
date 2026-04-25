package io.github.senthilganeshs.fj.ds;

/**
 * A typeclass for types that support ordering.
 */
public interface Ord<T> extends Eq<T> {
    int compare(T a, T b);

    @Override
    default boolean eq(T a, T b) {
        return compare(a, b) == 0;
    }

    default boolean lt(T a, T b) { return compare(a, b) < 0; }
    default boolean lte(T a, T b) { return compare(a, b) <= 0; }
    default boolean gt(T a, T b) { return compare(a, b) > 0; }
    default boolean gte(T a, T b) { return compare(a, b) >= 0; }

    static <R extends Comparable<R>> Ord<R> natural() {
        return (a, b) -> a.compareTo(b);
    }

    static <R> Ord<R> fromComparator(java.util.Comparator<R> comparator) {
        return (a, b) -> comparator.compare(a, b);
    }
}
