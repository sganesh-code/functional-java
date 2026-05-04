package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.typeclass.Ord;
import java.util.function.BiFunction;

/**
 * A purely functional Priority Queue implementation using a Skew Leftist Heap.
 */
public interface PriorityQueue<T> extends Collection<T> {

    @SuppressWarnings("unchecked")
    static <R extends Comparable<R>> PriorityQueue<R> nil() {
        return nilWithOrd(Ord.<R>natural());
    }

    @SuppressWarnings("unchecked")
    static <R> PriorityQueue<R> nilWithOrd(Ord<R> ord) {
        return new LeftistHeap<>(ord, 0, null, null, null);
    }

    @SafeVarargs
    static <R extends Comparable<R>> PriorityQueue<R> of(R... values) {
        return of(Ord.natural(), values);
    }

    @SafeVarargs
    static <R> PriorityQueue<R> of(Ord<R> ord, R... values) {
        PriorityQueue<R> pq = nilWithOrd(ord);
        if (values == null) return pq;
        for (R val : values) {
            pq = (PriorityQueue<R>) pq.build(val);
        }
        return pq;
    }

    default Maybe<T> findMin() {
        return peek();
    }

    default Maybe<PriorityQueue<T>> deleteMin() {
        return pop();
    }

    Maybe<T> peek();
    Maybe<PriorityQueue<T>> pop();

    @Override
    default <R> Collection<R> empty() {
        return (Collection<R>) (PriorityQueue<R>) PriorityQueue.nil();
    }

    @Override
    default Collection<T> build(T input) {
        return insert(input);
    }

    PriorityQueue<T> insert(T value);
    PriorityQueue<T> merge(PriorityQueue<T> other);

    final class LeftistHeap<T> implements PriorityQueue<T> {
        private final Ord<T> ord;
        private final int rank;
        private final T value;
        private final PriorityQueue<T> left;
        private final PriorityQueue<T> right;

        LeftistHeap(Ord<T> ord, int rank, T value, PriorityQueue<T> left, PriorityQueue<T> right) {
            this.ord = ord;
            this.rank = rank;
            this.value = value;
            this.left = left;
            this.right = right;
        }

        private static <T> int rank(PriorityQueue<T> pq) {
            return (pq instanceof LeftistHeap && ((LeftistHeap<T>) pq).value != null) 
                ? ((LeftistHeap<T>) pq).rank : 0;
        }

        private static <T> PriorityQueue<T> make(Ord<T> ord, T v, PriorityQueue<T> a, PriorityQueue<T> b) {
            if (rank(a) >= rank(b)) return new LeftistHeap<>(ord, rank(b) + 1, v, a, b);
            return new LeftistHeap<>(ord, rank(a) + 1, v, b, a);
        }

        @Override
        public PriorityQueue<T> merge(PriorityQueue<T> other) {
            return merge(this, other);
        }

        private PriorityQueue<T> merge(PriorityQueue<T> h1, PriorityQueue<T> h2) {
            if (h1.isEmpty()) return h2;
            if (h2.isEmpty()) return h1;

            LeftistHeap<T> l1 = (LeftistHeap<T>) h1;
            LeftistHeap<T> l2 = (LeftistHeap<T>) h2;

            if (ord.compare(l1.value, l2.value) <= 0) {
                return make(ord, l1.value, l1.left, merge(l1.right, l2));
            }
            return make(ord, l2.value, l2.left, merge(l1, l2.right));
        }

        @Override
        public PriorityQueue<T> insert(T value) {
            return merge(this, new LeftistHeap<>(ord, 1, value, nilWithOrd(ord), nilWithOrd(ord)));
        }

        @Override
        public Maybe<T> peek() {
            return Maybe.of(value);
        }

        @Override
        public Maybe<PriorityQueue<T>> pop() {
            return isEmpty() ? Maybe.nothing() : Maybe.some(merge(left, right));
        }

        @Override
        public <R> R foldl(R seed, BiFunction<R, T, R> fn) {
            if (isEmpty()) return seed;
            R res = fn.apply(seed, value);
            res = left.foldl(res, fn);
            return right.foldl(res, fn);
        }

        @Override
        public boolean isEmpty() { return value == null; }

        @Override
        public String toString() {
            return Collection.toString(this);
        }
    }
}
