package io.github.senthilganeshs.fj.ds;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public interface LazyList<T> extends Collection<T> {

    Maybe<T> head();
    LazyList<T> tail();

    @Override
    default <R> LazyList<R> map(java.util.function.Function<T, R> fn) {
        return ((Maybe<LazyList<R>>) head().map(h -> cons(fn.apply(h), () -> tail().map(fn)))).fromMaybe((LazyList<R>) nil());
    }

    @Override
    default LazyList<T> filter(java.util.function.Predicate<T> pred) {
        return ((Maybe<LazyList<T>>) head().map(h -> {
            if (pred.test(h)) {
                return cons(h, () -> tail().filter(pred));
            } else {
                return tail().filter(pred);
            }
        })).fromMaybe(this);
    }

    @Override
    default LazyList<T> take(int n) {
        if (n <= 0) return nil();
        return ((Maybe<LazyList<T>>) head().map(h -> cons(h, () -> tail().take(n - 1)))).fromMaybe(this);
    }

    @Override
    default LazyList<T> drop(int n) {
        if (n <= 0) return this;
        return ((Maybe<LazyList<T>>) head().map(__ -> tail().drop(n - 1))).fromMaybe(this);
    }

    @Override
    default Collection<T> concat(Collection<T> other) {
        return ((Maybe<LazyList<T>>) head().map(h -> cons(h, () -> (LazyList<T>) tail().concat(other)))).fromMaybe((LazyList<T>) other);
    }

    static <R> LazyList<R> nil() {
        return new Empty<>();
    }

    static <R> LazyList<R> cons(R head, Supplier<LazyList<R>> tail) {
        return new NonEmpty<>(head, tail);
    }

    @SafeVarargs
    static <R> LazyList<R> of(R... values) {
        LazyList<R> l = nil();
        for (int i = values.length - 1; i >= 0; i--) {
            R val = values[i];
            LazyList<R> finalL = l;
            l = cons(val, () -> finalL);
        }
        return l;
    }

    static <R> LazyList<R> iterate(R seed, java.util.function.UnaryOperator<R> fn) {
        return cons(seed, () -> iterate(fn.apply(seed), fn));
    }

    final class NonEmpty<T> implements LazyList<T> {
        private final T head;
        private final Supplier<LazyList<T>> tailSupplier;
        private LazyList<T> evaluatedTail;

        NonEmpty(T head, Supplier<LazyList<T>> tail) {
            this.head = head;
            this.tailSupplier = tail;
        }

        @Override
        public Maybe<T> head() {
            return Maybe.some(head);
        }

        @Override
        public LazyList<T> tail() {
            if (evaluatedTail == null) {
                evaluatedTail = tailSupplier.get();
            }
            return evaluatedTail;
        }

        @Override
        public <R> Collection<R> empty() {
            return nil();
        }

        @Override
        public Collection<T> build(T input) {
            // build for LazyList is like snoc, which is expensive for a ConsList
            // but we implement it for Collection compatibility
            return concat(LazyList.of(input));
        }

        @Override
        public <R> R foldl(R seed, BiFunction<R, T, R> fn) {
            return tail().foldl(fn.apply(seed, head), fn);
        }

        @Override
        public int length() {
            return tail().length() + 1;
        }

        @Override
        public String toString() {
            // We can't safely call length() here if it might be infinite.
            // Let's just limit toString to 10 elements and only add ... if there's more.
            StringBuilder sb = new StringBuilder("[");
            LazyList<T> current = this;
            int count = 0;
            while (current.head().isSome() && count < 10) {
                if (count > 0) sb.append(",");
                sb.append(current.head().fromMaybe(null));
                current = current.tail();
                count++;
            }
            if (current.head().isSome()) {
                sb.append(",...");
            }
            sb.append("]");
            return sb.toString();
        }
    }

    final class Empty<T> implements LazyList<T> {
        @Override
        public Maybe<T> head() {
            return Maybe.nothing();
        }

        @Override
        public LazyList<T> tail() {
            return this;
        }

        @Override
        public <R> Collection<R> empty() {
            return nil();
        }

        @Override
        public Collection<T> build(T input) {
            return cons(input, () -> this);
        }

        @Override
        public <R> R foldl(R seed, BiFunction<R, T, R> fn) {
            return seed;
        }

        @Override
        public String toString() {
            return "[]";
        }
    }
}
