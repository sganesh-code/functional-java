package io.github.senthilganeshs.fj.ds;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class LazyListTest {

    @Test
    public void testLazyListBasic() {
        LazyList<Integer> l = LazyList.of(1, 2, 3);
        Assert.assertEquals(l.toString(), "[1,2,3]");
        Assert.assertEquals(l.head().orElse(-1), Integer.valueOf(1));
        Assert.assertEquals(l.tail().head().orElse(-1), Integer.valueOf(2));
        Assert.assertEquals(l.length(), 3);
    }

    @Test
    public void testLazyListLazyEvaluation() {
        AtomicInteger counter = new AtomicInteger(0);
        LazyList<Integer> l = LazyList.iterate(1, i -> {
            counter.incrementAndGet();
            return i + 1;
        });
        
        // At this point, only the head is evaluated (seed), but UnaryOperator hasn't run yet
        // Wait, iterate(seed, fn) evaluates seed as head immediately.
        Assert.assertEquals(counter.get(), 0);
        
        LazyList<Integer> t1 = l.tail();
        Assert.assertEquals(counter.get(), 1);
        Assert.assertEquals(t1.head().orElse(-1), Integer.valueOf(2));
        
        LazyList<Integer> t2 = t1.tail();
        Assert.assertEquals(counter.get(), 2);
        Assert.assertEquals(t2.head().orElse(-1), Integer.valueOf(3));
    }

    @Test
    public void testLazyListInfinite() {
        LazyList<Integer> infinite = LazyList.iterate(1, i -> i + 1);
        
        Collection<Integer> firstTen = infinite.take(10);
        Assert.assertEquals(firstTen.length(), 10);
        Assert.assertEquals(firstTen.toString(), "[1,2,3,4,5,6,7,8,9,10]");
        
        Collection<Integer> dropped = infinite.drop(10).take(5);
        Assert.assertEquals(dropped.toString(), "[11,12,13,14,15]");
    }

    @Test
    public void testLazyListToStringInfinite() {
        LazyList<Integer> infinite = LazyList.iterate(1, i -> i + 1);
        String s = infinite.toString();
        Assert.assertTrue(s.contains("..."));
        Assert.assertTrue(s.startsWith("[1,2,3,4,5,6,7,8,9,10,"));
    }

    @Test
    public void testLazyListEmpty() {
        LazyList<Integer> nil = LazyList.nil();
        Assert.assertTrue(nil.head().isNothing());
        Assert.assertTrue(nil.tail().head().isNothing());
        Assert.assertEquals(nil.length(), 0);
        Assert.assertEquals(nil.toString(), "[]");
    }

    @Test
    public void testLazyListCollectionAPIs() {
        LazyList<Integer> l = LazyList.of(1, 2, 3, 4, 5);
        
        Assert.assertEquals(l.map(i -> i * 2).toString(), "[2,4,6,8,10]");
        Assert.assertEquals(l.filter(i -> i % 2 == 0).toString(), "[2,4]");
        
        // zip
        List<Tuple<Integer, String>> zipped = l.zip(List.of("a", "b", "c"));
        Assert.assertEquals(zipped.length(), 3);
        // zipped is [ (1,a), (2,b), (3,c) ]
        
        // concat
        Collection<Integer> concated = l.concat(LazyList.of(6, 7));
        Assert.assertEquals(concated.length(), 7);
    }

    @Test
    public void testLazyListUnzip() {
        LazyList<Integer> l = LazyList.of(1, 2, 3);
        Tuple<Maybe<Integer>, List<Integer>> unzipped = l.unzip();
        Assert.assertEquals(unzipped.getA().orElse(Maybe.nothing()).orElse(-1), Integer.valueOf(1));
        Assert.assertEquals(unzipped.getB().orElse(List.nil()).length(), 2);
    }
}
