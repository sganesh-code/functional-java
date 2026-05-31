package io.github.senthilganeshs.fj.typeclass;

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

    default <A> Ord<A> contramap(java.util.function.Function<A, T> f) {
        return (a1, a2) -> compare(f.apply(a1), f.apply(a2));
    }

    static <R extends Comparable<R>> Ord<R> natural() {
        return (a, b) -> a.compareTo(b);
    }

    static <R> Ord<R> fromComparator(java.util.Comparator<R> comparator) {
        return (a, b) -> comparator.compare(a, b);
    }

    /**
     * Returns the smaller of two values.
     */
    static <T> T min(Ord<T> ord, T a, T b) {
        return ord.lte(a, b) ? a : b;
    }

    /**
     * Returns the larger of two values.
     */
    static <T> T max(Ord<T> ord, T a, T b) {
        return ord.gte(a, b) ? a : b;
    }

    /**
     * Clamps a value between a low and high bound.
     */
    static <T> T clamp(Ord<T> ord, T low, T high, T val) {
        if (ord.lt(val, low)) return low;
        if (ord.gt(val, high)) return high;
        return val;
    }

    /**
     * Returns true if a value is between low and high (inclusive).
     */
    static <T> boolean between(Ord<T> ord, T low, T high, T val) {
        return ord.gte(val, low) && ord.lte(val, high);
    }

    static <A, B> Ord<A> contramap(Ord<B> ord, java.util.function.Function<A, B> f) {
        return ord.contramap(f);
    }
}
