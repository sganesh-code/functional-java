package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.typeclass.Monad;
import io.github.senthilganeshs.fj.typeclass.Monoid;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A purely functional Linked List (Snoc-list style).
 */
public interface List<T> extends Collection<T> {

    @SuppressWarnings("unchecked")
    static <R> List<R> from(Iterable<R> i) {
        List<R> res = nil();
        for (R r : i) res = (List<R>) res.build(r);
        return res;
    }

    @SuppressWarnings("unchecked")
    static <R> List<R> from(Collection<R> c) {
        if (c instanceof List) return (List<R>) c;
        return (List<R>) c.foldl(List.<R>nil(), (acc, r) -> (List<R>) acc.build(r));
    }

    @SafeVarargs
    static <R> List<R> of(R... values) {
        List<R> list = nil();
        if (values == null) return list;
        for (R value : values) {
            list = (List<R>) list.build(value);
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    static <R> List<R> nil() {
        return (List<R>) EMPTY;
    }

    static <T> Monoid<List<T>> monoid() {
        return Monoid.of(nil(), (a, b) -> from(a.concat(b)));
    }

    static final Monad<Collection.µ> monad = new Monad<Collection.µ>() {
        @Override public <A> Higher<Collection.µ, A> pure(A a) { return of(a); }
        @Override public <A, B> Higher<Collection.µ, B> flatMap(Function<A, Higher<Collection.µ, B>> fn, Higher<Collection.µ, A> fa) {
            Collection<A> src = Collection.narrowK(fa);
            return src.foldl(src.empty(), (acc, a) -> Collection.narrowK(fn.apply(a)).foldl(acc, Collection::build));
        }
    };

    static <R> Collection<R> newQueue(R... values) {
        return Queue.of(values);
    }

    static List<Integer> range(int start, int end) {
        List<Integer> res = nil();
        for (int i = start; i < end; i++) {
            res = (List<Integer>) res.build(i);
        }
        return res;
    }

    static <R> List<R> cons(final List<R> head, final R tail) {
        return new LinkedList<>(head, tail);
    }

    @Override
    default <R> Collection<R> empty() {
        return nil();
    }

    @Override
    default Collection<T> build(T input) {
        return cons(this, input);
    }

    @SuppressWarnings("unchecked")
    default <R> List<R> map(Function<T, R> fn) {
        return (List<R>) Collection.super.map(fn);
    }

    @SuppressWarnings("unchecked")
    default <R> List<R> flatMap(Function<T, Collection<R>> fn) {
        return (List<R>) Collection.super.flatMap(fn);
    }

    @SuppressWarnings("unchecked")
    default List<T> concat(Collection<T> other) {
        return (List<T>) Collection.super.concat(other);
    }

    @SuppressWarnings("unchecked")
    default <R> List<Tuple<T, R>> zip(List<R> other) {
        return (List<Tuple<T, R>>) zipWith(Tuple::of, other);
    }

    @SuppressWarnings("unchecked")
    default List<T> filter(Predicate<T> pred) {
        return (List<T>) Collection.super.filter(pred);
    }

    @SuppressWarnings("unchecked")
    default <R, S> List<S> zipWith(BiFunction<T, R, S> fn, Collection<R> other) {
        return (List<S>) Collection.super.zipWith(fn, other);
    }

    @SuppressWarnings("unchecked")
    default List<T> take(int n) {
        return (List<T>) Collection.super.take(n);
    }

    @SuppressWarnings("unchecked")
    default List<T> drop(int n) {
        return (List<T>) Collection.super.drop(n);
    }

    @SuppressWarnings("unchecked")
    default List<T> slice(int start, int n) {
        return (List<T>) Collection.super.slice(start, n);
    }

    @SuppressWarnings("unchecked")
    default List<T> reverse() {
        return (List<T>) Collection.super.reverse();
    }

    @SuppressWarnings("unchecked")
    default <R> List<R> mapMaybe(Function<T, Maybe<R>> fn) {
        return (List<R>) Collection.super.mapMaybe(fn);
    }

    boolean isEmpty();

    final static List<Void> EMPTY = new EmptyList<>();

    final static class EmptyList<T> implements List<T> {
        @Override public boolean isEmpty() { return true; }
        @Override public <R> R foldl(R seed, BiFunction<R, T, R> fn) { return seed; }
        @Override public String toString() { return "[]"; }
        @Override public boolean equals(Object other) { return other instanceof EmptyList; }
        @Override public int hashCode() { return 0; }
        @Override public <R> Collection<R> empty() { return nil(); }
        @Override public Collection<T> build(T input) { return cons(this, input); }
    }

    final static class LinkedList<T> implements List<T> {
        private final List<T> head;
        private final T tail;

        LinkedList(List<T> head, T tail) {
            this.head = head;
            this.tail = tail;
        }

        @Override public boolean isEmpty() { return false; }
        @Override public <R> R foldl(R seed, BiFunction<R, T, R> fn) {
            return fn.apply(head.foldl(seed, fn), tail);
        }

        @Override public String toString() {
            return foldl("[", (acc, t) -> acc + (acc.equals("[") ? "" : ",") + t) + "]";
        }

        @Override public boolean equals(Object other) {
            if (other instanceof List) {
                List<?> l = (List<?>) other;
                if (l.length() != length()) return false;
                return this.toString().equals(l.toString());
            }
            return false;
        }

        @Override public int hashCode() { return toString().hashCode(); }
        @Override public <R> Collection<R> empty() { return nil(); }
        @Override public Collection<T> build(T input) { return cons(this, input); }
    }
}
