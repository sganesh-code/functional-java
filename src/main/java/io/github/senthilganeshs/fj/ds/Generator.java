package io.github.senthilganeshs.fj.ds;

import java.util.function.UnaryOperator;

/**
 * Utility for generating potentially infinite sequences of values.
 */
public interface Generator {

    /**
     * Generates an infinite sequence by repeatedly applying a function to a seed value.
     */
    static <T> LazyList<T> iterate(T seed, UnaryOperator<T> f) {
        return LazyList.iterate(seed, f);
    }

    /**
     * Generates an infinite sequence by repeating a single value.
     */
    static <T> LazyList<T> repeat(T value) {
        return LazyList.cons(value, () -> repeat(value));
    }

    /**
     * Generates a sequence of integers from start to end (exclusive).
     */
    static LazyList<Integer> range(int start, int end) {
        if (start >= end) return LazyList.nil();
        return LazyList.cons(start, () -> range(start + 1, end));
    }

    /**
     * Generates an infinite sequence by cycling through the elements of a collection.
     */
    static <T> LazyList<T> cycle(Collection<T> c) {
        if (c.isEmpty()) return LazyList.nil();
        List<T> list = List.from(c);
        return cycleHelper(list, 0);
    }

    private static <T> LazyList<T> cycleHelper(List<T> items, int index) {
        return LazyList.cons(items.atIndex(index % items.length()).orElse(null), () -> cycleHelper(items, index + 1));
    }
}
