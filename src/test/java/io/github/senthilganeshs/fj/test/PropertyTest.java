package io.github.senthilganeshs.fj.test;

import io.github.senthilganeshs.fj.ds.List;
import io.github.senthilganeshs.fj.typeclass.Monoid;
import org.testng.Assert;
import org.testng.annotations.Test;

public class PropertyTest {

    @Test
    public void testGenBasic() {
        Gen<Integer> gen = Gen.choose(1, 10);
        java.util.Random rnd = new java.util.Random();
        for (int i = 0; i < 100; i++) {
            int val = gen.sample().apply(rnd);
            Assert.assertTrue(val >= 1 && val < 10);
        }
    }

    @Test
    public void testPropertySuccess() {
        Property<Integer> prop = Property.forAll(Gen.choose(1, 100), i -> i > 0);
        prop.assertTrue(100);
    }

    @Test(expectedExceptions = AssertionError.class)
    public void testPropertyFailure() {
        Property<Integer> prop = Property.forAll(Gen.choose(-10, 10), i -> i > 0);
        prop.assertTrue(100);
    }

    @Test
    public void testMonoidLaws() {
        Monoid<Integer> sumMonoid = new Monoid<>() {
            public Integer empty() { return 0; }
            public Integer combine(Integer a, Integer b) { return a + b; }
        };
        
        MonoidLaws.check(sumMonoid, Gen.integer(), 100);
    }

    @Test
    public void testListLaws() {
        Monoid<List<Integer>> listMonoid = new Monoid<>() {
            public List<Integer> empty() { return List.nil(); }
            public List<Integer> combine(List<Integer> a, List<Integer> b) { return List.from(a.concat(b)); }
        };

        MonoidLaws.check(listMonoid, (Gen<List<Integer>>) (Gen) Gen.list(Gen.integer(), 10), 50);
    }
}
