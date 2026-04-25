package io.github.senthilganeshs.fj.ds;

import java.util.Arrays;
import java.util.function.BiFunction;

public interface Stack<T> extends Collection<T>{

    Maybe<T> head();
    Maybe<Stack<T>> tail();

    @SuppressWarnings("unchecked")
    static <R> Stack<R> from(Collection<R> c) {
        return (Stack<R>) c.foldl(Stack.<R>emptyStack(), (s, r) -> (Stack<R>) s.build(r));
    }

    @Override
    default <R> Stack<R> map(java.util.function.Function<T, R> fn) {
        return from(Collection.super.map(fn));
    }

    @Override
    default Stack<T> filter(java.util.function.Predicate<T> pred) {
        return from(Collection.super.filter(pred));
    }

    @Override
    default Stack<T> take(int n) {
        if (n <= 0) return emptyStack();
        return (Stack<T>) ((Maybe<Stack<T>>) head().flatMap(h -> tail().map(t -> (Stack<T>) new NonEmpty<>(h, t.take(n - 1))))).orElse(this);
    }

    @Override
    default Stack<T> drop(int n) {
        if (n <= 0) return this;
        return (Stack<T>) ((Maybe<Stack<T>>) tail().flatMap(t -> Maybe.some(t.drop(n - 1)))).orElse(emptyStack());
    }

    @Override
    default Stack<T> reverse() {
        return foldl(emptyStack(), (r, t) -> (Stack<T>) r.build(t));
    }

    static <R> Stack<R> emptyStack() {
        return new Empty<>();
    }

    static <R> Stack<R> newStack(R[] values) {
        return Arrays.stream(values).reduce(emptyStack(), (stack, r) -> (Stack<R>) stack.build(r), (a, b) -> b);
    }

    final static class NonEmpty<T> implements Stack<T> {

        private Stack<T> tail;
        private T head;

        NonEmpty(T head, Stack<T> tail) {
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
            return new Empty<>();
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
    }


    final static class Empty<T> implements Stack<T> {

        @Override
        public Maybe<T> head() {
            return Maybe.nothing();
        }

        @Override
        public Maybe<Stack<T>> tail() {
            return Maybe.nothing();
        }

        @Override
        public <R> Collection<R> empty() {
            return new Empty<>();
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
    }
}
