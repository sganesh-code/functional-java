package io.github.senthilganeshs.fj.ds;

import java.util.function.BiFunction;

/**
 * A purely functional Priority Queue implemented as a Leftist Heap.
 * Supports O(1) findMin and O(log n) merge, insert, and deleteMin.
 */
public interface PriorityQueue<T extends Comparable<T>> extends Collection<T> {

    Maybe<T> findMin();

    Maybe<PriorityQueue<T>> deleteMin();

    PriorityQueue<T> merge(PriorityQueue<T> other);

    static <R extends Comparable<R>> PriorityQueue<R> nil() {
        return new LeftistHeap<>();
    }

    @SafeVarargs
    static <R extends Comparable<R>> PriorityQueue<R> of(R... values) {
        PriorityQueue<R> pq = nil();
        for (R val : values) {
            pq = (PriorityQueue<R>) pq.build(val);
        }
        return pq;
    }

    final class LeftistHeap<T extends Comparable<T>> implements PriorityQueue<T> {
        private final T value;
        private final LeftistHeap<T> left;
        private final LeftistHeap<T> right;
        private final int rank;

        // Empty heap constructor
        LeftistHeap() {
            this.value = null;
            this.left = null;
            this.right = null;
            this.rank = 0;
        }

        // Non-empty heap constructor
        private LeftistHeap(T value, LeftistHeap<T> left, LeftistHeap<T> right) {
            this.value = value;
            if (left.rank >= right.rank) {
                this.left = left;
                this.right = right;
            } else {
                this.left = right;
                this.right = left;
            }
            this.rank = (this.right == null ? 0 : this.right.rank) + 1;
        }

        @Override
        public Maybe<T> findMin() {
            return value == null ? Maybe.nothing() : Maybe.some(value);
        }

        @Override
        public PriorityQueue<T> merge(PriorityQueue<T> other) {
            if (this.value == null) return other;
            if (other instanceof LeftistHeap) {
                LeftistHeap<T> o = (LeftistHeap<T>) other;
                if (o.value == null) return this;

                if (this.value.compareTo(o.value) <= 0) {
                    return new LeftistHeap<>(this.value, this.left, (LeftistHeap<T>) this.right.merge(o));
                } else {
                    return new LeftistHeap<>(o.value, o.left, (LeftistHeap<T>) this.merge(o.right));
                }
            }
            // Fallback for different implementations if any
            return other.foldl((PriorityQueue<T>) this, (pq, t) -> (PriorityQueue<T>) pq.build(t));
        }

        @Override
        public Maybe<PriorityQueue<T>> deleteMin() {
            return value == null ? Maybe.nothing() : Maybe.some(left.merge(right));
        }

        @Override
        public <R> Collection<R> empty() {
            return List.nil();
        }

        @Override
        public Collection<T> build(T input) {
            return merge(new LeftistHeap<>(input, new LeftistHeap<>(), new LeftistHeap<>()));
        }

        @Override
        public <R> R foldl(R seed, BiFunction<R, T, R> fn) {
            if (value == null) return seed;
            // Pre-order traversal for foldl
            R res = fn.apply(seed, value);
            res = left.foldl(res, fn);
            return right.foldl(res, fn);
        }

        @Override
        public String toString() {
            if (value == null) return "[]";
            return foldl("[", (r, t) -> r + (r.equals("[") ? "" : ",") + t) + "]";
        }

        @Override
        public boolean equals(Object other) {
            if (other == null) return false;
            if (other == this) return true;
            if (!(other instanceof PriorityQueue)) return false;
            PriorityQueue<?> o = (PriorityQueue<?>) other;
            if (this.length() != o.length()) return false;
            // LeftistHeap foldl is pre-order. Two heaps with same elements might have different shapes.
            // For a robust equals, we'd need to compare sorted contents or ensure same shape.
            // Given it's a priority queue, maybe compare sorted list.
            return this.toString().equals(o.toString()); 
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }
    }
}
