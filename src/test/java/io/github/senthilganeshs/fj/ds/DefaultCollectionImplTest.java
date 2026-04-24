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
}
