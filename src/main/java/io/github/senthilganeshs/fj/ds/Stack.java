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
    static <R> Stack<R> nil() {
        return emptyStack();
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
    List<T> internal();

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
        return (Stack<T>) new StackImpl<>(List.cons(internal(), value));
    }

    @SuppressWarnings("unchecked")
    default Maybe<Tuple<T, Stack<T>>> pop() {
        if (isEmpty()) return Maybe.nothing();
        Maybe<T> h = head();
        Maybe<Stack<T>> t = tail();
        return h.flatMapMaybe(hv -> t.map(tv -> Tuple.of(hv, tv)));
    }

    @SuppressWarnings("unchecked")
    default Stack<T> reverse() {
        return (Stack<T>) new StackImpl<>(List.from(Collection.super.reverse()));
    }

    @SuppressWarnings("unchecked")
    @Override
    default <R> Stack<R> map(java.util.function.Function<T, R> fn) {
        return new StackImpl<>(internal().map(fn));
    }

    @SuppressWarnings("unchecked")
    @Override
    default <R> Stack<R> flatMap(java.util.function.Function<T, Collection<R>> fn) {
        return new StackImpl<>(internal().flatMap(fn));
    }

    @SuppressWarnings("unchecked")
    @Override
    default Stack<T> filter(java.util.function.Predicate<T> pred) {
        return new StackImpl<>(internal().filter(pred));
    }

    @SuppressWarnings("unchecked")
    @Override
    default Stack<T> take(int n) {
        // Stack is LIFO, so take(n) takes from the TOP (end of Snoc-list)
        return new StackImpl<>(internal().reverse().take(n).reverse());
    }

    @SuppressWarnings("unchecked")
    @Override
    default Stack<T> drop(int n) {
        return new StackImpl<>(internal().reverse().drop(n).reverse());
    }

    @SuppressWarnings("unchecked")
    default Stack<T> concat(Collection<T> other) {
        return new StackImpl<>(internal().concat(other));
    }

    @SuppressWarnings("unchecked")
    default <R> Stack<R> mapMaybe(java.util.function.Function<T, Maybe<R>> fn) {
        return new StackImpl<>(internal().mapMaybe(fn));
    }

    record StackImpl<T>(List<T> internal) implements Stack<T> {
        @Override public <R> R foldl(R seed, BiFunction<R, T, R> fn) { 
            return internal.reverse().foldl(seed, fn); 
        }
        @Override public String toString() { return Collection.toString(this); }
        @Override public boolean equals(Object other) {
            if (other instanceof Stack) {
                return this.toString().equals(other.toString());
            }
            return false;
        }
        @Override public int hashCode() { return toString().hashCode(); }

        @SuppressWarnings("unchecked")
        @Override public Maybe<T> head() { return internal.lastMaybe(); }
        
        @SuppressWarnings("unchecked")
        @Override public Maybe<Stack<T>> tail() {
            if (internal.isEmpty()) return Maybe.nothing();
            return Maybe.some((Stack<T>) new StackImpl<>(List.from(internal.take(internal.length() - 1))));
        }

        @Override public List<T> internal() { return internal; }
        @Override public <R> Collection<R> empty() { return emptyStack(); }
    }
}
