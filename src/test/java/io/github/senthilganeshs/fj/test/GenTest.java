package io.github.senthilganeshs.fj.test;

import io.github.senthilganeshs.fj.ds.List;
import io.github.senthilganeshs.fj.ds.Tuple;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.Random;

public class GenTest {

    @Test
    public void testBooleanGen() {
        Gen<Boolean> gen = Gen.booleanGen();
        Random rnd = new Random();
        boolean sawTrue = false;
        boolean sawFalse = false;
        for (int i = 0; i < 100; i++) {
            if (gen.sample().apply(rnd)) sawTrue = true;
            else sawFalse = true;
        }
        Assert.assertTrue(sawTrue && sawFalse);
    }

    @Test
    public void testElements() {
        Gen<String> gen = Gen.elements("a", "b", "c");
        Random rnd = new Random();
        for (int i = 0; i < 100; i++) {
            String s = gen.sample().apply(rnd);
            Assert.assertTrue(s.equals("a") || s.equals("b") || s.equals("c"));
        }
    }

    @Test
    public void testOneOf() {
        Gen<Integer> gen = Gen.oneOf(List.of(Gen.pure(1), Gen.pure(2)));
        Random rnd = new Random();
        boolean saw1 = false;
        boolean saw2 = false;
        for (int i = 0; i < 100; i++) {
            int val = gen.sample().apply(rnd);
            if (val == 1) saw1 = true;
            else if (val == 2) saw2 = true;
        }
        Assert.assertTrue(saw1 && saw2);
    }

    @Test
    public void testFrequency() {
        // 90% chance of 1, 10% chance of 2
        Gen<Integer> gen = Gen.frequency(List.of(
            Tuple.of(90, Gen.pure(1)),
            Tuple.of(10, Gen.pure(2))
        ));
        Random rnd = new Random();
        int count1 = 0;
        int count2 = 0;
        for (int i = 0; i < 1000; i++) {
            int val = gen.sample().apply(rnd);
            if (val == 1) count1++;
            else if (val == 2) count2++;
        }
        Assert.assertTrue(count1 > count2);
        Assert.assertTrue(count2 > 0);
    }

    @Test
    public void testFilter() {
        Gen<Integer> gen = Gen.choose(1, 100).filter(i -> i % 2 == 0);
        Random rnd = new Random();
        for (int i = 0; i < 100; i++) {
            int val = gen.sample().apply(rnd);
            Assert.assertTrue(val % 2 == 0);
        }
    }
}
