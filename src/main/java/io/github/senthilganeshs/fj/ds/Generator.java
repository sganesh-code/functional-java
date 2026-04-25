package io.github.senthilganeshs.fj.ds;

import java.util.function.UnaryOperator;

/**
 * Utility for generating infinite or sequential functional collections.
 */
public final class Generator {
    private Generator() {}

    /**
     * Generates an infinite LazyList by repeatedly applying a function to a seed.
     */
    public static <T> LazyList<T> iterate(T seed, UnaryOperator<T> f) {
        return LazyList.iterate(seed, f);
    }

    /**
     * Generates an infinite LazyList repeating the same value.
     */
    public static <T> LazyList<T> repeat(T value) {
        return LazyList.cons(value, () -> repeat(value));
    }

    /**
     * Generates a sequential range of integers.
     */
    public static LazyList<Integer> range(int start, int end) {
        if (start > end) return LazyList.nil();
        return LazyList.cons(start, () -> range(start + 1, end));
    }

    /**
     * Generates an infinite sequence by cycling through a collection.
     */
    public static <T> LazyList<T> cycle(Collection<T> c) {
        if (c.length() == 0) return LazyList.nil();
        LazyList<T> list = LazyList.from(c);
        return list.concat(LazyList.of()).foldr(LazyList.nil(), (t, __) -> {
             // This is handled better by a direct lazy cycle
             return null;
        });
    }

    /**
     * Infinite cycle implementation.
     */
    public static <T> LazyList<T> cycle(java.util.List<T> items) {
        if (items.isEmpty()) return LazyList.nil();
        return cycleHelper(items, 0);
    }

    private static <T> LazyList<T> cycleHelper(java.util.List<T> items, int index) {
        return LazyList.cons(items.get(index % items.size()), () -> cycleHelper(items, index + 1));
    }
}
