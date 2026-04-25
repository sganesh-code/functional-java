package io.github.senthilganeshs.fj.ds;

import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.concurrent.atomic.AtomicInteger;

public class LazyTest {

    @Test
    public void testMemoization() {
        AtomicInteger counter = new AtomicInteger(0);
        Lazy<Integer> l = Lazy.of(() -> counter.incrementAndGet());
        
        Assert.assertEquals(counter.get(), 0); // Not triggered yet
        
        Assert.assertEquals(l.get(), Integer.valueOf(1));
        Assert.assertEquals(l.get(), Integer.valueOf(1)); // Cached
        Assert.assertEquals(counter.get(), 1); // Only run once
    }

    @Test
    public void testLazyMapping() {
        AtomicInteger counter = new AtomicInteger(0);
        Lazy<Integer> l = Lazy.of(() -> counter.incrementAndGet());
        
        Lazy<Integer> mapped = l.map(i -> i * 10);
        Assert.assertEquals(counter.get(), 0); // Map is also lazy
        
        Assert.assertEquals(mapped.get(), Integer.valueOf(10));
        Assert.assertEquals(counter.get(), 1);
    }
}
