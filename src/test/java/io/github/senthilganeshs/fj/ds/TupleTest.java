package io.github.senthilganeshs.fj.ds;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TupleTest {

    @Test
    public void testTupleBasic() {
        Tuple<String, Integer> t = Tuple.of("a", 1);
        Assert.assertEquals(t.getA().fromMaybe(""), "a");
        Assert.assertEquals(t.getB().fromMaybe(-1), Integer.valueOf(1));
    }

    @Test
    public void testTupleEmpty() {
        Tuple<String, Integer> t = Tuple.of();
        Assert.assertTrue(t.getA().isNothing());
        Assert.assertTrue(t.getB().isNothing());
    }

    @Test
    public void testTupleMap() {
        Tuple<String, Integer> t = Tuple.of("val", 10);
        Collection<String> mapped = t.map((s, i) -> s + ":" + i);
        Assert.assertEquals(mapped.length(), 1);
        Assert.assertEquals(mapped.foldl("", (acc, s) -> s), "val:10");
    }

    @Test
    public void testTupleMapEmpty() {
        Tuple<String, Integer> t = Tuple.of();
        Collection<String> mapped = t.map((s, i) -> s + ":" + i);
        Assert.assertEquals(mapped.length(), 0);
    }
}
