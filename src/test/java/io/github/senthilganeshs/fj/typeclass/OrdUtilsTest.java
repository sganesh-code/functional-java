package io.github.senthilganeshs.fj.typeclass;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class OrdUtilsTest {

    @Test
    public void testEqNotEq() {
        Eq<Integer> eq = Eq.fromEquals();
        assertTrue(Eq.notEq(eq, 1, 2));
        assertFalse(Eq.notEq(eq, 1, 1));
    }

    @Test
    public void testOrdMinMax() {
        Ord<Integer> ord = Ord.natural();
        assertEquals(Ord.min(ord, 1, 2), (Integer) 1);
        assertEquals(Ord.min(ord, 2, 1), (Integer) 1);
        assertEquals(Ord.max(ord, 1, 2), (Integer) 2);
        assertEquals(Ord.max(ord, 2, 1), (Integer) 2);
    }

    @Test
    public void testOrdClamp() {
        Ord<Integer> ord = Ord.natural();
        assertEquals(Ord.clamp(ord, 1, 10, 5), (Integer) 5);
        assertEquals(Ord.clamp(ord, 1, 10, 0), (Integer) 1);
        assertEquals(Ord.clamp(ord, 1, 10, 15), (Integer) 10);
    }

    @Test
    public void testOrdBetween() {
        Ord<Integer> ord = Ord.natural();
        assertTrue(Ord.between(ord, 1, 10, 5));
        assertTrue(Ord.between(ord, 1, 10, 1));
        assertTrue(Ord.between(ord, 1, 10, 10));
        assertFalse(Ord.between(ord, 1, 10, 0));
        assertFalse(Ord.between(ord, 1, 10, 11));
    }

    @Test
    public void testContramap() {
        Ord<Integer> ordInt = Ord.natural();
        Ord<String> ordStringLen = Ord.contramap(ordInt, String::length);

        assertTrue(ordStringLen.lt("a", "abc"));
        assertTrue(ordStringLen.eq("abc", "def"));
        assertEquals(Ord.min(ordStringLen, "abc", "a"), "a");
    }
}
