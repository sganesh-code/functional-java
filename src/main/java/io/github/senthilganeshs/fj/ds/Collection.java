package io.github.senthilganeshs.fj.ds;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The fundamental abstraction for all functional data structures in this library.
 * 
 * <p>To implement a new data structure, only three methods are required:
 * {@link #empty()}, {@link #build(Object)}, and {@link #foldl(Object, BiFunction)}.
 * All other functional methods are provided as defaults based on this core triad.</p>
 * 
 * @param <T> The type of elements in the collection.
 */
public interface Collection<T> {

    /**
     * Returns an empty instance of the data structure.
     * 
     * @param <R> The type of elements for the empty collection.
     * @return An empty Collection implementation.
     */
    <R> Collection<R> empty();

    /**
     * Creates a new instance of the data structure with the provided element added.
     * This operation is persistent and does not modify the original instance.
     * 
     * @param input The element to add.
     * @return A new Collection containing the added element.
     */
    Collection<T> build (final T input);
    
    /**
     * Performs a left-associative reduction (fold) over the elements of this collection.
     * This is the primary traversal mechanism for all derived operations.
     * 
     * @param <R> The type of the accumulated result.
     * @param seed The initial value for the accumulation.
     * @param fn The combining function (accumulator, element) -> result.
     * @return The final accumulated value.
     */
    <R> R foldl (final R seed, final BiFunction<R,T,R> fn);

    /**
     * Performs a right-associative reduction over the elements of this collection.
     * 
     * @param <R> The type of the accumulated result.
     * @param seed The initial value for the accumulation.
     * @param fn The combining function (element, accumulator) -> result.
     * @return The final accumulated value.
     */
    default <R> R foldr (final R seed, final BiFunction<T,R,R> fn) {
        final Function<R, R> res = 
            foldl(a -> a,
                (g, t) -> s -> g.apply(fn.apply(t, s)));
        return res.apply(seed);
    }

    /**
     * Maps each element to a collection-producing function, then flips the structure.
     * Also known as "inside-out" mapping.
     * 
     * <p>Example: {@code List.of(1, 2).traverse(i -> Maybe.some(i + 1))} results in {@code Some([2, 3])}</p>
     * 
     * @param <R> The type of elements in the inner collection.
     * @param fn Function that returns a collection.
     * @return A collection of collections.
     */
    default <R> Collection<Collection<R>> traverse (final Function<T, Collection<R>> fn) {            
        Collection<Collection<R>> seed = empty();
        Collection<Collection<R>> sseed = seed.build(empty());
       
        return foldl (sseed, (rrs, t) -> fn.apply(t).liftA2((r,  rs) -> rs.build(r), rrs));
    }
   
    /**
     * Returns a new collection containing only elements that satisfy the predicate.
     * 
     * @param pred The condition to test against.
     * @return A filtered collection.
     */
    default Collection<T> filter (final Predicate<T> pred) {
        return foldl(
            empty(),
            (r, t) -> pred.test(t) ? r.build(t) : r);
    }

    /**
     * Filters elements by their class type and casts them.
     * Useful for heterogeneous collections.
     * 
     * @param <R> The target type.
     * @param clazz The class to filter by.
     * @return A collection containing only elements of type R.
     */
    default <R> Collection<R> filterType(Class<R> clazz) {
        return filter(clazz::isInstance).map(clazz::cast);
    }

    /**
     * Finds the first element satisfying the predicate.
     * 
     * @param pred The condition to search for.
     * @return Some(element) if found, Nothing otherwise.
     */
    @SuppressWarnings("unchecked")
    default Maybe<T> find(final Predicate<T> pred) {
        return foldl(Maybe.nothing(), (acc, t) -> acc.isSome() ? acc : (pred.test(t) ? Maybe.some(t) : acc));
    }

    @SuppressWarnings("unchecked")
    default Maybe<Integer> findIndex(final Predicate<T> pred) {
        Object[] state = new Object[2];
        state[0] = 0; // index
        state[1] = Maybe.nothing(); // result
        
        return (Maybe<Integer>) foldl(state, (acc, t) -> {
            int idx = (Integer) acc[0];
            Maybe<Integer> res = (Maybe<Integer>) acc[1];
            if (res.isSome()) return new Object[] { idx + 1, res };
            return new Object[] { idx + 1, pred.test(t) ? Maybe.some(idx) : Maybe.nothing() };
        })[1];
    }

    default Maybe<Integer> indexOf(T value) {
        return findIndex(t -> t.equals(value));
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
            return res.isSome() ? rs.build(res.orElse(null)) : rs;
        });
    }

    @SuppressWarnings("unchecked")
    default Tuple<Collection<T>, Collection<T>> partition (final Predicate<T> pred) {
        return (Tuple<Collection<T>, Collection<T>>) foldl(Tuple.of(this.<T>empty(), this.<T>empty()), (acc, t) -> {
            Tuple<Collection<T>, Collection<T>> tuple = (Tuple<Collection<T>, Collection<T>>) acc;
            Collection<T> left = tuple.getA().orElse(empty());
            Collection<T> right = tuple.getB().orElse(empty());
            return pred.test(t) ? Tuple.of(left.build(t), right) : Tuple.of(left, right.build(t));
        });
    }

    /**
     * Combines two collections by appending the provided collection to the end of this one.
     * 
     * @param first The collection to append.
     * @return A new collection containing elements of both.
     */
    default Collection<T> concat (final Collection<T> first) {
        return first.foldl(this, (ts, t) -> ts.build(t));
    }

    /**
     * Maps each element to a collection and flattens the result into a single collection.
     * 
     * @param <R> The type of elements in the resulting collection.
     * @param fn The transformation function.
     * @return A flattened collection of results.
     */
    default <R> Collection<R> flatMap (final Function<T, Collection<R>> fn) {
        return foldl (
            empty(),
            (rs, t) -> rs.concat(fn.apply(t)));
    }

    /**
     * Pairs each element with its 0-based index.
     * 
     * @return A collection of Tuples (element, index).
     */
    @SuppressWarnings("unchecked")
    default Collection<Tuple<T, Integer>> zipWithIndex() {
        Object[] res = new Object[2];
        res[0] = 0; // Current index
        res[1] = empty();
        
        return (Collection<Tuple<T, Integer>>)foldl(res,
            (r, t) -> new Object[] { (Integer) r[0] + 1, ((Collection<Tuple<T, Integer>>)r[1]).build(Tuple.of(t, (Integer) r[0]))})[1];
    }

    /**
     * Combines two collections element-wise using the provided function.
     * 
     * @param <R> The type of elements in the other collection.
     * @param <S> The type of elements in the resulting collection.
     * @param fn The combining function.
     * @param other The other collection.
     * @return A collection of combined results.
     */
    @SuppressWarnings("unchecked")
    default <R, S> Collection<S> zipWith(final BiFunction<T, R, S> fn, final List<R> other) {
        // We use the already implemented List.zip() logic
        List<T> thisAsList = (this instanceof List) ? (List<T>) this : List.from(this);
        return (Collection<S>) thisAsList.zip(other).map(t -> fn.apply(t.getA().orElse(null), t.getB().orElse(null)));
    }

    /**
     * Applicative apply: applies a collection of functions to this collection of values.
     * 
     * @param <R> The resulting element type.
     * @param fns The collection of functions.
     * @return A collection containing every possible application of the functions to the values.
     */
    default <R> Collection<R> apply (final Collection<Function<T, R>> fns) {
        return fns.flatMap(this::map);
    }

    /**
     * Performs an action for each element in the collection.
     * 
     * @param action The consumer action.
     * @return This collection (for chaining).
     */
    default Collection<T> forEach (final Consumer<T> action) {
        return foldl(this,
            (__, t) -> {
                action.accept(t);
                return this;
            });
    }    

    /**
     * Categorizes elements into a HashMap based on a key extraction function.
     * 
     * @param <K> The type of the key.
     * @param keyFn The key extraction function.
     * @return A HashMap where each key points to a collection of elements belonging to that group.
     */
    @SuppressWarnings("unchecked")
    default <K> HashMap<K, Collection<T>> groupBy(final Function<T, K> keyFn) {
        return (HashMap<K, Collection<T>>) foldl(HashMap.<K, Collection<T>>nil(), (acc, t) -> {
            HashMap<K, Collection<T>> map = (HashMap<K, Collection<T>>) acc;
            K key = keyFn.apply(t);
            Collection<T> group = ((Maybe<Collection<T>>) map.get(key)).orElse(this.<T>empty());
            return (HashMap<K, Collection<T>>) map.put(key, group.build(t));
        });
    }

    /**
     * Like foldl, but returns a collection of all intermediate accumulation states.
     * 
     * @param <R> The type of states.
     * @param seed The initial state.
     * @param fn The accumulation function.
     * @return A collection of states.
     */
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

    /**
     * Splits the collection into two parts based on a predicate.
     * The first part contains the prefix of elements satisfying the predicate.
     * The second part contains the remaining elements.
     * 
     * @param pred The predicate condition.
     * @return A Tuple containing (takeWhile, dropWhile).
     */
    @SuppressWarnings("unchecked")
    default Tuple<Collection<T>, Collection<T>> span (final Predicate<T> pred) {
        Object[] res = new Object[3];
        res[0] = true; // Flag: still taking?
        res[1] = empty(); // TakeWhile part
        res[2] = empty(); // DropWhile part
        
        Object[] finalRes = (Object[]) foldl(res, (r, t) -> {
            boolean taking = (Boolean) r[0];
            Collection<T> taken = (Collection<T>) r[1];
            Collection<T> dropped = (Collection<T>) r[2];
            
            if (taking && pred.test(t)) {
                return new Object[] { true, taken.build(t), dropped };
            } else {
                return new Object[] { false, taken, dropped.build(t) };
            }
        });
        return Tuple.of((Collection<T>)finalRes[1], (Collection<T>)finalRes[2]);
    }

    /**
     * Splits the collection into sub-collections of the specified size.
     * 
     * @param size The size of each chunk.
     * @return A collection of chunks.
     */
    @SuppressWarnings("unchecked")
    default Collection<Collection<T>> chunk (int size) {
        if (size <= 0) return empty();
        
        Object[] res = new Object[3];
        res[0] = 0;       // Current chunk count
        res[1] = empty(); // Current chunk being built
        res[2] = this.<Collection<T>>empty(); // Final collection of chunks
        
        Object[] finalRes = (Object[]) foldl(res, (r, t) -> {
            int count = (Integer) r[0];
            Collection<T> current = (Collection<T>) r[1];
            Collection<Collection<T>> chunks = (Collection<Collection<T>>) r[2];
            
            if (count < size) {
                return new Object[] { count + 1, current.build(t), chunks };
            } else {
                return new Object[] { 1, this.<T>empty().build(t), chunks.build(current) };
            }
        });
        
        Collection<T> last = (Collection<T>) finalRes[1];
        Collection<Collection<T>> chunks = (Collection<Collection<T>>) finalRes[2];
        return last.count() > 0 ? chunks.build(last) : chunks;
    }

    /**
     * Returns the total number of elements in the collection.
     * 
     * @return The size of the collection.
     */
    default int length() {
        return foldl(0, (r, t) -> r + 1);
    }
    
    /**
     * Drops the first n elements from the collection.
     * 
     * @param n Number of elements to drop.
     * @return A collection containing all but the first n elements.
     */
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

    /**
     * Reverses the order of elements in the collection.
     * 
     * @return A reversed collection.
     */
    default Collection<T> reverse () {
        return foldr (empty(), 
            (t, r) -> r.build(t));
    }
    
    /**
     * Takes elements as long as the predicate is satisfied.
     * 
     * @param pred The predicate condition.
     * @return A collection containing the prefix elements satisfying the predicate.
     */
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

    /**
     * Drops elements as long as the predicate is satisfied.
     * 
     * @param pred The predicate condition.
     * @return A collection containing the remaining elements after dropping the prefix.
     */
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
    
    /**
     * Takes the first n elements from the collection.
     * 
     * @param n Number of elements to take.
     * @return A collection containing only the first n elements.
     */
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
    
    /**
     * Places the separator element between every two existing elements.
     * 
     * @param sep The separator.
     * @return A collection with separators interspersed.
     */
    default Collection<T> intersperse (final T sep) {
        return drop(1).foldl(take(1), (r, t) -> r.build(sep).build(t));
    }

    /**
     * Intersperses this collection between every two collections in the input collection.
     * 
     * @param rss A collection of collections.
     * @return A single collection with elements of this collection intercalated.
     */
    default Collection<Collection<T>> intercalate(final Collection<Collection<T>> rss) {
        return rss.drop(1).foldl(rss.take(1), (r, t) -> r.build(this).build(t));
    }

    /**
     * Returns a view of a portion of this collection.
     * 
     * @param start The starting index.
     * @param n The number of elements.
     * @return A slice of the collection.
     */
    default public Collection<T> slice(int start, int n) {
        return drop(start).take(n);
    }

    /**
     * Alias for {@link #length()}.
     * 
     * @return The element count.
     */
    default public int count() {
        return foldl(0, (count, t) -> count + 1);
    }

    /**
     * Returns true if any element satisfies the predicate.
     * 
     * @param pred The predicate condition.
     * @return True if at least one match is found.
     */
    default boolean any(Predicate<T> pred) {
        return foldl(false, (acc, t) -> acc || pred.test(t));
    }

    /**
     * Returns true if all elements satisfy the predicate.
     * 
     * @param pred The predicate condition.
     * @return True if all elements match.
     */
    default boolean all(Predicate<T> pred) {
        return foldl(true, (acc, t) -> acc && pred.test(t));
    }

    /**
     * Returns the first element of the collection safely.
     * 
     * @return Some(head) if not empty, Nothing otherwise.
     */
    default Maybe<T> headMaybe() {
        return find(i -> true);
    }

    /**
     * Returns the last element of the collection safely.
     * 
     * @return Some(last) if not empty, Nothing otherwise.
     */
    default Maybe<T> lastMaybe() {
        return foldl(Maybe.nothing(), (acc, t) -> Maybe.some(t));
    }

    /**
     * Reduces the collection to a single value using a binary function.
     * 
     * @param fn The reduction function.
     * @return Some(result) if not empty, Nothing otherwise.
     */
    @SuppressWarnings("unchecked")
    default Maybe<T> reduce(BiFunction<T, T, T> fn) {
        return (Maybe<T>) foldl((Collection<T>) Maybe.nothing(), (acc, t) -> {
            Maybe<T> maybeAcc = (Maybe<T>) acc;
            Maybe<T> res = (Maybe<T>) maybeAcc.flatMap(v -> Maybe.some(fn.apply(v, t)));
            return (Collection<T>) (res.isNothing() ? Maybe.some(t) : res);
        });
    }

    /**
     * Renders the collection as a string with customizable boundaries and separator.
     * 
     * @param start Starting string.
     * @param sep Separator string.
     * @param end Ending string.
     * @return The rendered string.
     */
    default String mkString(String start, String sep, String end) {
        return foldl(start, (acc, t) -> acc + (acc.equals(start) ? "" : sep) + t) + end;
    }

    /**
     * Renders the collection as a string with a separator.
     * 
     * @param sep Separator string.
     * @return The rendered string.
     */
    default String mkString(String sep) {
        return mkString("", sep, "");
    }

    /**
     * Returns a new collection containing only unique elements using standard equals.
     * 
     * @return A collection without duplicates.
     */
    @SuppressWarnings("unchecked")
    default Collection<T> distinct() {
        return distinct(Eq.fromEquals());
    }

    /**
     * Returns a new collection containing only unique elements using the provided Eq strategy.
     * 
     * @param eq The equality strategy.
     * @return A collection without duplicates.
     */
    @SuppressWarnings("unchecked")
    default Collection<T> distinct(Eq<T> eq) {
        return foldl(empty(), (results, t) -> {
            if (results.any(existing -> eq.eq(existing, t))) return results;
            return results.build(t);
        });
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

    default T fold(Monoid<T> monoid) {
        return foldl(monoid.empty(), monoid::combine);
    }

    default <R> R foldMap(Function<T, R> fn, Monoid<R> monoid) {
        return map(fn).foldl(monoid.empty(), monoid::combine);
    }

    public static <T, S> Collection<T> unfold(S seed, Function<S, Maybe<Tuple<T, S>>> f) {
        return (Collection<T>) f.apply(seed).foldl(
            (Collection<T>) List.<T>nil(),
            (nil, tuple) -> {
                T val = tuple.getA().orElse(null);
                S nextSeed = tuple.getB().orElse(null);
                return (Collection<T>) List.of(val).concat(unfold(nextSeed, f));
            }
        );
    }
 
}