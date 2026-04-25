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

    @Test
    public void testDistinct() {
        StubCollection<Integer> s = new StubCollection<>(List.of(1, 2, 1, 3, 2, 4));
        Assert.assertEquals(s.distinct().toString(), "[1,2,3,4]");
    }

    @Test
    public void testUnfold() {
        // Generate [5, 4, 3, 2, 1]
        Collection<Integer> result = Collection.unfold(5, i -> i > 0 ? Maybe.some(Tuple.of(i, i - 1)) : Maybe.nothing());
        Assert.assertEquals(result.toString(), "[5,4,3,2,1]");
    }

    @Test
    public void testSpan() {
        StubCollection<Integer> s = new StubCollection<>(List.of(1, 2, 3, 4, 1, 2));
        Tuple<Collection<Integer>, Collection<Integer>> result = s.span(i -> i < 4);
        Assert.assertEquals(result.getA().fromMaybe(List.nil()).toString(), "[1,2,3]");
        Assert.assertEquals(result.getB().fromMaybe(List.nil()).toString(), "[4,1,2]");
    }

    @Test
    public void testChunk() {
        StubCollection<Integer> s = new StubCollection<>(List.of(1, 2, 3, 4, 5));
        Collection<Collection<Integer>> result = s.chunk(2);
        // [[1,2], [3,4], [5]]
        Assert.assertEquals(result.toString(), "[[1,2],[3,4],[5]]");
    }

    @Test
    public void testHeadLastMaybe() {
        StubCollection<Integer> s = new StubCollection<>(List.of(1, 2, 3));
        Assert.assertEquals(s.headMaybe().fromMaybe(-1), Integer.valueOf(1));
        Assert.assertEquals(s.lastMaybe().fromMaybe(-1), Integer.valueOf(3));
        
        StubCollection<Integer> empty = new StubCollection<>(List.nil());
        Assert.assertTrue(empty.headMaybe().isNothing());
        Assert.assertTrue(empty.lastMaybe().isNothing());
    }
}
