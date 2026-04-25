package io.github.senthilganeshs.fj.ds;

import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.function.BiFunction;

public class DefaultCollectionImplTest {

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
        
        Assert.assertEquals(s.map(i -> i * 2).toString(), "[2,4,6]");
        Assert.assertEquals(s.filter(i -> i % 2 == 0).toString(), "[2]");
        Assert.assertEquals(s.drop(1).toString(), "[2,3]");
        Assert.assertEquals(s.take(1).toString(), "[1]");
        Assert.assertEquals(s.reverse().toString(), "[3,2,1]");
    }

    @Test
    public void testMapMaybe() {
        StubCollection<Integer> s = new StubCollection<>(List.of(1, 2, 3, 4));
        Collection<Integer> result = s.mapMaybe(i -> i % 2 == 0 ? Maybe.some(i * 2) : Maybe.nothing());
        Assert.assertEquals(result.toString(), "[4,8]");
        
        Assert.assertEquals(s.mapMaybe(i -> Maybe.<Integer>nothing()).length(), 0);
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
        Assert.assertEquals(s.takeWhile(i -> i < 4).toString(), "[1,2,3]");
        Assert.assertEquals(s.takeWhile(i -> false).length(), 0);
    }

    @Test
    public void testDropWhile() {
        StubCollection<Integer> s = new StubCollection<>(List.of(1, 2, 3, 4, 1, 2));
        Assert.assertEquals(s.dropWhile(i -> i < 4).toString(), "[4,1,2]");
        Assert.assertEquals(s.dropWhile(i -> false).length(), 6);
    }

    @Test
    public void testAnyAllExhaustive() {
        StubCollection<Integer> s = new StubCollection<>(List.of(1, 2, 3));
        Assert.assertTrue(s.any(i -> i == 3));
        Assert.assertFalse(s.any(i -> i == 4));
        Assert.assertTrue(s.all(i -> i < 10));
        Assert.assertFalse(s.all(i -> i < 3));
    }

    @Test
    public void testReduceExhaustive() {
        StubCollection<Integer> s = new StubCollection<>(List.of(10));
        Assert.assertEquals(s.reduce(Integer::sum).fromMaybe(-1), Integer.valueOf(10));
        
        StubCollection<Integer> s2 = new StubCollection<>(List.of(1, 2, 3));
        Assert.assertEquals(s2.reduce(Integer::sum).fromMaybe(-1), Integer.valueOf(6));
    }

    @Test
    public void testHeadLastMaybe() {
        StubCollection<Integer> s = new StubCollection<>(List.of(1, 2, 3));
        Assert.assertEquals(s.headMaybe().fromMaybe(-1), Integer.valueOf(1));
        Assert.assertEquals(s.lastMaybe().fromMaybe(-1), Integer.valueOf(3));
    }

    @Test
    public void testDistinct() {
        StubCollection<Integer> s = new StubCollection<>(List.of(1, 2, 1, 3, 2, 4));
        Assert.assertEquals(s.distinct().toString(), "[1,2,3,4]");
    }

    @Test
    public void testUnfold() {
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
        Assert.assertEquals(s.chunk(2).toString(), "[[1,2],[3,4],[5]]");
    }
}
