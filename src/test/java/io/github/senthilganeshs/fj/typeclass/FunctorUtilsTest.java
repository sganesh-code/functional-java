package io.github.senthilganeshs.fj.typeclass;

import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.ds.List;
import io.github.senthilganeshs.fj.ds.Maybe;
import io.github.senthilganeshs.fj.ds.Collection;
import io.github.senthilganeshs.fj.ds.Tuple;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

import java.util.function.Function;

public class FunctorUtilsTest {

    static final Functor<Collection.µ> collectionFunctor = new Functor<Collection.µ>() {
        @Override
        public <A, B> Higher<Collection.µ, B> map(Function<A, B> fn, Higher<Collection.µ, A> fa) {
            return Collection.narrowK(fa).map(fn);
        }
    };

    static final Bifunctor<Collection.µ> collectionBifunctor = new Bifunctor<Collection.µ>() {
        @Override
        public <A, B, C, D> Higher<Higher<Collection.µ, C>, D> bimap(Function<A, C> fa, Function<B, D> fb, Higher<Higher<Collection.µ, A>, B> fab) {
            // This is a bit of a hack since Collection isn't naturally a Bifunctor,
            // but we can use it for types that are, like Either (if we had its witness).
            // For testing purposes, we'll just use a mock-like implementation if possible,
            // or just test the static utility with a type that is a Bifunctor.
            return null; // Will implement properly if needed, but Bifunctor is usually for Either/Tuple
        }
    };

    @Test
    public void testFunctorMap() {
        List<Integer> list = List.of(1, 2, 3);
        List<Integer> doubled = List.from(Collection.narrowK(Functor.map(collectionFunctor, x -> x * 2, list)));
        assertEquals(doubled.toString(), "[2,4,6]");
    }

    @Test
    public void testFunctorAs() {
        List<Integer> list = List.of(1, 2, 3);
        List<String> asList = List.from(Collection.narrowK(Functor.as(collectionFunctor, "a", list)));
        assertEquals(asList.toString(), "[a,a,a]");
    }

    @Test
    public void testFunctorTupled() {
        List<Integer> list = List.of(1, 2);
        List<Tuple<Integer, Integer>> tupledList = List.from(Collection.narrowK(Functor.tupled(collectionFunctor, list)));
        assertEquals(tupledList.length(), 2);
        // Tuple.toString on (1,1) is "(1,1)" (no space)
        assertEquals(tupledList.headMaybe().orElse(null).toString(), "(1,1)");
    }
}
