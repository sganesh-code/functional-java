package io.github.senthilganeshs.fj.ds;

import java.util.function.BiFunction;

/**
 * A purely functional Double-Ended Queue (Deque).
 */
public interface Deque<T> extends Collection<T> {

    @SuppressWarnings("unchecked")
    static <R> Deque<R> nil() {
        return (Deque<R>) BankersDeque.EMPTY;
    }

    @SafeVarargs
    static <R> Deque<R> of(R... values) {
        Deque<R> d = nil();
        if (values == null) return d;
        for (R val : values) {
            d = d.pushBack(val);
        }
        return d;
    }

    @SuppressWarnings("unchecked")
    static <R> Deque<R> from(Collection<R> c) {
        if (c instanceof Deque) return (Deque<R>) c;
        return (Deque<R>) c.foldl(Deque.<R>nil(), (d, r) -> d.pushBack(r));
    }

    Deque<T> pushFront(T value);
    Deque<T> pushBack(T value);
    Maybe<Tuple<T, Deque<T>>> popFront();
    Maybe<Tuple<T, Deque<T>>> popBack();

    @Override
    default Collection<T> build(T input) {
        return pushBack(input);
    }

    final class BankersDeque<T> implements Deque<T> {
        private final Stack<T> f;
        private final Stack<T> b;

        static final BankersDeque<?> EMPTY = new BankersDeque<>(Stack.emptyStack(), Stack.emptyStack());

        BankersDeque(Stack<T> f, Stack<T> b) {
            this.f = f;
            this.b = b;
        }

        private static <T> Deque<T> check(Stack<T> f, Stack<T> b) {
            if (f.isEmpty() && !b.isEmpty()) {
                if (b.length() == 1) return new BankersDeque<>(b, Stack.emptyStack());
                int mid = b.length() / 2;
                Stack<T> newF = Stack.from(b.take(mid).reverse());
                Stack<T> newB = Stack.from(b.drop(mid));
                return new BankersDeque<>(newF, newB);
            }
            if (b.isEmpty() && !f.isEmpty()) {
                if (f.length() == 1) return new BankersDeque<>(Stack.emptyStack(), f);
                int mid = f.length() / 2;
                Stack<T> newB = Stack.from(f.take(mid).reverse());
                Stack<T> newF = Stack.from(f.drop(mid));
                return new BankersDeque<>(newF, newB);
            }
            return new BankersDeque<>(f, b);
        }

        @Override public Deque<T> pushFront(T value) { return check((Stack<T>) f.build(value), b); }
        @Override public Deque<T> pushBack(T value) { return check(f, (Stack<T>) b.build(value)); }

        @Override
        public Maybe<Tuple<T, Deque<T>>> popFront() {
            if (isEmpty()) return Maybe.nothing();
            return Maybe.some(Tuple.of(f.head().orElse(null), check(f.tail().orElse(Stack.emptyStack()), b)));
        }

        @Override
        public Maybe<Tuple<T, Deque<T>>> popBack() {
            if (isEmpty()) return Maybe.nothing();
            // If f was moved to b in check, we might have b not empty but f empty.
            if (b.isEmpty()) return popFront().map(t -> Tuple.of(t.getA().orElse(null), nil())); // Should not happen with check
            return Maybe.some(Tuple.of(b.head().orElse(null), check(f, b.tail().orElse(Stack.emptyStack()))));
        }

        @Override
        public <R> R foldl(R seed, BiFunction<R, T, R> fn) {
            // f contains front elements (LIFO). b contains back elements (FIFO).
            // LIFO foldl on f visits elements from front-most to middle.
            // FIFO foldl on b visits elements from middle to back-most.
            R res = f.foldl(seed, fn);
            return b.internal().foldl(res, fn);
        }

        @Override public <R> Collection<R> empty() { return nil(); }

        @Override
        public String toString() {
            return Collection.toString(this);
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
