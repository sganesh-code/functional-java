package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.typeclass.Hashable;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class SketchesTest {

    private final Hashable<String> stringHashable = value -> value;

    @Test
    public void testBloomFilter() {
        BloomFilter<String> bf = BloomFilter.empty(100, 3, stringHashable);
        
        bf = (BloomFilter<String>) bf.build("apple").build("banana").build("cherry");
        
        assertTrue(bf.contains("apple"));
        assertTrue(bf.contains("banana"));
        assertTrue(bf.contains("cherry"));
        assertFalse(bf.contains("date"));
        assertFalse(bf.contains("elderberry"));
    }

    @Test
    public void testBloomFilterMerge() {
        BloomFilter<String> bf1 = (BloomFilter<String>) BloomFilter.empty(100, 3, stringHashable).build("apple");
        BloomFilter<String> bf2 = (BloomFilter<String>) BloomFilter.empty(100, 3, stringHashable).build("banana");
        
        BloomFilter<String> merged = BloomFilter.monoid(100, 3, stringHashable).combine(bf1, bf2);
        
        assertTrue(merged.contains("apple"));
        assertTrue(merged.contains("banana"));
        assertFalse(merged.contains("cherry"));
    }

    @Test
    public void testHyperLogLog() {
        HyperLogLog<String> hll = HyperLogLog.empty(14, stringHashable); // 16384 registers
        
        // Add 1000 distinct items
        for (int i = 0; i < 1000; i++) {
            hll = (HyperLogLog<String>) hll.build("item-" + i);
        }
        
        long estimate = hll.estimateCardinality();
        System.out.println("HLL Estimate for 1000 items: " + estimate);
        
        // With 16384 registers, standard error is ~0.8%
        assertTrue(estimate > 950 && estimate < 1050, "Estimate " + estimate + " is outside reasonable bounds");
    }

    @Test
    public void testHyperLogLogMerge() {
        HyperLogLog<String> hll1 = (HyperLogLog<String>) HyperLogLog.empty(10, stringHashable).build("apple");
        HyperLogLog<String> hll2 = (HyperLogLog<String>) HyperLogLog.empty(10, stringHashable).build("banana");
        
        HyperLogLog<String> merged = HyperLogLog.monoid(10, stringHashable).combine(hll1, hll2);
        
        assertTrue(merged.estimateCardinality() >= 2);
    }
}
