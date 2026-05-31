package io.github.senthilganeshs.fj.typeclass;

import io.github.senthilganeshs.fj.ds.List;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class MonoidUtilsTest {

    @Test
    public void testSemigroupCombine() {
        assertEquals(Semigroup.combine(Monoid.INTEGER_SUM, 1, 2), (Integer) 3);
    }

    @Test
    public void testSemigroupCombineN() {
        assertEquals(Semigroup.combineN(Monoid.INTEGER_SUM, 5, 3), (Integer) 15);
        assertEquals(Semigroup.combineN(Monoid.STRING_CONCAT, "a", 3), "aaa");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSemigroupCombineNInvalid() {
        Semigroup.combineN(Monoid.INTEGER_SUM, 5, 0);
    }

    @Test
    public void testMonoidCombineAll() {
        assertEquals(Monoid.combineAll(Monoid.INTEGER_SUM, List.of(1, 2, 3, 4)), (Integer) 10);
        assertEquals(Monoid.combineAll(Monoid.STRING_CONCAT, List.of("a", "b", "c")), "abc");
        assertEquals(Monoid.combineAll(Monoid.INTEGER_SUM, List.<Integer>nil()), (Integer) 0);
    }

    @Test
    public void testMonoidFoldMap() {
        assertEquals(Monoid.foldMap(Monoid.INTEGER_SUM, String::length, List.of("a", "bb", "ccc")), (Integer) 6);
    }

    @Test
    public void testMonoidIntercalate() {
        assertEquals(Monoid.intercalate(Monoid.STRING_CONCAT, ", ", List.of("a", "b", "c")), "a, b, c");
        assertEquals(Monoid.intercalate(Monoid.STRING_CONCAT, ", ", List.of("a")), "a");
        assertEquals(Monoid.intercalate(Monoid.STRING_CONCAT, ", ", List.<String>nil()), "");
    }
}
