package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.optic.AffineTraversal;
import io.github.senthilganeshs.fj.optic.Traversal;
import io.github.senthilganeshs.fj.typeclass.Applicative;
import io.github.senthilganeshs.fj.typeclass.Eq;
import io.github.senthilganeshs.fj.typeclass.Functor;
import io.github.senthilganeshs.fj.typeclass.Monad;
import io.github.senthilganeshs.fj.typeclass.Monoid;
import io.github.senthilganeshs.fj.ds.Task;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The fundamental abstraction for all functional data structures in this library.
 * Consolidates all combinators and typeclasses into a single polymorphic interface.
 */
public interface Collection<T> extends Higher<Collection.µ, T> {
    final class µ {}

    @SuppressWarnings("unchecked")
    static <T> Collection<T> narrowK(Higher<µ, T> hka) {
        return (Collection<T>) hka;
    }

    @SuppressWarnings("unchecked")
    default <R extends Collection<T>> Maybe<R> narrow(TypeReference<R> typeRef) {
        return typeRef.isInstance(this) ? Maybe.some((R) this) : Maybe.nothing();
    }

    <R> Collection<R> empty();
    Collection<T> build(final T input);
    <R> R foldl(final R seed, final BiFunction<R, T, R> fn);

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
                return monad().flatMap(f -> functor().map(f, fa), ff);
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

    default Monoid<Collection<T>> monoid() {
        return Monoid.of(empty(), Collection::concat);
    }

    @SuppressWarnings("unchecked")
    default <R> Collection<R> map(Function<T, R> fn) {
        return (Collection<R>) functor().map(fn, this);
    }

    @SuppressWarnings("unchecked")
    default <R> Collection<R> flatMap(Function<T, Collection<R>> fn) {
        return (Collection<R>) monad().flatMap(a -> (Higher<µ, R>) fn.apply(a), this);
    }

    @SuppressWarnings("unchecked")
    default <R> Collection<R> apply(Collection<Function<T, R>> fns) {
        return (Collection<R>) applicative().ap((Higher<µ, Function<T, R>>) fns, this);
    }

    @SuppressWarnings("unchecked")
    default <R, S> Collection<S> liftA2(BiFunction<T, R, S> fn, Collection<R> other) {
        return (Collection<S>) applicative().liftA2(fn, this, (Higher<µ, R>) other);
    }

    @SuppressWarnings("unchecked")
    default <A, B, R> Collection<R> liftA3(io.github.senthilganeshs.fj.ds.F3<T, A, B, R> fn, Collection<A> ca, Collection<B> cb) {
        Collection<Function<A, Function<B, R>>> f1 = map(t -> a -> b -> fn.apply(t, a, b));
        Collection<Function<B, R>> f2 = (Collection<Function<B, R>>) ca.applicative().ap((Higher) f1, (Higher) ca);
        return (Collection<R>) cb.applicative().ap((Higher) f2, (Higher) cb);
    }

    @SuppressWarnings("unchecked")
    default <A, B, C, R> Collection<R> liftA4(io.github.senthilganeshs.fj.ds.F4<T, A, B, C, R> fn, Collection<A> ca, Collection<B> cb, Collection<C> cc) {
        Collection<Function<A, Function<B, Function<C, R>>>> f1 = map(t -> a -> b -> c -> fn.apply(t, a, b, c));
        Collection<Function<B, Function<C, R>>> f2 = (Collection<Function<B, Function<C, R>>>) ca.applicative().ap((Higher) f1, (Higher) ca);
        Collection<Function<C, R>> f3 = (Collection<Function<C, R>>) cb.applicative().ap((Higher) f2, (Higher) cb);
        return (Collection<R>) cc.applicative().ap((Higher) f3, (Higher) cc);
    }

    default T fold(Monoid<T> m) {
        return foldl(m.empty(), m::combine);
    }

    default <R> R foldMap(Function<T, R> fn, Monoid<R> m) {
        return map(fn).fold(m);
    }

    default Collection<T> concat(Collection<T> other) {
        return other.foldl(this, Collection::build);
    }

