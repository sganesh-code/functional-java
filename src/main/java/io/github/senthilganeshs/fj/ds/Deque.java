package io.github.senthilganeshs.fj.ds;

import java.util.function.BiFunction;

/**
 * A purely functional Double-Ended Queue (Deque).
 * Supports O(1) amortized access/removal from both ends.
 */
public interface Deque<T> extends Collection<T> {

    Deque<T> pushFront(T value);
    Deque<T> pushBack(T value);

    Maybe<Tuple<T, Deque<T>>> popFront();
    Maybe<Tuple<T, Deque<T>>> popBack();

    static <R> Deque<R> nil() {
        return new BankersDeque<>(Stack.emptyStack(), Stack.emptyStack());
    }

    @SafeVarargs
    static <R> Deque<R> of(R... values) {
        Deque<R> d = nil();
        for (R val : values) {
            d = d.pushBack(val);
        }
        return d;
    }

    final class BankersDeque<T> implements Deque<T> {
        private final Stack<T> front;
        private final Stack<T> back;

        BankersDeque(Stack<T> front, Stack<T> back) {
            this.front = front;
            this.back = back;
        }

        @Override
        public Deque<T> pushFront(T value) {
            return check((Stack<T>) front.build(value), back);
        }

        @Override
        public Deque<T> pushBack(T value) {
            return check(front, (Stack<T>) back.build(value));
        }

        @Override
        public Maybe<Tuple<T, Deque<T>>> popFront() {
            return (Maybe<Tuple<T, Deque<T>>>) front.head().flatMap(h -> 
                front.tail().map(t -> 
                    Tuple.of(h, check(t, back))
                )
            );
        }

        @Override
        public Maybe<Tuple<T, Deque<T>>> popBack() {
            return (Maybe<Tuple<T, Deque<T>>>) back.head().flatMap(h -> 
                back.tail().map(t -> 
                    Tuple.of(h, check(front, t))
                )
            );
        }

        private Deque<T> check(Stack<T> f, Stack<T> b) {
            int fLen = f.length();
            int bLen = b.length();
            if (fLen == 0 && bLen > 0) {
                int mid = bLen / 2;
                // b is [last, ..., mid, ..., first]
                // front should get [first, ..., mid-1]
                // back should stay [last, ..., mid]
                Stack<T> newF = (Stack<T>) b.drop(mid).reverse();
                Stack<T> newB = (Stack<T>) b.take(mid);
                return new BankersDeque<>(newF, newB);
            } else if (bLen == 0 && fLen > 0) {
                int mid = fLen / 2;
                Stack<T> newB = (Stack<T>) f.drop(mid).reverse();
                Stack<T> newF = (Stack<T>) f.take(mid);
                return new BankersDeque<>(newF, newB);
            }
            return new BankersDeque<>(f, b);
        }

        @Override
        public <R> Collection<R> empty() {
            return Deque.nil();
        }

        @Override
        public Collection<T> build(T input) {
            return pushBack(input);
        }

        @Override
        public <R> R foldl(R seed, BiFunction<R, T, R> fn) {
            R frontRes = front.foldl(seed, fn);
            return ((Stack<T>) back.reverse()).foldl(frontRes, fn);
        }

        @Override
        public String toString() {
            return foldl("[", (r, t) -> r + (r.equals("[") ? "" : ",") + t) + "]";
        }

        @Override
        public boolean equals(Object other) {
            if (other == null) return false;
            if (other == this) return true;
            if (!(other instanceof Deque)) return false;
            Deque<?> o = (Deque<?>) other;
            if (this.length() != o.length()) return false;
            return this.toString().equals(o.toString());
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }
    }
}
