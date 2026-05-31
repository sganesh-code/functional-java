package io.github.senthilganeshs.fj.typeclass;

import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.ds.List;
import io.github.senthilganeshs.fj.ds.Collection;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

import java.util.function.BiFunction;
import java.util.function.Function;

public class FoldableTest {

    static final Foldable<Collection.µ> collectionFoldable = new Foldable<Collection.µ>() {
        @Override
        public <A, B> B foldl(BiFunction<B, A, B> f, B seed, Higher<Collection.µ, A> fa) {
            return Collection.narrowK(fa).foldl(seed, f);
        }

        @Override
        public <A, B> B foldr(BiFunction<A, B, B> f, B seed, Higher<Collection.µ, A> fa) {
            // Simple foldr using reverse and foldl
            return Collection.narrowK(fa).reverse().foldl(seed, (b, a) -> f.apply(a, b));
        }
    };

    @Test
    public void testFoldMap() {
        List<Integer> list = List.of(1, 2, 3, 4);
        int sum = Foldable.foldMap(collectionFoldable, Monoid.INTEGER_SUM, x -> x, list);
        assertEquals(sum, 10);
    }

    @Test
    public void testFold() {
        List<String> list = List.of("a", "b", "c");
        String concat = Foldable.fold(collectionFoldable, Monoid.STRING_CONCAT, list);
        assertEquals(concat, "abc");
    }

    @Test
    public void testContramap() {
        Eq<Integer> eqInt = Eq.fromEquals();
        Eq<String> eqStringLen = Eq.contramap(eqInt, String::length);

        assertTrue(eqStringLen.eq("abc", "def"));
        assertFalse(eqStringLen.eq("abc", "abcd"));
    }
}
