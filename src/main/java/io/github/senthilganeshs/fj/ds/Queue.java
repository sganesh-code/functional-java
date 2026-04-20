package io.github.senthilganeshs.fj.ds;

import java.util.function.BiFunction;

public interface Queue<T> extends Collection<T> {

    Maybe<Tuple<T, Queue<T>>> dequeue();

    static <R> Queue<R> nil() {
        return new BankersQueue<>(Stack.emptyStack(), Stack.emptyStack());
    }

    @SafeVarargs
    static <R> Queue<R> of(R... values) {
        Queue<R> q = nil();
        for (R v : values) {
            q = (Queue<R>) q.build(v);
        }
        return q;
    }

    final class BankersQueue<T> implements Queue<T> {
        private final Stack<T> front;
        private final Stack<T> rear;

        BankersQueue(Stack<T> front, Stack<T> rear) {
            this.front = front;
            this.rear = rear;
        }

        @Override
        public <R> Collection<R> empty() {
            return Queue.nil();
        }

        @Override
        public Collection<T> build(T input) {
            return check(front, (Stack<T>) rear.build(input));
        }

        @Override
        public <R> R foldl(R seed, BiFunction<R, T, R> fn) {
            R frontRes = front.foldl(seed, fn);
            return rear.reverse().foldl(frontRes, fn);
        }

        @Override
        public Maybe<Tuple<T, Queue<T>>> dequeue() {
            return (Maybe<Tuple<T, Queue<T>>>) front.head().flatMap(h -> 
                front.tail().map(t -> 
                    Tuple.of(h, check(t, rear))
                )
            );
        }
        
        // Internal balance check
        private Queue<T> check(Stack<T> f, Stack<T> r) {
            if (f.head().isNothing()) {
                return new BankersQueue<>((Stack<T>) r.reverse(), Stack.emptyStack());
            }
            return new BankersQueue<>(f, r);
        }

        @Override
        public String toString() {
            return foldl("[", (r, t) -> r + (r.equals("[") ? "" : ",") + t) + "]";
        }
        
        @Override
        public boolean equals(Object other) {
            if (other == null) return false;
            if (other == this) return true;
            if (!(other instanceof Queue)) return false;
            Queue<T> qOther = (Queue<T>) other;
            if (this.length() != qOther.length()) return false;
            
            // This is a bit slow but correct for now
            return this.toString().equals(qOther.toString());
        }
    }
}
