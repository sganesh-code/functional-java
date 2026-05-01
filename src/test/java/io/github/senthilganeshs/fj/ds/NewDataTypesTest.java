package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.typeclass.Monad;
import io.github.senthilganeshs.fj.typeclass.Monoid;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class NewDataTypesTest {

    @Test
    public void testIdentity() {
        Identity<Integer> id = new Identity<>(10);
        Assert.assertEquals(id.value(), Integer.valueOf(10));
        
        Identity<Integer> id2 = Identity.narrowK(Identity.monad.map(i -> i + 5, id));
        Assert.assertEquals(id2.value(), Integer.valueOf(15));
    }

    @Test
    public void testConst() {
        Const<String, Integer> c = new Const<>("Hello");
        Assert.assertEquals(c.value(), "Hello");
        
        // Mapping over Const should keep the value same but change phantom type
        Higher<Higher<Const.µ, String>, String> c2 = Const.<String>functor().map(i -> i.toString(), c);
        Assert.assertEquals(Const.narrowK(c2).value(), "Hello");
    }

    @Test
    public void testEndo() {
        Monoid<Endo<Integer>> monoid = Endo.monoid();
        Endo<Integer> f1 = new Endo<>(i -> i + 1);
        Endo<Integer> f2 = new Endo<>(i -> i * 2);
        
        Endo<Integer> combined = monoid.combine(f1, f2); // f1(f2(5)) = (5 * 2) + 1 = 11
        Assert.assertEquals(combined.run().apply(5), Integer.valueOf(11));
        
        Assert.assertEquals(monoid.combine(monoid.empty(), f1).run().apply(10), Integer.valueOf(11));
    }

    @Test
    public void testThese() {
        These<String, Integer> t1 = These.left("Error");
        These<String, Integer> t2 = These.right(10);
        These<String, Integer> t3 = These.both("Warning", 20);

        Assert.assertEquals(t1.fold(s -> s, i -> i.toString(), (s, i) -> s + i), "Error");
        Assert.assertEquals(t2.fold(s -> s, i -> i.toString(), (s, i) -> s + i), "10");
        Assert.assertEquals(t3.fold(s -> s, i -> i.toString(), (s, i) -> s + i), "Warning20");
    }

    @Test
    public void testNonEmptyList() {
        NonEmptyList<Integer> nel = NonEmptyList.of(1, 2, 3);
        Assert.assertEquals(nel.head(), Integer.valueOf(1));
        Assert.assertEquals(nel.toList().length(), 3);
        
        NonEmptyList<Integer> doubled = nel.map(i -> i * 2);
        Assert.assertEquals(doubled.head(), Integer.valueOf(2));
        Assert.assertEquals(doubled.toList().drop(1).headMaybe().orElse(0), Integer.valueOf(4));
    }
}
