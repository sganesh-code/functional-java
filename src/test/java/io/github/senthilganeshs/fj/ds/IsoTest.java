package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.optic.Iso;
import io.github.senthilganeshs.fj.optic.Lens;
import org.testng.Assert;
import org.testng.annotations.Test;

public class IsoTest {

    record Point(int x, int y) {}

    @Test
    public void testIsoBasic() {
        // Iso between Point and Tuple<Integer, Integer>
        Iso<Point, Tuple<Integer, Integer>> pointIso = Iso.of(
            p -> Tuple.of(p.x(), p.y()),
            t -> new Point(t.getA().orElse(0), t.getB().orElse(0))
        );

        Point p = new Point(10, 20);
        Tuple<Integer, Integer> t = pointIso.get(p);
        
        Assert.assertEquals(t.getA().orElse(0), Integer.valueOf(10));
        Assert.assertEquals(pointIso.reverseGet(t), p);
    }

    @Test
    public void testIsoReverse() {
        Iso<String, Integer> stringIntIso = Iso.of(Integer::parseInt, Object::toString);
        Iso<Integer, String> intStringIso = stringIntIso.reverse();

        Assert.assertEquals(intStringIso.get(100), "100");
        Assert.assertEquals(intStringIso.reverseGet("200"), Integer.valueOf(200));
    }

    @Test
    public void testIsoComposition() {
        Iso<Point, Tuple<Integer, Integer>> pointIso = Iso.of(
            p -> Tuple.of(p.x(), p.y()),
            t -> new Point(t.getA().orElse(0), t.getB().orElse(0))
        );
        
        // A lens that works on Tuples
        Lens<Tuple<Integer, Integer>, Integer> firstL = Lens.of(
            t -> t.getA().orElse(0),
            (val, t) -> Tuple.of(val, t.getB().orElse(0))
        );

        // Compose Iso with Lens to get a Lens for Point automatically!
        Lens<Point, Integer> xLens = pointIso.compose(firstL);

        Point p = new Point(1, 2);
        Assert.assertEquals(xLens.get(p), Integer.valueOf(1));
        
        Point updated = xLens.set(10, p);
        Assert.assertEquals(updated.x(), 10);
        Assert.assertEquals(updated.y(), 2);
    }
}
