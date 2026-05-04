package io.github.senthilganeshs.fj.ds;

import java.util.function.BiFunction;

/**
 * A purely functional Stack (LIFO).
 */
public interface Stack<T> extends Collection<T> {

    @SuppressWarnings("unchecked")
    static <R> Stack<R> from(Collection<R> c) {
        if (c instanceof Stack) return (Stack<R>) c;
        return (Stack<R>) c.foldl(Stack.<R>emptyStack(), (s, r) -> (Stack<R>) s.build(r));
    }

    @SuppressWarnings("unchecked")
    static <R> Stack<R> emptyStack() {
        return new StackImpl<>(List.nil());
    }

    @SuppressWarnings("unchecked")
    static <R> Stack<R> newStack(R... values) {
        return of(values);
    }

    @SafeVarargs
    static <R> Stack<R> of(R... values) {
        Stack<R> s = emptyStack();
        if (values == null) return s;
        for (R val : values) {
            s = (Stack<R>) s.build(val);
        }
        return s;
    }

    Maybe<T> head();
    Maybe<Stack<T>> tail();

    @Override
    default <R> Collection<R> empty() {
        return emptyStack();
    }

    @Override
    default Collection<T> build(T input) {
        return push(input);
    }

    @SuppressWarnings("unchecked")
    default Stack<T> push(T value) {
        return (Stack<T>) new StackImpl<>(List.cons(List.from(this), value));
    }

    @SuppressWarnings("unchecked")
    default Maybe<Tuple<T, Stack<T>>> pop() {
        if (isEmpty()) return Maybe.nothing();
        List<T> l = List.from(this);
        return l.lastMaybe().map(t -> Tuple.of(t, (Stack<T>) new StackImpl<>(List.from(l.take(l.length() - 1)))));
    }

    @SuppressWarnings("unchecked")
    default Stack<T> reverse() {
        return (Stack<T>) new StackImpl<>(List.from(Collection.super.reverse()));
    }

    @SuppressWarnings("unchecked")
    default <R> Stack<R> map(java.util.function.Function<T, R> fn) {
        return (Stack<R>) Collection.super.map(fn);
    }

    @SuppressWarnings("unchecked")
    default <R> Stack<R> flatMap(java.util.function.Function<T, Collection<R>> fn) {
        return (Stack<R>) Collection.super.flatMap(fn);
    }

    @SuppressWarnings("unchecked")
    default Stack<T> concat(Collection<T> other) {
        return (Stack<T>) Collection.super.concat(other);
    }

    @SuppressWarnings("unchecked")
    default Stack<T> take(int n) {
        return (Stack<T>) Collection.super.take(n);
    }

    @SuppressWarnings("unchecked")
    default Stack<T> drop(int n) {
        return (Stack<T>) Collection.super.drop(n);
    }

    @SuppressWarnings("unchecked")
    default Stack<T> filter(java.util.function.Predicate<T> pred) {
        return (Stack<T>) Collection.super.filter(pred);
    }

    @SuppressWarnings("unchecked")
    default <R> Stack<R> mapMaybe(java.util.function.Function<T, Maybe<R>> fn) {
        return (Stack<R>) Collection.super.mapMaybe(fn);
    }

    record StackImpl<T>(List<T> internal) implements Stack<T> {
        @Override public <R> R foldl(R seed, BiFunction<R, T, R> fn) { return internal.foldl(seed, fn); }
        @Override public String toString() { return "Stack(" + internal + ")"; }
        @Override public boolean equals(Object other) {
            return other instanceof Stack && internal.equals(List.from((Stack<?>) other));
        }
        @Override public int hashCode() { return internal.hashCode(); }

        @SuppressWarnings("unchecked")
        @Override public Maybe<T> head() { return internal.lastMaybe(); }
        
        @SuppressWarnings("unchecked")
        @Override public Maybe<Stack<T>> tail() {
            if (internal.isEmpty()) return Maybe.nothing();
            return Maybe.some((Stack<T>) new StackImpl<>(List.from(internal.take(internal.length() - 1))));
        }

        @Override public <R> Collection<R> empty() { return emptyStack(); }
    }
}
