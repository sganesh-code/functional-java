package io.github.senthilganeshs.fj.ds;

import java.util.Arrays;
import java.util.function.BiFunction;

public interface Stack<T> extends Collection<T>{

    Maybe<T> head();
    Maybe<Stack<T>> tail();

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
            return tail.foldl(fn.apply(seed, head), fn);
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
    }
}
