package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.typeclass.Ord;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BiFunction;

/**
 * A purely functional Priority Queue implemented as a Leftist Heap.
 * Supports O(1) findMin and O(log n) merge, insert, and deleteMin.
 * 
 * @param <T> The type of elements in the priority queue.
 */
public interface PriorityQueue<T> extends Collection<T> {

    Maybe<T> findMin();

    Maybe<PriorityQueue<T>> deleteMin();

    PriorityQueue<T> merge(PriorityQueue<T> other);

    static <R extends Comparable<R>> PriorityQueue<R> nil() {
        return empty(Ord.<R>natural());
    }

    static <R> PriorityQueue<R> empty(Ord<R> ord) {
        return new LeftistHeap<>(ord, null, null, null);
    }

    @SafeVarargs
    static <R extends Comparable<R>> PriorityQueue<R> of(R... values) {
        return of(Ord.<R>natural(), values);
    }

    @SafeVarargs
    static <R> PriorityQueue<R> of(Ord<R> ord, R... values) {
        PriorityQueue<R> pq = empty(ord);
        for (R val : values) {
            pq = (PriorityQueue<R>) pq.build(val);
        }
        return pq;
    }

    final class LeftistHeap<T> implements PriorityQueue<T> {
        private final Ord<T> ord;
        private final T value;
        private final LeftistHeap<T> left;
        private final LeftistHeap<T> right;
        private final int rank;

        LeftistHeap(Ord<T> ord, T value, LeftistHeap<T> left, LeftistHeap<T> right) {
            this.ord = ord;
            this.value = value;
            this.left = left;
            this.right = right;
            this.rank = (right == null) ? 0 : right.rank + 1;
        }

        @Override
        public Maybe<T> findMin() {
            return value == null ? Maybe.nothing() : Maybe.some(value);
        }

        @Override
        public Maybe<PriorityQueue<T>> deleteMin() {
            return value == null ? Maybe.nothing() : Maybe.some(left.merge(right));
        }

        @Override
        public PriorityQueue<T> merge(PriorityQueue<T> other) {
            if (this.value == null) return other;
            LeftistHeap<T> o = (LeftistHeap<T>) other;
            if (o.value == null) return this;

            if (ord.compare(this.value, o.value) > 0) {
                return o.merge(this);
            }

            return makeHeap(this.value, this.left, (LeftistHeap<T>) this.right.merge(o));
        }

        private LeftistHeap<T> makeHeap(T v, LeftistHeap<T> l, LeftistHeap<T> r) {
            if (l.rank < r.rank) return new LeftistHeap<>(ord, v, r, l);
            return new LeftistHeap<>(ord, v, l, r);
        }

        @Override
        public <R> Collection<R> empty() {
            return List.nil();
        }

        @Override
        public Collection<T> build(T input) {
            return merge(new LeftistHeap<>(ord, input, emptyHeap(ord), emptyHeap(ord)));
        }
        
        private LeftistHeap<T> emptyHeap(Ord<T> ord) {
            return new LeftistHeap<>(ord, null, null, null);
        }

        @Override
        public <R> R foldl(R seed, BiFunction<R, T, R> fn) {
            R acc = seed;
            Deque<LeftistHeap<T>> stack = new ArrayDeque<>();
            stack.push(this);
            while (!stack.isEmpty()) {
                LeftistHeap<T> curr = stack.pop();
                if (curr.value != null) {
                    acc = fn.apply(acc, curr.value);
                    if (curr.right != null) stack.push(curr.right);
                    if (curr.left != null) stack.push(curr.left);
                }
            }
            return acc;
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
            return this.toString().equals(o.toString()); 
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }
    }
}
