package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.typeclass.*;

import org.testng.Assert;
import org.testng.annotations.Test;

public class PriorityQueueTest {

    @Test
    public void testPriorityQueueBasic() {
        PriorityQueue<Integer> pq = PriorityQueue.of(3, 1, 4, 1, 5);
        Assert.assertEquals(pq.findMin().orElse(-1), Integer.valueOf(1));
        Assert.assertEquals(pq.length(), 5);
    }

    @Test
    public void testPriorityQueueDeleteMin() {
        PriorityQueue<Integer> pq = PriorityQueue.of(3, 1, 4, 5);
        
        pq = pq.deleteMin().orElse(PriorityQueue.nil());
        Assert.assertEquals(pq.findMin().orElse(-1), Integer.valueOf(3));
        
        pq = pq.deleteMin().orElse(PriorityQueue.nil());
        Assert.assertEquals(pq.findMin().orElse(-1), Integer.valueOf(4));
        
        pq = pq.deleteMin().orElse(PriorityQueue.nil());
        Assert.assertEquals(pq.findMin().orElse(-1), Integer.valueOf(5));
        
        pq = pq.deleteMin().orElse(PriorityQueue.nil());
        Assert.assertTrue(pq.findMin().isNothing());
    }

    @Test
    public void testPriorityQueueMerge() {
        PriorityQueue<Integer> pq1 = PriorityQueue.of(1, 10, 5);
        PriorityQueue<Integer> pq2 = PriorityQueue.of(2, 8, 3);
        
        PriorityQueue<Integer> merged = pq1.merge(pq2);
        Assert.assertEquals(merged.length(), 6);
        Assert.assertEquals(merged.findMin().orElse(-1), Integer.valueOf(1));
        
        merged = merged.deleteMin().orElse(PriorityQueue.nil());
        Assert.assertEquals(merged.findMin().orElse(-1), Integer.valueOf(2));
        
        merged = merged.deleteMin().orElse(PriorityQueue.nil());
        Assert.assertEquals(merged.findMin().orElse(-1), Integer.valueOf(3));
    }

    @Test
    public void testPriorityQueueMergeEmpty() {
        PriorityQueue<Integer> pq = PriorityQueue.of(1, 2, 3);
        PriorityQueue<Integer> empty = PriorityQueue.nil();
        
        Assert.assertEquals(pq.merge(empty).length(), 3);
        Assert.assertEquals(empty.merge(pq).length(), 3);
        Assert.assertEquals(empty.merge(empty).length(), 0);
    }

    @Test
    public void testPriorityQueueDuplicates() {
        PriorityQueue<Integer> pq = PriorityQueue.of(2, 2, 2);
        Assert.assertEquals(pq.length(), 3);
        Assert.assertEquals(pq.findMin().orElse(-1), Integer.valueOf(2));
        
        pq = pq.deleteMin().orElse(PriorityQueue.nil());
        Assert.assertEquals(pq.length(), 2);
        Assert.assertEquals(pq.findMin().orElse(-1), Integer.valueOf(2));
    }

    @Test
    public void testPriorityQueueEmpty() {
        PriorityQueue<Integer> empty = PriorityQueue.nil();
        Assert.assertTrue(empty.findMin().isNothing());
        Assert.assertTrue(empty.deleteMin().isNothing());
        Assert.assertEquals(empty.length(), 0);
    }

    @Test
    public void testPriorityQueueCollectionAPIs() {
        PriorityQueue<Integer> pq = PriorityQueue.of(10, 20, 30);
        
        // foldl on LeftistHeap is pre-order traversal
        int sum = pq.foldl(0, Integer::sum);
        Assert.assertEquals(sum, 60);
        
        Collection<Integer> mapped = pq.map(i -> i / 10);
        Assert.assertEquals(mapped.length(), 3);
        Assert.assertTrue(mapped.any(i -> i == 1));
    }
}
