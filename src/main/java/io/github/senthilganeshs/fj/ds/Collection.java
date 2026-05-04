package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.optic.AffineTraversal;
import io.github.senthilganeshs.fj.optic.Traversal;
import io.github.senthilganeshs.fj.typeclass.Applicative;
import io.github.senthilganeshs.fj.typeclass.Functor;
import io.github.senthilganeshs.fj.typeclass.Monad;
import io.github.senthilganeshs.fj.typeclass.Monoid;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The root interface for all purely functional polymorphic collections.
 */
public interface Collection<T> extends Higher<Collection.µ, T>, Iterable<T> {
    final class µ {}

    <R> Collection<R> empty();
    Collection<T> build(T input);
    <R> R foldl(R seed, BiFunction<R, T, R> fn);

    default int length() { return foldl(0, (acc, __) -> acc + 1); }
    default int count() { return length(); }

    static <T> String toString(Collection<T> c) {
        StringBuilder sb = new StringBuilder("[");
        c.forEachIndexed((val, i) -> {
            if (i > 0) sb.append(",");
            sb.append(val);
        });
        return sb.append("]").toString();
    }

    @SuppressWarnings("unchecked")
    default <R> R foldr(R seed, BiFunction<T, R, R> fn) {
        java.util.List<T> list = new java.util.ArrayList<>();
        forEach(list::add);
        java.util.Collections.reverse(list);
        R res = seed;
        for (T t : list) res = fn.apply(t, res);
        return res;
    }

    default boolean contains(final T value) { return any(t -> t.equals(value)); }

    default Functor<µ> functor() {
        return new Functor<µ>() {
            @Override public <A, B> Higher<µ, B> map(Function<A, B> fn, Higher<µ, A> fa) {
                Collection<A> src = narrowK(fa);
                return src.foldl(src.empty(), (acc, t) -> acc.build(fn.apply(t)));
            }
        };
    }

    default Applicative<µ> applicative() {
        return new Applicative<µ>() {
            @Override public <A> Higher<µ, A> pure(A a) { return (Higher<µ, A>) empty().build(a); }
            @Override public <A, B> Higher<µ, B> ap(Higher<µ, Function<A, B>> ff, Higher<µ, A> fa) {
                Collection<Function<A, B>> functions = narrowK(ff);
                Collection<A> values = narrowK(fa);
                return functions.foldl(empty(), (acc, f) -> values.foldl(acc, (acc2, a) -> acc2.build(f.apply(a))));
            }
        };
    }

    default Monad<µ> monad() {
        return new Monad<µ>() {
            @Override public <A> Higher<µ, A> pure(A a) { return applicative().pure(a); }
            @Override public <A, B> Higher<µ, B> flatMap(Function<A, Higher<µ, B>> fn, Higher<µ, A> fa) {
                Collection<A> src = narrowK(fa);
                return src.foldl(src.empty(), (acc, t) -> narrowK(fn.apply(t)).foldl(acc, Collection::build));
            }
        };
    }

    @SuppressWarnings("unchecked")
    default <R> Collection<R> map(Function<T, R> fn) {
        return (Collection<R>) functor().map(fn, (Higher<µ, T>) this);
    }

    @SuppressWarnings("unchecked")
    default <R> Collection<R> flatMap(Function<T, Collection<R>> fn) {
        return (Collection<R>) monad().flatMap(t -> (Higher<µ, R>) fn.apply(t), (Higher<µ, T>) this);
    }

    default Collection<T> filter(Predicate<T> pred) {
        return foldl(empty(), (acc, t) -> pred.test(t) ? acc.build(t) : acc);
    }

    default <R> Collection<R> filterType(Class<R> clazz) {
        return filter(clazz::isInstance).map(clazz::cast);
    }

    @SuppressWarnings("unchecked")
    default Collection<T> sort(java.util.Comparator<? super T> cmp) {
        if (isEmpty()) return this;
        Object[] arr = new Object[length()];
        final int[] i = {0};
        this.forEach(t -> arr[i[0]++] = t);
        java.util.Arrays.sort(arr, (a, b) -> cmp.compare((T) a, (T) b));
        Collection<T> res = empty();
        for (Object t : arr) {
            res = res.build((T) t);
        }
        return res;
    }

