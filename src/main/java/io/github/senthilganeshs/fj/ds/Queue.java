package io.github.senthilganeshs.fj.ds;

import java.util.function.BiFunction;

/**
 * A purely functional Queue (FIFO).
 * 
 * @param <T> The type of elements.
 */
public interface Queue<T> extends Collection<T> {

    Maybe<Tuple<T, Queue<T>>> dequeue();

    @SuppressWarnings("unchecked")
    static <R> Queue<R> from(Collection<R> c) {
        return (Queue<R>) c.foldl(Queue.<R>nil(), (q, r) -> (Queue<R>) q.build(r));
    }

    @SuppressWarnings("unchecked")
    @Override
    default <R> Queue<R> map(java.util.function.Function<T, R> fn) {
        return from(Collection.super.map(fn));
    }

    static <R> Queue<R> nil() {
        return new BankersQueue<>(Stack.emptyStack(), Stack.emptyStack());
    }

    @SafeVarargs
    static <R> Queue<R> of(R... values) {
        Queue<R> q = nil();
        if (values == null) return q;
        for (R val : values) q = (Queue<R>) q.build(val);
        return q;
    }

    final class BankersQueue<T> implements Queue<T> {
        private final Stack<T> front;
        private final Stack<T> back;

        BankersQueue(Stack<T> front, Stack<T> back) {
            this.front = front;
            this.back = back;
        }

        @Override
        public Maybe<Tuple<T, Queue<T>>> dequeue() {
            return front.head().map(h -> Tuple.of(h, check(front.tail().orElse(Stack.emptyStack()), back)));
        }

        private Queue<T> check(Stack<T> f, Stack<T> b) {
            if (f.length() == 0) {
                // To move back to front, we must reverse LIFO back to get FIFO front
                return new BankersQueue<>(b.reverse(), Stack.emptyStack());
            }
            return new BankersQueue<>(f, b);
        }

        @Override
        public <R> Collection<R> empty() {
            return Queue.nil();
        }

        @Override
        public Collection<T> build(T input) {
            return check(front, (Stack<T>) back.build(input));
        }

        @Override
        public <R> R foldl(R seed, BiFunction<R, T, R> fn) {
            R res = front.foldl(seed, fn);
            return back.reverse().foldl(res, fn);
        }

        @Override
        public String toString() {
            return foldl("[", (r, t) -> r + (r.equals("[") ? "" : ",") + t) + "]";
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(Object other) {
            if (other == null) return false;
            if (other == this) return true;
            if (!(other instanceof Queue)) return false;
            Queue<?> o = (Queue<?>) other;
            if (o.length() != length()) return false;
            return this.toString().equals(o.toString());
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }
    }
}
