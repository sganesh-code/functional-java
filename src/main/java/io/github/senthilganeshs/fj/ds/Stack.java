package io.github.senthilganeshs.fj.ds;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A purely functional Stack (LIFO).
 * 
 * @param <T> The type of elements in the stack.
 */
public interface Stack<T> extends Collection<T>{

    Maybe<T> head();
    Maybe<Stack<T>> tail();

    @SuppressWarnings("unchecked")
    static <R> Stack<R> from(Collection<R> c) {
        return (Stack<R>) c.foldl(Stack.<R>emptyStack(), (s, r) -> (Stack<R>) s.build(r));
    }

    @SuppressWarnings("unchecked")
    @Override
    default <R> Stack<R> map(Function<T, R> fn) {
        return from(Collection.super.map(fn));
    }

    @SuppressWarnings("unchecked")
    @Override
    default Stack<T> filter(Predicate<T> pred) {
        return from(Collection.super.filter(pred));
    }

    @SuppressWarnings("unchecked")
    @Override
    default Stack<T> take(int n) {
        if (n <= 0) return emptyStack();
        return ((Maybe<Stack<T>>) (Maybe<?>) head().flatMap(h -> tail().map(t -> (Stack<T>) new NonEmpty<>(h, t.take(n - 1))))).orElse(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    default Stack<T> drop(int n) {
        if (n <= 0) return this;
        return ((Maybe<Stack<T>>) (Maybe<?>) tail().flatMap(t -> Maybe.some(t.drop(n - 1)))).orElse(emptyStack());
    }

    @Override
    default Stack<T> reverse() {
        return foldl(emptyStack(), (r, t) -> (Stack<T>) r.build(t));
    }

    static <R> Stack<R> emptyStack() {
        return new Empty<>();
    }

    @SafeVarargs
    static <R> Stack<R> newStack(R... values) {
        Stack<R> s = emptyStack();
        if (values == null) return s;
        for (R val : values) s = (Stack<R>) s.build(val);
        return s;
    }

    final static class NonEmpty<T> implements Stack<T> {

        private final T head;
        private final Stack<T> tail;

        NonEmpty(final T head, final Stack<T> tail) {
            this.head = head;
            this.tail = tail;
        }

        @Override
        public Maybe<T> head() {
            return Maybe.some(head);
        }

        @Override
        public Maybe<Stack<T>> tail() {
            return Maybe.some(tail);
        }

        @Override
        public <R> Collection<R> empty() {
            return emptyStack();
        }

        @Override
        public Collection<T> build(T input) {
            return new NonEmpty<T>(input, this);
        }

        @Override
        public <R> R foldl(R seed, BiFunction<R, T, R> fn) {
            R acc = seed;
            Stack<T> curr = this;
            while (curr instanceof NonEmpty) {
                NonEmpty<T> ne = (NonEmpty<T>) curr;
                acc = fn.apply(acc, ne.head);
                curr = ne.tail;
            }
            return acc;
        }

        @Override
        public String toString() {
            return foldl("[", (r, t) -> r + (r.equals("[") ? "" : ",") + t) + "]";
        }

        @Override
        public boolean equals(Object other) {
            if (other == null) return false;
            if (other == this) return true;
            if (!(other instanceof Stack)) return false;
            Stack<?> o = (Stack<?>) other;
            if (o.length() != length()) return false;
            return toString().equals(o.toString());
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }
    }


    final static class Empty<T> implements Stack<T> {

        @Override
        public <R> Collection<R> empty() {
            return emptyStack();
        }

        @Override
        public Collection<T> build(T input) {
            return new NonEmpty<T>(input, this);
        }

        @Override
        public <R> R foldl(R seed, BiFunction<R, T, R> fn) {
            return seed;
        }

        @Override
        public String toString() {
            return "[]";
        }

        @Override
        public Maybe<T> head() {
            return Maybe.nothing();
        }

        @Override
        public Maybe<Stack<T>> tail() {
            return Maybe.nothing();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Empty;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }
}