    @SuppressWarnings("unchecked")
    default <R> Collection<R> concatMap(Function<T, Collection<R>> fn) {
        return foldl(empty(), (acc, t) -> (Collection<R>) acc.concat(fn.apply(t)));
    }

    @SuppressWarnings("unchecked")
    default Maybe<T> find(Predicate<T> pred) {
        return (Maybe<T>) foldl(Maybe.<T>nothing(), (acc, t) -> acc.isSome() ? acc : (pred.test(t) ? Maybe.some(t) : Maybe.<T>nothing()));
    }

    @SuppressWarnings("unchecked")
    default Maybe<Integer> findIndex(Predicate<T> pred) {
        return (Maybe<Integer>) foldl(Tuple.<Integer, Maybe<Integer>>of(0, Maybe.nothing()), (acc, t) -> {
            int idx = acc.getA().orElse(0);
            Maybe<Integer> res = acc.getB().orElse(null);
            if (res != null && res.isSome()) return acc;
            return pred.test(t) ? Tuple.of(idx + 1, Maybe.some(idx)) : Tuple.of(idx + 1, Maybe.<Integer>nothing());
        }).getB().orElse(Maybe.nothing());
    }

    default Maybe<Integer> indexOf(T value) { return findIndex(t -> t.equals(value)); }

    @SuppressWarnings("unchecked")
    default Collection<T> concat(Collection<T> other) { return other.foldl(this, Collection::build); }

