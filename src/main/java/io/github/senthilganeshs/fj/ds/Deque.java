package io.github.senthilganeshs.fj.ds;

import java.util.function.BiFunction;

/**
 * A purely functional Double-Ended Queue (Deque).
 */
public interface Deque<T> extends Collection<T> {

    @SuppressWarnings("unchecked")
    static <R> Deque<R> from(Collection<R> c) {
        if (c instanceof Deque) return (Deque<R>) c;
        return (Deque<R>) c.foldl(Deque.<R>nil(), (d, r) -> (Deque<R>) d.build(r));
    }

    @SuppressWarnings("unchecked")
    static <R> Deque<R> nil() {
        return (Deque<R>) BankersDeque.nil();
    }

    @SafeVarargs
    static <R> Deque<R> of(R... values) {
        Deque<R> d = nil();
        if (values == null) return d;
        for (R val : values) {
            d = (Deque<R>) d.build(val);
        }
        return d;
    }

    Deque<T> pushFront(T value);
    Deque<T> pushBack(T value);
    Maybe<Tuple<T, Deque<T>>> popFront();
    Maybe<Tuple<T, Deque<T>>> popBack();

    @Override
    default <R> Collection<R> empty() {
        return nil();
    }

    @Override
    default Collection<T> build(T input) {
        return pushBack(input);
    }

    final class BankersDeque<T> implements Deque<T> {
        private final Stack<T> front;
        private final Stack<T> back;

        BankersDeque(Stack<T> front, Stack<T> back) {
            this.front = front;
            this.back = back;
        }

        static <T> Deque<T> nil() {
            return new BankersDeque<>(Stack.emptyStack(), Stack.emptyStack());
        }

        private Deque<T> check(Stack<T> f, Stack<T> b) {
            if (f.isEmpty() && !b.isEmpty()) {
                int mid = b.length() / 2;
                Stack<T> newF = (Stack<T>) b.drop(mid).reverse();
                Stack<T> newB = (Stack<T>) b.take(mid);
                return new BankersDeque<>(newF, newB);
            }
            if (b.isEmpty() && !f.isEmpty()) {
                int mid = f.length() / 2;
                Stack<T> newB = (Stack<T>) f.drop(mid).reverse();
                Stack<T> newF = (Stack<T>) f.take(mid);
                return new BankersDeque<>(newF, newB);
            }
            return new BankersDeque<>(f, b);
        }

        @Override
        public Deque<T> pushFront(T value) {
            return check(front.push(value), back);
        }

        @Override
        public Deque<T> pushBack(T value) {
            return check(front, back.push(value));
        }

        @Override
        public Maybe<Tuple<T, Deque<T>>> popFront() {
            return front.pop().map(t -> Tuple.of(t.getA().orElse(null), check(t.getB().orElse(null), back)));
        }

        @Override
        public Maybe<Tuple<T, Deque<T>>> popBack() {
            return back.pop().map(t -> Tuple.of(t.getA().orElse(null), check(front, t.getB().orElse(null))));
        }

        @Override
        public <R> R foldl(R seed, BiFunction<R, T, R> fn) {
            R res = front.foldl(seed, fn);
            return back.reverse().foldl(res, fn);
        }

        @Override public <R> Collection<R> empty() { return nil(); }

        @Override
        public String toString() {
            return "Deque(" + front + ", " + back + ")";
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Deque) {
                Deque<?> d = (Deque<?>) other;
                if (d.length() != length()) return false;
                return this.toString().equals(d.toString());
            }
            return false;
        }

        @Override public int hashCode() { return toString().hashCode(); }
    }
}
