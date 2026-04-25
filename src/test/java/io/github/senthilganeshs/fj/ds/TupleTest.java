package io.github.senthilganeshs.fj.ds;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TupleTest {

    @Test
    public void testTupleBasic() {
        Tuple<String, Integer> t = Tuple.of("a", 1);
        Assert.assertEquals(t.getA().orElse(""), "a");
        Assert.assertEquals(t.getB().orElse(-1), Integer.valueOf(1));
    }

    @Test
    public void testTupleSwap() {
        Tuple<String, Integer> t = Tuple.of("a", 1);
        Tuple<Integer, String> s = t.swap();
        Assert.assertEquals(s.getA().orElse(-1), Integer.valueOf(1));
        Assert.assertEquals(s.getB().orElse(""), "a");
    }

    @Test
    public void testTupleBimap() {
        Tuple<String, Integer> t = Tuple.of("a", 1);
        Tuple<String, Integer> b = t.bimap(s -> s + "!", i -> i + 1);
        Assert.assertEquals(b.getA().orElse(""), "a!");
        Assert.assertEquals(b.getB().orElse(-1), Integer.valueOf(2));
    }
}