    @SuppressWarnings("unchecked")
    default <R, S> Collection<S> zipWith(BiFunction<T, R, S> fn, Collection<R> other) {
        java.util.Iterator<T> it1 = iterator();
        java.util.Iterator<R> it2 = other.iterator();
        Collection<S> result = empty();
        while (it1.hasNext() && it2.hasNext()) {
            result = result.build(fn.apply(it1.next(), it2.next()));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    default Collection<T> take(int n) {
        return (Collection<T>) foldl(Tuple.<Integer, Collection<T>>of(n, this.<T>empty()), (acc, t) -> {
            int remaining = acc.getA().orElse(0);
            Collection<T> res = (Collection<T>) acc.getB().orElse(null);
            return remaining > 0 ? Tuple.of(remaining - 1, res.build(t)) : acc;
        }).getB().orElse(null);
    }

    @SuppressWarnings("unchecked")
    default Collection<T> drop(int n) {
        return (Collection<T>) foldl(Tuple.<Integer, Collection<T>>of(n, this.<T>empty()), (acc, t) -> {
            int remaining = acc.getA().orElse(0);
            Collection<T> res = (Collection<T>) acc.getB().orElse(null);
            return remaining > 0 ? Tuple.of(remaining - 1, res) : Tuple.of(0, res.build(t));
        }).getB().orElse(null);
    }

    default Collection<T> slice(int start, int n) { return drop(start).take(n); }
    default Collection<T> reverse() {
        java.util.List<T> list = new java.util.ArrayList<>();
        forEach(list::add);
        java.util.Collections.reverse(list);
        Collection<T> res = empty();
        for (T t : list) res = res.build(t);
        return res;
    }

    @SuppressWarnings("unchecked")
    default <R> Collection<R> mapMaybe(Function<T, Maybe<R>> fn) {
        return foldl(empty(), (acc, t) -> {
            Maybe<R> res = fn.apply(t);
            return res.isSome() ? acc.build(res.orElse(null)) : acc;
        });
    }

    @SuppressWarnings("unchecked")
    default <A, B> Tuple<Collection<A>, Collection<B>> unzip() {
        Collection<A> emptyA = this.<A>empty();
        Collection<B> emptyB = this.<B>empty();
        return foldl(Tuple.of(emptyA, emptyB), (acc, t) -> {
            Tuple<A, B> pair = (Tuple<A, B>) t;
            Collection<A> as = (Collection<A>) acc.getA().orElse(emptyA);
            Collection<B> bs = (Collection<B>) acc.getB().orElse(emptyB);
            return Tuple.of(as.build(pair.getA().orElse(null)), bs.build(pair.getB().orElse(null)));
        });
    }

    default String mkString(String sep) {
        String res = foldl(null, (acc, t) -> {
            String s = (t == null) ? "null" : t.toString();
            return (acc == null) ? s : acc + sep + s;
        });
        return res == null ? "" : res;
    }

    default String mkString(String start, String sep, String end) {
        return start + mkString(sep) + end;
    }

    default <R> R foldMap(Function<T, R> fn, Monoid<R> monoid) {
        return foldl(monoid.empty(), (acc, t) -> monoid.combine(acc, fn.apply(t)));
    }

    default T fold(Monoid<T> monoid) {
        return foldl(monoid.empty(), monoid::combine);
    }


    @SuppressWarnings("unchecked")
    default Collection<Collection<T>> chunk(int size) {
        if (size <= 0) return empty();
        Tuple<Integer, Tuple<Collection<T>, Collection<Collection<T>>>> initial = Tuple.of(0, Tuple.of(this.<T>empty(), this.<Collection<T>>empty()));
        Tuple<Integer, Tuple<Collection<T>, Collection<Collection<T>>>> result = foldl(initial, (acc, t) -> {
            int count = acc.getA().orElse(0);
            Tuple<Collection<T>, Collection<Collection<T>>> inner = (Tuple<Collection<T>, Collection<Collection<T>>>) acc.getB().orElse(null);
            if (count + 1 == size) return Tuple.of(0, Tuple.of(this.<T>empty(), (Collection<Collection<T>>) inner.getB().orElse(null).build(inner.getA().orElse(null).build(t))));
            return Tuple.of(count + 1, Tuple.of(inner.getA().orElse(null).build(t), inner.getB().orElse(null)));
        });
        Collection<T> last = ((Tuple<Collection<T>, Collection<Collection<T>>>) result.getB().orElse(null)).getA().orElse(null);
        Collection<Collection<T>> res = ((Tuple<Collection<T>, Collection<Collection<T>>>) result.getB().orElse(null)).getB().orElse(null);
        return (last == null || last.isEmpty()) ? res : (Collection<Collection<T>>) res.build(last);
    }

    @SuppressWarnings("unchecked")
    default <K> HashMap<K, Collection<T>> groupBy(Function<T, K> keyFn) {
        return foldl(HashMap.nil(), (acc, t) -> {
            K key = keyFn.apply(t);
            Collection<T> group = acc.get(key).orElse(empty());
            return acc.put(key, (Collection<T>) group.build(t));
        });
    }

    default <R, S> Collection<S> liftA2(BiFunction<T, R, S> fn, Collection<R> other) { return (Collection<S>) applicative().liftA2(fn, this, (Higher<µ, R>) other); }
    @SuppressWarnings("unchecked")
    default <R1, R2, S> Collection<S> liftA3(F3<T, R1, R2, S> fn, Collection<R1> o1, Collection<R2> o2) { return (Collection<S>) applicative().ap(applicative().ap(applicative().map(t -> r1 -> r2 -> fn.apply(t, r1, r2), (Higher<µ, T>) this), (Higher<µ, R1>) o1), (Higher<µ, R2>) o2); }
    @SuppressWarnings("unchecked")
    default <R1, R2, R3, S> Collection<S> liftA4(F4<T, R1, R2, R3, S> fn, Collection<R1> o1, Collection<R2> o2, Collection<R3> o3) { return (Collection<S>) applicative().ap(applicative().ap(applicative().ap(applicative().map(t -> r1 -> r2 -> r3 -> fn.apply(t, r1, r2, r3), (Higher<µ, T>) this), (Higher<µ, R1>) o1), (Higher<µ, R2>) o2), (Higher<µ, R3>) o3); }

    @SuppressWarnings("unchecked")
    default <R> Collection<R> apply(Collection<Function<T, R>> fns) {
        return (Collection<R>) applicative().ap((Higher<µ, Function<T, R>>) fns, this);
    }

    @SuppressWarnings("unchecked")
    default <R> Collection<Collection<R>> traverse(Function<T, Collection<R>> fn) {
        Collection<Collection<R>> sseed = this.<Collection<R>>empty().build(this.<R>empty());
        return foldl(sseed, (acc, t) -> fn.apply(t).liftA2((r, rs) -> rs.build(r), acc));
    }

    @SuppressWarnings("unchecked")
    static <R> Collection<Collection<R>> sequence(Collection<? extends Collection<R>> rs) { return rs.traverse(id -> id); }

    @SuppressWarnings("unchecked")
    default Collection<T> intersperse(T value) {
        return (Collection<T>) foldl(Tuple.<Boolean, Collection<T>>of(true, this.<T>empty()), (acc, t) -> {
            if ((Boolean) acc.getA().orElse(false)) return Tuple.of(false, acc.getB().orElse(null).build(t));
            return Tuple.of(false, acc.getB().orElse(null).build(value).build(t));
        }).getB().orElse(null);
    }

    @SuppressWarnings("unchecked")
    default Collection<T> intercalate(Collection<? extends Collection<T>> cs) {
        Collection<Collection<T>> casted = (Collection<Collection<T>>) cs;
        return Collection.flatten(casted.intersperse((Collection<T>) this));
    }

    @Override default void forEach(Consumer<? super T> action) { foldl(null, (__, t) -> { action.accept(t); return null; }); }
    default void forEachIndexed(java.util.function.BiConsumer<? super T, Integer> action) { foldl(0, (idx, t) -> { action.accept(t, idx); return idx + 1; }); }

    default boolean any(Predicate<T> pred) { return (Boolean) foldl(false, (acc, t) -> acc || pred.test(t)); }
    default boolean all(Predicate<T> pred) { return (Boolean) foldl(true, (acc, t) -> acc && pred.test(t)); }
    default boolean isEmpty() { return length() == 0; }

    default Maybe<T> head() { return headMaybe(); }
    @SuppressWarnings("unchecked")
    default Maybe<T> headMaybe() { return (Maybe<T>) foldl(Maybe.<T>nothing(), (acc, t) -> acc.isSome() ? acc : Maybe.some(t)); }
    @SuppressWarnings("unchecked")
    default Maybe<T> lastMaybe() { return (Maybe<T>) foldl(Maybe.<T>nothing(), (acc, t) -> Maybe.some(t)); }

    default Maybe<T> reduce(BiFunction<T, T, T> fn) {
        if (isEmpty()) return Maybe.nothing();
        return Maybe.some(drop(1).foldl(head().orElse(null), fn));
    }

    @SuppressWarnings("unchecked")
    static <R> Collection<R> flatten(Collection<? extends Collection<R>> rs) { return rs.foldl((Collection<R>) List.<R>nil(), (acc, r) -> (Collection<R>) acc.concat(r)); }

    @SuppressWarnings("unchecked")
    static <T, µ extends Collection.µ> Collection<T> narrowK(Higher<µ, T> h) { return (Collection<T>) h; }

    @SuppressWarnings("unchecked")
    static <A, S> Collection<A> unfold(S seed, Function<S, Maybe<Tuple<A, S>>> f) {
        Collection<A> res = List.nil();
        S current = seed;
        Maybe<Tuple<A, S>> next;
        while ((next = f.apply(current)).isSome()) {
            Tuple<A, S> pair = next.orElse(null);
            res = res.build(pair.getA().orElse(null));
            current = pair.getB().orElse(null);
        }
        return res;
    }

    @SuppressWarnings("unchecked")
    static <R> Collection<R> from(Iterable<R> i) {
        List<R> res = List.nil();
        for (R r : i) res = (List<R>) res.build(r);
        return res;
    }

    @Override
    default java.util.Iterator<T> iterator() {
        java.util.List<T> list = new java.util.ArrayList<>();
        this.forEach(list::add);
        return list.iterator();
    }

    static <T> AffineTraversal<Collection<T>, T> at(int index) {
        return AffineTraversal.of(c -> c.atIndex(index), (v, c) -> c.updateAtIndex(index, v));
    }

    @SuppressWarnings("unchecked")
    default Maybe<T> atIndex(int index) {
        return (Maybe<T>) foldl(Tuple.<Integer, Maybe<T>>of(0, Maybe.nothing()), (acc, t) -> {
            int currentIdx = acc.getA().orElse(0);
            if (acc.getB().orElse(null) != null && acc.getB().orElse(null).isSome()) return acc;
            return (currentIdx == index) ? Tuple.of(currentIdx + 1, Maybe.some(t)) : Tuple.of(currentIdx + 1, Maybe.<T>nothing());
        }).getB().orElse(Maybe.nothing());
    }

    @SuppressWarnings("unchecked")
    default Collection<T> updateAtIndex(int index, T value) {
        return (Collection<T>) foldl(Tuple.<Integer, Collection<T>>of(0, this.<T>empty()), (acc, t) -> {
            int currentIdx = acc.getA().orElse(0);
            Collection<T> res = acc.getB().orElse(null);
            return (currentIdx == index) ? Tuple.of(currentIdx + 1, res.build(value)) : Tuple.of(currentIdx + 1, res.build(t));
        }).getB().orElse(null);
    }

    @SuppressWarnings("unchecked")
    default Collection<T> distinct() {
        return (Collection<T>) foldl(Tuple.<Collection<T>, Collection<T>>of(List.nil(), empty()), (acc, t) -> {
            Collection<T> seen = acc.getA().orElse(null);
            Collection<T> res = acc.getB().orElse(null);
            if (seen.indexOf(t).isSome()) return acc;
            return Tuple.of(seen.build(t), res.build(t));
        }).getB().orElse(null);
    }

    @SuppressWarnings("unchecked")
    default Task<Collection<T>> parMap(Function<T, T> fn) {
        return Task.parTraverse(List.from(this), t -> Task.of(() -> fn.apply(t)))
            .map(l -> Collection.<T>from((Iterable<T>) l));
    }

    static double sum(Collection<? extends Number> c) { return c.foldl(0.0, (acc, n) -> acc + n.doubleValue()); }

    @SuppressWarnings("unchecked")
    default Collection<T> takeWhile(Predicate<T> pred) {
        return (Collection<T>) foldl(Tuple.<Boolean, Collection<T>>of(true, this.<T>empty()), (acc, t) -> {
            if ((Boolean) acc.getA().orElse(false) && pred.test(t)) return Tuple.of(true, acc.getB().orElse(null).build(t));
            return Tuple.of(false, acc.getB().orElse(null));
        }).getB().orElse(null);
    }

    @SuppressWarnings("unchecked")
    default Collection<T> dropWhile(Predicate<T> pred) {
        return (Collection<T>) foldl(Tuple.<Boolean, Collection<T>>of(true, this.<T>empty()), (acc, t) -> {
            if ((Boolean) acc.getA().orElse(false) && pred.test(t)) return Tuple.of(true, acc.getB().orElse(null));
            return Tuple.of(false, acc.getB().orElse(null).build(t));
        }).getB().orElse(null);
    }

    default Tuple<Collection<T>, Collection<T>> partition(Predicate<T> pred) {
        return foldl(Tuple.of(empty(), empty()), (acc, t) -> {
            Collection<T> t1 = acc.getA().orElse(empty());
            Collection<T> t2 = acc.getB().orElse(empty());
            return pred.test(t) ? Tuple.of(t1.build(t), t2) : Tuple.of(t1, t2.build(t));
        });
    }

    default Tuple<Collection<T>, Collection<T>> span(Predicate<T> pred) { return Tuple.of(takeWhile(pred), dropWhile(pred)); }
    default Collection<Tuple<T, Integer>> zipWithIndex() { return zipWith(Tuple::of, List.range(0, length())); }

    static <T> Traversal<Collection<T>, T> eachP() {
        return new Traversal<Collection<T>, T>() {
            @Override public Collection<T> getAll(Collection<T> s) { return s; }
            @Override public Collection<T> modify(Collection<T> s, java.util.function.UnaryOperator<T> fn) { return (Collection<T>) s.map(fn::apply); }
        };
    }
}
