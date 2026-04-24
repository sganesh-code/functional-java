package io.github.senthilganeshs.fj.ds;

import org.testng.Assert;
import org.testng.annotations.Test;

public class QueueTest {

    @Test
    public void testQueueBasic() {
        Queue<Integer> q = Queue.of(1, 2, 3);
        Assert.assertEquals(q.toString(), "[1,2,3]");
        Assert.assertEquals(q.length(), 3);
    }

    @Test
    public void testQueueDequeue() {
        Queue<Integer> q = Queue.of(1, 2);
        
        Tuple<Integer, Queue<Integer>> d1 = q.dequeue().fromMaybe(null);
        Assert.assertEquals(d1.getA().fromMaybe(-1), Integer.valueOf(1));
        
        Queue<Integer> q2 = d1.getB().fromMaybe(Queue.nil());
        Tuple<Integer, Queue<Integer>> d2 = q2.dequeue().fromMaybe(null);
        Assert.assertEquals(d2.getA().fromMaybe(-1), Integer.valueOf(2));
        
        Assert.assertTrue(d2.getB().fromMaybe(Queue.nil()).dequeue().isNothing());
    }

    @Test
    public void testQueueEmpty() {
        Queue<Integer> empty = Queue.nil();
        Assert.assertTrue(empty.dequeue().isNothing());
        Assert.assertEquals(empty.length(), 0);
    }

    @Test
    public void testQueuePersistence() {
        Queue<Integer> q1 = Queue.of(1);
        Queue<Integer> q2 = (Queue<Integer>) q1.build(2);
        
        Assert.assertEquals(q1.length(), 1);
        Assert.assertEquals(q2.length(), 2);
    }

    @Test
    public void testQueueCollectionAPIs() {
        Queue<Integer> q = Queue.of(1, 2, 3);
        Assert.assertEquals(q.map(i -> i * 2).toString(), "[2,4,6]");
        Assert.assertEquals(q.filter(i -> i > 1).toString(), "[2,3]");
    }
}
