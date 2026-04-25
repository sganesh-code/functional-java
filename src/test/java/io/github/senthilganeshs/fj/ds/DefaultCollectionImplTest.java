package io.github.senthilganeshs.fj.ds;

import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.function.BiFunction;

public class DefaultCollectionImplTest {

    // A minimal implementation that only provides the required triad
    static class StubCollection<T> implements Collection<T> {
        private final List<T> internal;

        StubCollection(List<T> internal) {
            this.internal = internal;
        }

        @Override
        public <R> Collection<R> empty() {
            return new StubCollection<>(List.nil());
        }

        @Override
        public Collection<T> build(T input) {
            return new StubCollection<>(internal.build(input));
        }

        @Override
        public <R> R foldl(R seed, BiFunction<R, T, R> fn) {
            return internal.foldl(seed, fn);
        }

        @Override
        public String toString() {
            return internal.toString();
        }
    }

    @Test
    public void testDefaultsOnStub() {
        StubCollection<Integer> s = new StubCollection<>(List.of(1, 2, 3));
        
        // This will use Collection.map default
        Collection<Integer> mapped = s.map(i -> i * 2);
        Assert.assertEquals(mapped.toString(), "[2,4,6]");
        
        // This will use Collection.filter default
        Collection<Integer> filtered = s.filter(i -> i % 2 == 0);
        Assert.assertEquals(filtered.toString(), "[2]");
        
        // This will use Collection.drop default
        Assert.assertEquals(s.drop(1).toString(), "[2,3]");
        
        // This will use Collection.take default
        Assert.assertEquals(s.take(1).toString(), "[1]");
        
        // This will use Collection.reverse default
        Assert.assertEquals(s.reverse().toString(), "[3,2,1]");
        
        // This will use Collection.any/all
        Assert.assertTrue(s.any(i -> i == 2));
        Assert.assertFalse(s.all(i -> i == 1));
    }

    @Test
    public void testMapMaybe() {
        StubCollection<Integer> s = new StubCollection<>(List.of(1, 2, 3, 4));
        // Keep only even numbers, and double them
        Collection<Integer> result = s.mapMaybe(i -> i % 2 == 0 ? Maybe.some(i * 2) : Maybe.nothing());
        Assert.assertEquals(result.toString(), "[4,8]");
    }

    @Test
    public void testPartition() {
        StubCollection<Integer> s = new StubCollection<>(List.of(1, 2, 3, 4));
        Tuple<Collection<Integer>, Collection<Integer>> result = s.partition(i -> i % 2 == 0);
        
        Assert.assertEquals(result.getA().fromMaybe(List.nil()).toString(), "[2,4]");
        Assert.assertEquals(result.getB().fromMaybe(List.nil()).toString(), "[1,3]");
    }

    @Test
    public void testTakeWhile() {
        StubCollection<Integer> s = new StubCollection<>(List.of(1, 2, 3, 4, 1, 2));
        Collection<Integer> result = s.takeWhile(i -> i < 4);
        Assert.assertEquals(result.toString(), "[1,2,3]");
    }

    @Test
    public void testDropWhile() {
        StubCollection<Integer> s = new StubCollection<>(List.of(1, 2, 3, 4, 1, 2));
        Collection<Integer> result = s.dropWhile(i -> i < 4);
        Assert.assertEquals(result.toString(), "[4,1,2]");
    }

    @Test
    public void testZipWithIndex() {
        StubCollection<String> s = new StubCollection<>(List.of("a", "b"));
        Collection<Tuple<String, Integer>> result = s.zipWithIndex();
        Assert.assertEquals(result.toString(), "[(a,0),(b,1)]");
    }

    @Test
    public void testZipWith() {
        StubCollection<Integer> s = new StubCollection<>(List.of(1, 2, 3));
        List<Integer> other = List.of(10, 20, 30);
        Collection<Integer> result = s.zipWith(Integer::sum, other);
        Assert.assertEquals(result.toString(), "[11,22,33]");
    }

    @Test
    public void testGroupBy() {
        StubCollection<Integer> s = new StubCollection<>(List.of(1, 2, 3, 4, 5, 6));
        HashMap<String, Collection<Integer>> result = s.groupBy(i -> i % 2 == 0 ? "even" : "odd");
        
        Assert.assertEquals(result.get("even").fromMaybe(List.nil()).toString(), "[2,4,6]");
        Assert.assertEquals(result.get("odd").fromMaybe(List.nil()).toString(), "[1,3,5]");
    }

    @Test
    public void testScanl() {
        StubCollection<Integer> s = new StubCollection<>(List.of(1, 2, 3));
        Collection<Integer> result = s.scanl(0, Integer::sum);
        Assert.assertEquals(result.toString(), "[0,1,3,6]");
    }
}