    @SuppressWarnings("unchecked")
    default <R> Collection<Collection<R>> traverse(Function<T, Collection<R>> fn) {
        Collection<Collection<R>> sseed = this.<Collection<R>>empty().build(this.<R>empty());
        return foldl(sseed, (acc, t) -> fn.apply(t).liftA2((r, rs) -> rs.build(r), acc));
    }

    @SuppressWarnings("unchecked")
    default <R> Collection<R> mapMaybe(Function<T, Maybe<R>> fn) {
        return foldl(empty(), (rs, t) -> {
            Maybe<R> res = fn.apply(t);
            return res.isSome() ? rs.build(res.orElse(null)) : rs;
        });
    }

    default boolean isEmpty() {
        return !any(__ -> true);
    }

    default T orElse(T def) {
        return foldl(def, (__, t) -> t);
    }

    default <R> R either(java.util.function.Supplier<R> onEmpty, Function<T, R> onValue) {
        return head().map(onValue).orElse(onEmpty.get());
    }

    default Maybe<T> head() {
        return headMaybe();
    }

    default Collection<T> filter(Predicate<T> pred) {
        return foldl(empty(), (acc, t) -> pred.test(t) ? acc.build(t) : acc);
    }

    default <R> Collection<R> filterType(Class<R> clazz) {
        return filter(clazz::isInstance).map(clazz::cast);
    }

    /**
     * Returns a new collection containing the elements sorted according to the provided comparator.
     */
    @SuppressWarnings("unchecked")
    default Collection<T> sort(java.util.Comparator<? super T> cmp) {
        if (isEmpty()) return this;
        Object[] arr = new Object[length()];
        final int[] i = {0};
        forEach(t -> arr[i[0]++] = t);
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
        return foldl(Maybe.nothing(), (acc, t) -> acc.isSome() ? acc : (pred.test(t) ? Maybe.some(t) : acc));
    }

    @SuppressWarnings("unchecked")
    default Maybe<Integer> findIndex(Predicate<T> pred) {
        return (Maybe<Integer>) foldl(Tuple.of(0, Maybe.<Integer>nothing()), (acc, t) -> {
            Maybe<Integer> res = (Maybe<Integer>) acc.getB().orElse(Maybe.nothing());
            if (res.isSome()) return acc;
            int idx = (Integer) acc.getA().orElse(0);
            return pred.test(t) ? Tuple.of(idx + 1, Maybe.some(idx)) : Tuple.of(idx + 1, Maybe.<Integer>nothing());
        }).getB().orElse(Maybe.nothing());
    }

    default Maybe<Integer> indexOf(T value) {
        return findIndex(t -> t.equals(value));
    }

    default int length() {
        return foldl(0, (acc, __) -> acc + 1);
    }

    default boolean any(Predicate<T> pred) {
        return foldl(false, (acc, t) -> acc || pred.test(t));
    }

    default boolean all(Predicate<T> pred) {
        return foldl(true, (acc, t) -> acc && pred.test(t));
    }

    default Maybe<T> headMaybe() {
        return find(__ -> true);
    }

    default Maybe<T> lastMaybe() {
        return foldl(Maybe.nothing(), (__, t) -> Maybe.some(t));
    }

    @SuppressWarnings("unchecked")
    default <K> HashMap<K, Collection<T>> groupBy(Function<T, K> keyFn) {
        return foldl(HashMap.nil(), (acc, t) -> {
            K key = keyFn.apply(t);
            Collection<T> group = acc.get(key).orElse(this.<T>empty());
            return acc.put(key, (Collection<T>) group.build(t));
        });
    }

    @SuppressWarnings("unchecked")
    default Collection<Tuple<T, Integer>> zipWithIndex() {
        return foldl(Tuple.of(0, this.<Tuple<T, Integer>>empty()), (acc, t) -> {
            int i = acc.getA().orElse(0);
            Collection<Tuple<T, Integer>> res = acc.getB().orElse(null);
            return Tuple.of(i + 1, res.build(Tuple.of(t, i)));
        }).getB().orElse(null);
    }

