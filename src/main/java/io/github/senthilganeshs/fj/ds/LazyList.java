package io.github.senthilganeshs.fj.ds;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * A purely functional Lazy List with deferred evaluation.
 * 
 * @param <T> The type of elements.
 */
public interface LazyList<T> extends List<T> {

    Maybe<T> head();
    LazyList<T> tail();

    @Override
    default Tuple<Maybe<T>, List<T>> unzip() {
        return Tuple.of(head(), tail());
    }

    @SuppressWarnings("unchecked")
    @Override
    default <R> List<Tuple<T, R>> zip(List<R> other) {
        T h = head().orElse(null);
        if (h == null) return LazyList.nil();
        
        Tuple<Maybe<R>, List<R>> otherUnzipped = other.unzip();
        R oh = ((Maybe<R>) (Maybe<?>) otherUnzipped.getA().orElse(Maybe.nothing())).orElse(null);
        if (oh == null) return LazyList.nil();
        
        return cons(Tuple.of(h, oh), () -> (LazyList<Tuple<T, R>>) tail().zip(otherUnzipped.getB().orElse(List.nil())));
    }

    @SuppressWarnings("unchecked")
    @Override
    default <R> LazyList<R> map(java.util.function.Function<T, R> fn) {
        return ((Maybe<LazyList<R>>) (Maybe<?>) head().map(h -> cons(fn.apply(h), () -> tail().map(fn)))).orElse(nil());
    }

    @SuppressWarnings("unchecked")
    @Override
    default LazyList<T> filter(java.util.function.Predicate<T> pred) {
        return ((Maybe<LazyList<T>>) (Maybe<?>) head().map(h -> {
            if (pred.test(h)) {
                return cons(h, () -> tail().filter(pred));
            } else {
                return tail().filter(pred);
            }
        })).orElse(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    default LazyList<T> take(int n) {
        if (n <= 0) return nil();
        return ((Maybe<LazyList<T>>) (Maybe<?>) head().map(h -> cons(h, () -> tail().take(n - 1)))).orElse(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    default LazyList<T> drop(int n) {
        if (n <= 0) return this;
        return ((Maybe<LazyList<T>>) (Maybe<?>) head().map(__ -> tail().drop(n - 1))).orElse(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    default Collection<T> concat(Collection<T> other) {
        return ((Maybe<LazyList<T>>) (Maybe<?>) head().map(h -> cons(h, () -> (LazyList<T>) tail().concat(other)))).orElse((LazyList<T>) other);
    }

    static <R> LazyList<R> nil() {
        return new Empty<>();
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
        return c.foldr(nil(), (r, acc) -> cons(r, () -> acc));
    }

    @SafeVarargs
    static <R> LazyList<R> of(R... values) {
        if (values == null || values.length == 0) return nil();
        LazyList<R> res = nil();
        for (int i = values.length - 1; i >= 0; i--) {
            final R val = values[i];
            final LazyList<R> tail = res;
            res = cons(val, () -> tail);
        }
        return res;
    }

    final class NonEmpty<T> implements LazyList<T> {
        private final T head;
        private final Supplier<LazyList<T>> tail;

        NonEmpty(T head, Supplier<LazyList<T>> tail) {
            this.head = head;
            this.tail = tail;
        }

        @Override
        public Maybe<T> head() {
            return Maybe.some(head);
        }

        @Override
        public LazyList<T> tail() {
            return tail.get();
        }

        @Override
        public <R> Collection<R> empty() {
            return nil();
        }

        @Override
        public <R> R foldl(R seed, BiFunction<R, T, R> fn) {
            R acc = seed;
            LazyList<T> curr = this;
            while (curr instanceof NonEmpty) {
                acc = fn.apply(acc, curr.head().orElse(null));
                curr = curr.tail();
            }
            return acc;
        }

        @Override
        public int length() {
            return tail().length() + 1;
        }

        @Override
        public List<T> build(T input) {
            return cons(head, () -> (LazyList<T>) tail().build(input));
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("[");
            String content = take(10).foldl("", (r, t) -> r + (r.equals("") ? "" : ",") + t);
            sb.append(content);
            
            if (((Maybe<Boolean>) (Maybe<?>) drop(10).head().map(h -> true)).orElse(false)) {
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
        public List<T> build(T input) {
            return cons(input, () -> this);
        }

        @Override
        public <R> R foldl(R seed, BiFunction<R, T, R> fn) {
            return seed;
        }

        @Override
        public int length() {
            return 0;
        }

        @Override
        public String toString() {
            return "[]";
        }
    }
}
