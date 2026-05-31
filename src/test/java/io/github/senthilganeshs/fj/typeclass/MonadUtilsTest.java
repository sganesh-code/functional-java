package io.github.senthilganeshs.fj.typeclass;

import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.ds.List;
import io.github.senthilganeshs.fj.ds.Maybe;
import io.github.senthilganeshs.fj.ds.Collection;
import io.github.senthilganeshs.fj.ds.Tuple;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

import java.util.function.BiFunction;
import java.util.function.Function;

public class MonadUtilsTest {

    static final Monad<Collection.µ> collectionMonad = new Collection<Object>() {
        @Override public <R> Collection<R> empty() { return List.nil(); }
        @Override public Collection<Object> build(Object input) { return List.of(input); }
        @Override public <R> R foldl(R seed, BiFunction<R, Object, R> fn) { return seed; }
        @Override public java.util.Iterator<Object> iterator() { return java.util.Collections.emptyIterator(); }
    }.monad();

    static final Monad<Maybe.µ> maybeMonad = new Monad<Maybe.µ>() {
        @Override public <A> Higher<Maybe.µ, A> pure(A a) { return Maybe.some(a); }
        @Override public <A, B> Higher<Maybe.µ, B> ap(Higher<Maybe.µ, Function<A, B>> ff, Higher<Maybe.µ, A> fa) {
            return ((Maybe<A>) fa).flatMap(a -> ((Maybe<Function<A, B>>) ff).map(f -> f.apply(a)));
        }
        @Override public <A, B> Higher<Maybe.µ, B> flatMap(Function<A, Higher<Maybe.µ, B>> fn, Higher<Maybe.µ, A> fa) {
            return ((Maybe<A>) fa).flatMap(a -> (Maybe<B>) fn.apply(a));
        }
    };

    @Test
    public void testApplicativeLiftA2() {
        Maybe<Integer> m1 = Maybe.some(1);
        Maybe<Integer> m2 = Maybe.some(2);
        Maybe<Integer> res = (Maybe<Integer>) Applicative.liftA2(maybeMonad, Integer::sum, m1, m2);
        assertEquals(res.orElse(0), (Integer) 3);

        Maybe<Integer> m3 = Maybe.nothing();
        Maybe<Integer> res2 = (Maybe<Integer>) Applicative.liftA2(maybeMonad, Integer::sum, m1, m3);
        assertTrue(res2.isNothing());
    }

    @Test
    public void testApplicativeProduct() {
        Maybe<Integer> m1 = Maybe.some(1);
        Maybe<String> m2 = Maybe.some("a");
        Maybe<Tuple<Integer, String>> res = (Maybe<Tuple<Integer, String>>) Applicative.product(maybeMonad, m1, m2);
        assertEquals(res.orElse(null).toString(), "(1,a)");
    }

    @Test
    public void testMonadFlatMap() {
        Maybe<Integer> m1 = Maybe.some(1);
        Maybe<Integer> res = (Maybe<Integer>) Monad.flatMap(maybeMonad, x -> Maybe.some(x * 2), m1);
        assertEquals(res.orElse(0), (Integer) 2);
    }

    @Test
    public void testMonadFlatten() {
        Higher<Maybe.µ, Higher<Maybe.µ, Integer>> mm = Maybe.some(Maybe.some(1));
        Maybe<Integer> res = (Maybe<Integer>) Monad.flatten(maybeMonad, mm);
        assertEquals(res.orElse(0), (Integer) 1);
    }

    @Test
    public void testMonadIfM() {
        Maybe<Boolean> condTrue = Maybe.some(true);
        Maybe<Integer> ifTrue = Maybe.some(1);
        Maybe<Integer> ifFalse = Maybe.some(2);
        
        Maybe<Integer> res1 = (Maybe<Integer>) Monad.ifM(maybeMonad, condTrue, ifTrue, ifFalse);
        assertEquals(res1.orElse(0), (Integer) 1);

        Maybe<Boolean> condFalse = Maybe.some(false);
        Maybe<Integer> res2 = (Maybe<Integer>) Monad.ifM(maybeMonad, condFalse, ifTrue, ifFalse);
        assertEquals(res2.orElse(0), (Integer) 2);
    }
}