    @SuppressWarnings("unchecked")
    default <R, S> Collection<S> zipWith(BiFunction<T, R, S> fn, Collection<R> other) {
        Iterator<T> it1 = this.iterator();
        Iterator<R> it2 = other.iterator();
        Collection<S> result = empty();
        while (it1.hasNext() && it2.hasNext()) {
            result = result.build(fn.apply(it1.next(), it2.next()));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    default Collection<T> take(int n) {
        return foldl(Tuple.of(n, this.<T>empty()), (acc, t) -> {
            int remaining = acc.getA().orElse(0);
            Collection<T> res = acc.getB().orElse(null);
            return remaining > 0 ? Tuple.of(remaining - 1, res.build(t)) : acc;
        }).getB().orElse(null);
    }

    @SuppressWarnings("unchecked")
    default Collection<T> drop(int n) {
        return foldl(Tuple.of(n, this.<T>empty()), (acc, t) -> {
            int remaining = acc.getA().orElse(0);
            Collection<T> res = acc.getB().orElse(null);
            return remaining > 0 ? Tuple.of(remaining - 1, res) : Tuple.of(0, res.build(t));
        }).getB().orElse(null);
    }

    default Collection<T> slice(int start, int n) {
        return drop(start).take(n);
    }

    @SuppressWarnings("unchecked")
    default Collection<T> takeWhile(Predicate<T> pred) {
        return foldl(Tuple.of(true, this.<T>empty()), (acc, t) -> {
            boolean active = (Boolean) acc.getA().orElse(false);
            Collection<T> res = acc.getB().orElse(null);
            if (active && pred.test(t)) return Tuple.of(true, res.build(t));
            return Tuple.of(false, res);
        }).getB().orElse(null);
    }

    @SuppressWarnings("unchecked")
    default Collection<T> dropWhile(Predicate<T> pred) {
        return foldl(Tuple.of(true, this.<T>empty()), (acc, t) -> {
            boolean active = (Boolean) acc.getA().orElse(false);
            Collection<T> res = acc.getB().orElse(null);
            if (active && pred.test(t)) return Tuple.of(true, res);
            return Tuple.of(false, res.build(t));
        }).getB().orElse(null);
    }

    @SuppressWarnings("unchecked")
    default Tuple<Collection<T>, Collection<T>> span(Predicate<T> pred) {
        return Tuple.of(takeWhile(pred), dropWhile(pred));
    }

    @SuppressWarnings("unchecked")
    default Tuple<Collection<T>, Collection<T>> partition(Predicate<T> pred) {
        return foldl(Tuple.of(empty(), empty()), (acc, t) -> {
            Collection<T> left = acc.getA().orElse(empty());
            Collection<T> right = acc.getB().orElse(empty());
            return pred.test(t) ? Tuple.of(left.build(t), right) : Tuple.of(left, right.build(t));
        });
    }

    @SuppressWarnings("unchecked")
    default Collection<Collection<T>> chunk(int n) {
        if (n <= 0) return empty();
        Object[] initialState = new Object[]{ 0, this.<Collection<T>>empty(), this.<T>empty() };
        
        Object[] finalState = foldl(initialState, (acc, t) -> {
            int i = (Integer) acc[0];
            Collection<Collection<T>> res = (Collection<Collection<T>>) acc[1];
            Collection<T> current = (Collection<T>) acc[2];
            if (i == n) return new Object[]{ 1, res.build(current), empty().build(t) };
            return new Object[]{ i + 1, res, current.build(t) };
        });
        
        Collection<Collection<T>> res = (Collection<Collection<T>>) finalState[1];
        Collection<T> current = (Collection<T>) finalState[2];
        return current.isEmpty() ? res : res.build(current);
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

    default Maybe<T> reduce(BiFunction<T, T, T> fn) {
        return head().map(h -> drop(1).foldl(h, fn));
    }

    @SuppressWarnings("unchecked")
    default <A, B> Tuple<Collection<A>, Collection<B>> unzip() {
        return foldl(Tuple.of(empty(), empty()), (acc, t) -> {
            Tuple<A, B> pair = (Tuple<A, B>) t;
            Collection<A> as = acc.getA().orElse(empty());
            Collection<B> bs = acc.getB().orElse(empty());
            return Tuple.of(as.build(pair.getA().orElse(null)), bs.build(pair.getB().orElse(null)));
        });
    }

    default String mkString(String sep) {
        return mkString("", sep, "");
    }

    default String mkString(String start, String sep, String end) {
        StringBuilder sb = new StringBuilder(start);
        forEachIndexed((val, i) -> {
            if (i > 0) sb.append(sep);
            sb.append(val);
        });
        return sb.append(end).toString();
    }

    default Collection<T> intersperse(T sep) {
        return zipWithIndex().flatMap(t -> 
            t.getB().orElse(0) == 0 ? List.of(t.getA().orElse(null)) : List.of(sep, t.getA().orElse(null))
        );
    }

    @SuppressWarnings("unchecked")
    default Collection<T> intercalate(Collection<? extends Collection<T>> cs) {
        Collection<Collection<T>> casted = (Collection<Collection<T>>) cs;
        return flatten(casted.intersperse(this));
    }

    default void forEach(Consumer<T> action) {
        foldl(null, (__, t) -> {
            action.accept(t);
            return null;
        });
    }

    default void forEachIndexed(java.util.function.BiConsumer<T, Integer> action) {
        foldl(0, (i, t) -> {
            action.accept(t, i);
            return i + 1;
        });
    }

    default Iterator<T> iterator() {
        java.util.List<T> buffer = new ArrayList<>();
        this.forEach(buffer::add);
        return buffer.iterator();
    }

    default Collection<T> reverse() {
        return foldr(empty(), (t, acc) -> acc.build(t));
    }

    @SuppressWarnings("unchecked")
    default Maybe<T> atIndex(int index) {
        Maybe<T> optimized = narrow(new TypeReference<Vector<T>>(){})
            .flatMapMaybe(v -> v.at(index));
        
        if (optimized.isSome()) return optimized;

        return (Maybe<T>) foldl(Tuple.of(0, Maybe.<T>nothing()), (acc, t) -> {
            Maybe<T> res = (Maybe<T>) acc.getB().orElse(Maybe.nothing());
            if (res.isSome()) return acc;
            int currentIdx = (Integer) acc.getA().orElse(0);
            if (currentIdx == index) return Tuple.of(currentIdx + 1, Maybe.some(t));
            return Tuple.of(currentIdx + 1, Maybe.<T>nothing());
        }).getB().orElse(Maybe.nothing());
    }

    @SuppressWarnings("unchecked")
    default Task<Collection<T>> parMap(Function<T, T> fn) {
        return Task.parTraverse(List.from(this), t -> Task.of(() -> fn.apply(t)))
            .map(l -> Collection.<T>from((Iterable<T>) l));
    }

    static <T> AffineTraversal<Collection<T>, T> at(int index) {
        return AffineTraversal.of(
            c -> c.atIndex(index),
            (v, c) -> c.slice(0, index).concat(List.of(v)).concat(c.drop(index + 1))
        );
    }

    static <T> Traversal<Collection<T>, T> eachP() {
        return Traversal.fromCollection();
    }

    @SuppressWarnings("unchecked")
    static <S, R extends Collection<S>> Collection<S> flatten(Collection<R> rs) {
        return rs.flatMap(id -> id);
    }

    @SuppressWarnings("unchecked")
    static <R> Collection<Collection<R>> sequence(Collection<? extends Collection<R>> rs) {
        return rs.traverse(id -> id);
    }

    static <R extends Number> double sum(Collection<R> rs) {
        return rs.foldl(0.0, (acc, r) -> acc + r.doubleValue());
    }

    @SuppressWarnings("unchecked")
    static <A, S> Collection<A> unfold(S seed, Function<S, Maybe<Tuple<A, S>>> f) {
        Maybe<Tuple<A, S>> res = f.apply(seed);
        if (res.isNothing()) return List.nil();
        Tuple<A, S> pair = res.orElse(null);
        return (Collection<A>) unfold(pair.getB().orElse(null), f).build(pair.getA().orElse(null)).reverse();
    }

    @SuppressWarnings("unchecked")
    static <R> Collection<R> from(Iterable<R> i) {
        List<R> res = List.nil();
        for (R r : i) res = (List<R>) res.build(r);
        return res;
    }

    default int count() { return length(); }

    @SuppressWarnings("unchecked")
    default <R> R foldr(R seed, BiFunction<T, R, R> fn) {
        Function<R, R> res = foldl(a -> a, (g, t) -> s -> g.apply(fn.apply(t, s)));
        return res.apply(seed);
    }

    default boolean contains(final T value) {
        return any(t -> t.equals(value));
    }
}
