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
        R res = seed;
        LazyList<T> curr = this;
        while (!curr.isEmpty()) {
            NonEmpty<T> ne = (NonEmpty<T>) curr;
            res = fn.apply(res, ne.headVal());
            curr = ne.tailLazy();
        }
        return res;
    }

    @Override
    default <R> Collection<R> empty() {
        return nil();
    }

    @Override
    default Collection<T> build(T input) {
        return concat(of(input));
    }

    @Override
    default Maybe<T> head() {
        return headMaybe();
    }

    @Override
    default Maybe<T> headMaybe() {
        return isEmpty() ? Maybe.nothing() : Maybe.some(((NonEmpty<T>) this).headVal());
    }

    @Override
    default Maybe<List<T>> tail() {
        return isEmpty() ? Maybe.nothing() : Maybe.some((List<T>) ((NonEmpty<T>) this).tailLazy());
    }

    @Override
    default <R> LazyList<R> map(Function<T, R> fn) {
        if (isEmpty()) return nil();
        NonEmpty<T> ne = (NonEmpty<T>) this;
        return cons(fn.apply(ne.headVal()), () -> ne.tailLazy().map(fn));
    }

    @Override
    default <R> LazyList<R> flatMap(Function<T, Collection<R>> fn) {
        if (isEmpty()) return nil();
        NonEmpty<T> ne = (NonEmpty<T>) this;
        LazyList<R> headList = (LazyList<R>) from(fn.apply(ne.headVal()));
        return headList.concat(ne.tailLazy().flatMap(fn));
    }

    @Override
    default LazyList<T> filter(Predicate<T> pred) {
        LazyList<T> curr = this;
        while (!curr.isEmpty()) {
            NonEmpty<T> ne = (NonEmpty<T>) curr;
            if (pred.test(ne.headVal())) {
                final LazyList<T> nextTail = ne.tailLazy();
                return cons(ne.headVal(), () -> nextTail.filter(pred));
            }
            curr = ne.tailLazy();
        }
        return nil();
    }

    @Override
    default LazyList<T> concat(Collection<T> other) {
        if (isEmpty()) return from(other);
        NonEmpty<T> ne = (NonEmpty<T>) this;
        return cons(ne.headVal(), () -> ne.tailLazy().concat(other));
    }

    @Override
    default LazyList<T> take(int n) {
        if (n <= 0 || isEmpty()) return nil();
        NonEmpty<T> ne = (NonEmpty<T>) this;
        return cons(ne.headVal(), () -> ne.tailLazy().take(n - 1));
    }

    @Override
    default LazyList<T> drop(int n) {
        LazyList<T> curr = this;
        while (n > 0 && !curr.isEmpty()) {
            curr = ((NonEmpty<T>) curr).tailLazy();
            n--;
        }
        return curr;
    }

    @Override
    default LazyList<T> reverse() {
        return from(List.super.reverse());
    }

    @Override
    default <R> LazyList<R> mapMaybe(Function<T, Maybe<R>> fn) {
        if (isEmpty()) return nil();
        NonEmpty<T> ne = (NonEmpty<T>) this;
        Maybe<R> res = fn.apply(ne.headVal());
        if (res.isSome()) return cons(res.orElse(null), () -> ne.tailLazy().mapMaybe(fn));
        return ne.tailLazy().mapMaybe(fn);
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
        public String toString() {
            StringBuilder sb = new StringBuilder("[");
            LazyList<T> curr = this;
            int count = 0;
            while (!curr.isEmpty() && count < 10) {
                if (count > 0) sb.append(",");
                sb.append(((NonEmpty<T>) curr).headVal());
                curr = ((NonEmpty<T>) curr).tailLazy();
                count++;
            }
            if (!curr.isEmpty()) sb.append(",...");
            return sb.append("]").toString();
        }

        @Override public int length() {
            int len = 0;
            LazyList<T> curr = this;
            while (!curr.isEmpty()) {
                len++;
                curr = ((NonEmpty<T>) curr).tailLazy();
            }
            return len;
        }
    }

    final class Empty<T> implements LazyList<T> {
        static final Empty<?> INSTANCE = new Empty<>();
        @Override public String toString() { return "[]"; }
        @Override public int length() { return 0; }
    }
}
