package io.github.senthilganeshs.fj.ds;

import java.util.function.BiFunction;

/**
 * A purely functional Queue (FIFO).
 */
public interface Queue<T> extends Collection<T> {

    @SuppressWarnings("unchecked")
    static <R> Queue<R> nil() {
        return (Queue<R>) BankersQueue.EMPTY;
    }

    @SafeVarargs
    static <R> Queue<R> of(R... values) {
        Queue<R> q = nil();
        if (values == null) return q;
        for (R val : values) {
            q = q.enqueue(val);
        }
        return q;
    }

    @SuppressWarnings("unchecked")
    static <R> Queue<R> from(Collection<R> c) {
        if (c instanceof Queue) return (Queue<R>) c;
        return (Queue<R>) c.foldl(Queue.<R>nil(), (q, r) -> q.enqueue(r));
    }

    Queue<T> enqueue(T value);
    Maybe<Tuple<T, Queue<T>>> dequeue();

    @Override
    default Collection<T> build(T input) {
        return enqueue(input);
    }

    final class BankersQueue<T> implements Queue<T> {
        private final Stack<T> front;
        private final Stack<T> back;

        static final BankersQueue<?> EMPTY = new BankersQueue<>(Stack.emptyStack(), Stack.emptyStack());

        BankersQueue(Stack<T> front, Stack<T> back) {
            this.front = front;
            this.back = back;
        }

        private static <T> Queue<T> check(Stack<T> f, Stack<T> b) {
            if (f.isEmpty()) return new BankersQueue<>(Stack.from(b.reverse()), Stack.emptyStack());
            return new BankersQueue<>(f, b);
        }

        @Override
        public Queue<T> enqueue(T value) {
            return check(front, (Stack<T>) back.build(value));
        }

        @Override
        public Maybe<Tuple<T, Queue<T>>> dequeue() {
            if (isEmpty()) return Maybe.nothing();
            return Maybe.some(Tuple.of(front.head().orElse(null), check(front.tail().orElse(Stack.emptyStack()), back)));
        }

        @Override
        public <R> R foldl(R seed, BiFunction<R, T, R> fn) {
            // Stack.foldl is LIFO (newest to oldest). 
            // In BankersQueue, front contains elements in order of removal (top is next out).
            // So front.foldl visits elements in FIFO order.
            // back contains newest elements in reverse entry order, but back.internal().foldl visits them in entry order.
            R res = front.foldl(seed, fn);
            return back.internal().foldl(res, fn);
        }

        @Override public <R> Collection<R> empty() { return nil(); }

        @Override
        public String toString() {
            return Collection.toString(this);
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Queue) {
                Queue<?> q = (Queue<?>) other;
                if (q.length() != length()) return false;
                return this.toString().equals(q.toString());
            }
            return false;
        }
        
        @Override public int hashCode() { return toString().hashCode(); }
    }
}
