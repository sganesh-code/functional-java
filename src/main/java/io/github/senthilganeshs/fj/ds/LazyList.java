package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.hkt.Higher;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A lazy, potentially infinite persistent list.
 */
public interface LazyList<T> extends List<T> {

    @SuppressWarnings("unchecked")
    static <R> LazyList<R> nil() {
        return (LazyList<R>) Empty.INSTANCE;
    }

    static <R> LazyList<R> cons(R head, Supplier<LazyList<R>> tail) {
        return new NonEmpty<>(head, tail);
    }

    static <R> LazyList<R> iterate(R seed, java.util.function.UnaryOperator<R> f) {
        return cons(seed, () -> iterate(f.apply(seed), f));
    }

    @SuppressWarnings("unchecked")
    static <R> LazyList<R> from(Collection<R> c) {
        if (c instanceof LazyList) return (LazyList<R>) c;
        return (LazyList<R>) c.foldl(LazyList.<R>nil(), (acc, r) -> (LazyList<R>) acc.build(r));
    }

    @SafeVarargs
    static <R> LazyList<R> of(R... values) {
        if (values == null || values.length == 0) return nil();
        LazyList<R> res = nil();
        for (int i = values.length - 1; i >= 0; i--) {
            final int idx = i;
            final LazyList<R> current = res;
            res = cons(values[idx], () -> current);
        }
        return res;
    }

    @Override
    default boolean isEmpty() {
        return this instanceof Empty;
    }

    @Override
    default <R> R foldl(R seed, BiFunction<R, T, R> fn) {
        if (isEmpty()) return seed;
        NonEmpty<T> ne = (NonEmpty<T>) this;
        return ne.tailLazy().foldl(fn.apply(seed, ne.headVal()), fn);
    }

    @Override
    default <R> Collection<R> empty() {
        return nil();
    }

    default Collection<T> build(T input) {
        return List.from(this).build(input);
    }

    @Override
    default Maybe<T> headMaybe() {
        return isEmpty() ? Maybe.nothing() : Maybe.some(((NonEmpty<T>) this).headVal());
    }

    default Maybe<List<T>> tail() {
        return isEmpty() ? Maybe.nothing() : Maybe.some((List<T>) ((NonEmpty<T>) this).tailLazy());
    }

    default <R> LazyList<R> map(Function<T, R> fn) {
        if (isEmpty()) return nil();
        NonEmpty<T> ne = (NonEmpty<T>) this;
        return cons(fn.apply(ne.headVal()), () -> ne.tailLazy().map(fn));
    }

    default <R> LazyList<R> flatMap(Function<T, Collection<R>> fn) {
        if (isEmpty()) return nil();
        NonEmpty<T> ne = (NonEmpty<T>) this;
        LazyList<R> headList = (LazyList<R>) from(fn.apply(ne.headVal()));
        return headList.concat(ne.tailLazy().flatMap(fn));
    }

    default LazyList<T> concat(Collection<T> other) {
        if (isEmpty()) return from(other);
        NonEmpty<T> ne = (NonEmpty<T>) this;
        return cons(ne.headVal(), () -> ne.tailLazy().concat(other));
    }

    final class NonEmpty<T> implements LazyList<T> {
        private final T head;
        private final Supplier<LazyList<T>> tail;

        NonEmpty(T head, Supplier<LazyList<T>> tail) {
            this.head = head;
            this.tail = tail;
        }

        public T headVal() { return head; }
        public LazyList<T> tailLazy() { return tail.get(); }
        
        @Override
        public String toString() { return "LazyList(" + head + ", ...)"; }
    }

    final class Empty<T> implements LazyList<T> {
        static final Empty<?> INSTANCE = new Empty<>();
        @Override
        public String toString() { return "EmptyLazyList"; }
    }
}
