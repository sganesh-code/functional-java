package io.github.senthilganeshs.fj.ds;

import java.util.function.BiFunction;

/**
 * A purely functional Queue (FIFO).
 */
public interface Queue<T> extends Collection<T> {

    @SuppressWarnings("unchecked")
    static <R> Queue<R> from(Collection<R> c) {
        if (c instanceof Queue) return (Queue<R>) c;
        return (Queue<R>) c.foldl(Queue.<R>nil(), (q, r) -> (Queue<R>) q.build(r));
    }

    @SuppressWarnings("unchecked")
    static <R> Queue<R> nil() {
        return (Queue<R>) BankersQueue.nil();
    }

    @SafeVarargs
    static <R> Queue<R> of(R... values) {
        Queue<R> q = nil();
        if (values == null) return q;
        for (R val : values) {
            q = (Queue<R>) q.build(val);
        }
        return q;
    }

    Maybe<T> head();
    Maybe<Queue<T>> tail();

    @Override
    default <R> Collection<R> empty() {
        return nil();
    }

    @Override
    default Collection<T> build(T input) {
        return enqueue(input);
    }

    Queue<T> enqueue(T value);
    Maybe<Tuple<T, Queue<T>>> dequeue();

    final class BankersQueue<T> implements Queue<T> {
        private final Stack<T> front;
        private final Stack<T> back;

        BankersQueue(Stack<T> front, Stack<T> back) {
            this.front = front;
            this.back = back;
        }

        static <T> Queue<T> nil() {
            return new BankersQueue<>(Stack.emptyStack(), Stack.emptyStack());
        }

        private Queue<T> check(Stack<T> f, Stack<T> b) {
            if (f.isEmpty()) {
                return new BankersQueue<>(Stack.from(b.reverse()), Stack.emptyStack());
            }
            return new BankersQueue<>(f, b);
        }

        @Override
        public Queue<T> enqueue(T value) {
            return check(front, (Stack<T>) back.build(value));
        }

        @Override
        public Maybe<Tuple<T, Queue<T>>> dequeue() {
            return front.head().map(h -> Tuple.of(h, check(front.tail().orElse(Stack.emptyStack()), back)));
        }

        @Override
        public Maybe<T> head() {
            return front.head();
        }

        @Override
        public Maybe<Queue<T>> tail() {
            return front.head().map(h -> check(front.tail().orElse(Stack.emptyStack()), back));
        }

        @Override
        public <R> R foldl(R seed, BiFunction<R, T, R> fn) {
            R res = front.foldl(seed, fn);
            return back.reverse().foldl(res, fn);
        }

        @Override public <R> Collection<R> empty() { return nil(); }

        @Override
        public String toString() {
            return "Queue(" + front + ", " + back + ")";
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
