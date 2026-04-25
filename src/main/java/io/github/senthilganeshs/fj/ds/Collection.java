package io.github.senthilganeshs.fj.ds;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public interface Collection<T> {

    /**
     * Returns empty implementation of this data-structure.
     * 
     * @param <R>
     * @return
     */
    <R> Collection<R> empty();

    /**
     * Builds the data-structure with the input value.
     * 
     * @param input
     * @return
     */
    Collection<T> build (final T input);
    
    
    /**
     * Travel the data-structure in the left to right order.
     * 
     * @param <R>
     * @param seed
     * @param fn
     * @return
     */
    
    <R> R foldl (final R seed, final BiFunction<R,T,R> fn);


    
    default <R> R foldr (final R seed, final BiFunction<T,R,R> fn) {
        final Function<R, R> res = 
            foldl(a -> a,
                (g, t) -> s -> g.apply(fn.apply(t, s)));
        return res.apply(seed);
    }

    default <R> Collection<Collection<R>> traverse (final Function<T, Collection<R>> fn) {            
        Collection<Collection<R>> seed = empty();
        Collection<Collection<R>> sseed = seed.build(empty());
       
        return foldl (sseed, (rrs, t) -> fn.apply(t).liftA2((r,  rs) -> rs.build(r), rrs));
    }
   
    default Collection<T> filter (final Predicate<T> pred) {
        return foldl(
            empty(),
            (r, t) -> pred.test(t) ? r.build(t) : r);
    }

    @SuppressWarnings("unchecked")
    default Maybe<T> find(final Predicate<T> pred) {
        return foldl(Maybe.nothing(), (acc, t) -> acc.isSome() ? acc : (pred.test(t) ? Maybe.some(t) : acc));
    }

    default <R, S> Collection<S> liftA2 (final Function<T, Function<R, S>> fn, final Collection<R> rs) {
        return rs.apply(map(fn::apply));        
    }

    default <R, S> Collection<S> liftA2 (final BiFunction<T, R, S> fn, final Collection<R> rs) {
        return liftA2(t -> r -> fn.apply(t, r), rs);
    }
    
    default <P, Q, R> Collection<R> liftA3 (final Function<T, BiFunction<P, Q, R>> fn, final Collection<P> ps, final Collection<Q> qs) {
        return apply(ps.liftA2((p,  q) -> (t -> fn.apply(t).apply(p, q)), qs));        
    }
    
    default <P, Q, R, S> Collection<S> liftA4 (
        final Function<T, Function<P, BiFunction<Q, R, S>>> fn, 
        final Collection<P> ps, 
        final Collection<Q> qs,
        final Collection<R> rs) {
       
        return apply(
            ps.liftA3(p -> (q, r) -> (t -> fn.apply(t).apply(p).apply(q, r)), qs, rs));
    }

    default <R> Collection<R> map (final Function<T, R> fn) {
        return foldl (
            empty(),
            (rs, t) -> rs.build(fn.apply(t)));
    }

    @SuppressWarnings("unchecked")
    default <R> Collection<R> mapMaybe (final Function<T, Maybe<R>> fn) {
        return foldl(empty(), (rs, t) -> {
            Maybe<R> res = fn.apply(t);
            return res.isSome() ? rs.build(res.fromMaybe(null)) : rs;
        });
    }

    @SuppressWarnings("unchecked")
    default Tuple<Collection<T>, Collection<T>> partition (final Predicate<T> pred) {
        return (Tuple<Collection<T>, Collection<T>>) foldl(Tuple.of(this.<T>empty(), this.<T>empty()), (acc, t) -> {
            Tuple<Collection<T>, Collection<T>> tuple = (Tuple<Collection<T>, Collection<T>>) acc;
            Collection<T> left = tuple.getA().fromMaybe(empty());
            Collection<T> right = tuple.getB().fromMaybe(empty());
            return pred.test(t) ? Tuple.of(left.build(t), right) : Tuple.of(left, right.build(t));
        });
    }

    default Collection<T> concat (final Collection<T> first) {
        return first.foldl(this, (ts, t) -> ts.build(t));
    }

    default <R> Collection<R> flatMap (final Function<T, Collection<R>> fn) {
        return foldl (
            empty(),
            (rs, t) -> rs.concat(fn.apply(t)));
    }

    @SuppressWarnings("unchecked")
    default Collection<Tuple<T, Integer>> zipWithIndex() {
        Object[] res = new Object[2];
        res[0] = 0; // Current index
        res[1] = empty();
        
        return (Collection<Tuple<T, Integer>>)foldl(res,
            (r, t) -> new Object[] { (Integer) r[0] + 1, ((Collection<Tuple<T, Integer>>)r[1]).build(Tuple.of(t, (Integer) r[0]))})[1];
    }

    @SuppressWarnings("unchecked")
    default <R, S> Collection<S> zipWith(final BiFunction<T, R, S> fn, final List<R> other) {
        // We use the already implemented List.zip() logic
        List<T> thisAsList;
        if (this instanceof List) {
            thisAsList = (List<T>) this;
        } else {
            thisAsList = foldl(List.<T>nil(), (l, t) -> (List<T>) l.build(t));
        }
        return (Collection<S>) thisAsList.zip(other).map(t -> fn.apply(t.getA().fromMaybe(null), t.getB().fromMaybe(null)));
    }

    default <R> Collection<R> apply (final Collection<Function<T, R>> fns) {
        return fns.flatMap(this::map);
    }

    default Collection<T> forEach (final Consumer<T> action) {
        return foldl(this,
            (__, t) -> {
                action.accept(t);
                return this;
            });
    }    

    @SuppressWarnings("unchecked")
    default <K> HashMap<K, Collection<T>> groupBy(final Function<T, K> keyFn) {
        return (HashMap<K, Collection<T>>) foldl(HashMap.<K, Collection<T>>nil(), (acc, t) -> {
            HashMap<K, Collection<T>> map = (HashMap<K, Collection<T>>) acc;
            K key = keyFn.apply(t);
            Collection<T> group = ((Maybe<Collection<T>>) map.get(key)).fromMaybe(this.<T>empty());
            return (HashMap<K, Collection<T>>) map.put(key, group.build(t));
        });
    }

    @SuppressWarnings("unchecked")
    default <R> Collection<R> scanl(final R seed, final BiFunction<R, T, R> fn) {
        Object[] state = new Object[2];
        state[0] = seed;
        state[1] = this.<R>empty().build(seed);
        
        return (Collection<R>) foldl(state, (acc, t) -> {
            R current = (R) acc[0];
            Collection<R> results = (Collection<R>) acc[1];
            R next = fn.apply(current, t);
            return new Object[] { next, results.build(next) };
        })[1];
    }

    default int length() {
        return foldl(0, (r, t) -> r + 1);
    }
    
    @SuppressWarnings("unchecked")
    default Collection<T> drop (final int n) {
        Object[] res = new Object[2];
        res[0] = n;
        res[1] = empty();
        return (Collection<T>) foldl (res, 
            (r, t) -> ((Integer) r[0] > 0) ? 
            new Object[] {(Integer) r[0] - 1, r[1]} : 
            new Object[] {(Integer) r[0], ((Collection<T>)r[1]).build(t)})[1];
    }

    default Collection<T> reverse () {
        return foldr (empty(), 
            (t, r) -> r.build(t));
    }
    
    @SuppressWarnings("unchecked")
    default Collection<T> takeWhile (final Predicate<T> pred) {
        Object[] res = new Object[2];
        res[0] = true; // Boolean flag: still taking?
        res[1] = empty();
        
        return (Collection<T>)foldl(res,
            (r, t) -> ((Boolean) r[0] && pred.test(t)) ?
                new Object[] { true, ((Collection<T>)r[1]).build(t)} :
                new Object[] { false, r[1]})[1];
    }

    @SuppressWarnings("unchecked")
    default Collection<T> dropWhile (final Predicate<T> pred) {
        Object[] res = new Object[2];
        res[0] = true; // Boolean flag: still dropping?
        res[1] = empty();
        
        return (Collection<T>)foldl(res,
            (r, t) -> ((Boolean) r[0] && pred.test(t)) ?
                new Object[] { true, r[1]} :
                new Object[] { false, ((Collection<T>)r[1]).build(t)})[1];
    }
    
    @SuppressWarnings("unchecked")
    default Collection<T> take (final int n) {
        Object[] res = new Object[2];
        res[0] = n;
        res[1] = empty();
        
        return (Collection<T>)foldl(res,
            (r, t) -> ((Integer) r[0] > 0) ?
                new Object[] { (Integer) r[0] - 1, ((Collection<T>)r[1]).build(t)} :
                new Object[] { (Integer) r[0], r[1]})[1];
    }
    
    default Collection<T> intersperse (final T sep) {
        return drop(1).foldl(take(1), (r, t) -> r.build(sep).build(t));
    }

    default Collection<Collection<T>> intercalate(final Collection<Collection<T>> rss) {
        return rss.drop(1).foldl(rss.take(1), (r, t) -> r.build(this).build(t));
    }

    default public Collection<T> slice(int start, int n) {
        return drop(start).take(n);
    }

    default public int count() {
        return foldl(0, (count, t) -> count + 1);
    }

    default boolean any(Predicate<T> pred) {
        return foldl(false, (acc, t) -> acc || pred.test(t));
    }

    default boolean all(Predicate<T> pred) {
        return foldl(true, (acc, t) -> acc && pred.test(t));
    }

    @SuppressWarnings("unchecked")
    default Maybe<T> reduce(BiFunction<T, T, T> fn) {
        return (Maybe<T>) foldl((Collection<T>) Maybe.nothing(), (acc, t) -> {
            Maybe<T> maybeAcc = (Maybe<T>) acc;
            Maybe<T> res = (Maybe<T>) maybeAcc.flatMap(v -> Maybe.some(fn.apply(v, t)));
            return (Collection<T>) (res.isNothing() ? Maybe.some(t) : res);
        });
    }

    default String mkString(String start, String sep, String end) {
        return foldl(start, (acc, t) -> acc + (acc.equals(start) ? "" : sep) + t) + end;
    }

    default String mkString(String sep) {
        return mkString("", sep, "");
    }

    public static <S, R extends Collection<S>> Collection<S> flatten(Collection<R> rs) {
        return rs.flatMap(id -> id);
    }

    public static <R> Collection<Collection<R>> sequence (final Collection<Collection<R>> rs) {
        return rs.traverse(id -> id);
    }

    public static <R extends Number> double sum(Collection<R> rs) {
        return rs.foldl(0.0, (acc, r) -> acc + r.doubleValue());
    }
 
}