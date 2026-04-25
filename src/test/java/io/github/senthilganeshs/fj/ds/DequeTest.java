package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.typeclass.*;

import org.testng.Assert;
import org.testng.annotations.Test;

public class DequeTest {

    @Test
    public void testDequeBasic() {
        Deque<Integer> d = Deque.of(1, 2, 3);
        Assert.assertEquals(d.toString(), "[1,2,3]");
        Assert.assertEquals(d.length(), 3);
    }

    @Test
    public void testDequePushPopFront() {
        Deque<Integer> d = Deque.nil();
        d = d.pushFront(3).pushFront(2).pushFront(1);
        Assert.assertEquals(d.toString(), "[1,2,3]");
        
        Tuple<Integer, Deque<Integer>> pop1 = d.popFront().orElse(null);
        Assert.assertEquals(pop1.getA().orElse(-1), Integer.valueOf(1));
        Assert.assertEquals(pop1.getB().orElse(null).toString(), "[2,3]");
    }

    @Test
    public void testDequePushPopBack() {
        Deque<Integer> d = Deque.nil();
        d = d.pushBack(1).pushBack(2).pushBack(3);
        Assert.assertEquals(d.toString(), "[1,2,3]");
        
        Tuple<Integer, Deque<Integer>> pop1 = d.popBack().orElse(null);
        Assert.assertEquals(pop1.getA().orElse(-1), Integer.valueOf(3));
        Assert.assertEquals(pop1.getB().orElse(null).toString(), "[1,2]");
    }

    @Test
    public void testDequeEmptyPop() {
        Deque<Integer> empty = Deque.nil();
        Assert.assertTrue(empty.popFront().isNothing());
        Assert.assertTrue(empty.popBack().isNothing());
    }

    @Test
    public void testDequePersistence() {
        Deque<Integer> d1 = Deque.of(1, 2);
        Deque<Integer> d2 = d1.pushFront(0);
        Deque<Integer> d3 = d1.pushBack(3);
        
        Assert.assertEquals(d1.toString(), "[1,2]");
        Assert.assertEquals(d2.toString(), "[0,1,2]");
        Assert.assertEquals(d3.toString(), "[1,2,3]");
    }

    @Test
    public void testDequeBalanceExhaustive() {
        Deque<Integer> d1 = Deque.<Integer>nil().pushBack(1).pushBack(2).pushBack(3);
        Assert.assertEquals(d1.popFront().orElse(null).getA().orElse(-1), Integer.valueOf(1));
        
        Deque<Integer> d2 = Deque.<Integer>nil().pushFront(1).pushFront(2).pushFront(3);
        Assert.assertEquals(d2.popBack().orElse(null).getA().orElse(-1), Integer.valueOf(1));
    }

    @Test
    public void testDequeFunctionalAPIs() {
        Deque<Integer> d = Deque.of(1, 2, 3, 4);
        Assert.assertEquals(d.map(i -> i * 2).toString(), "[2,4,6,8]");
        Assert.assertEquals(d.filter(i -> i % 2 == 0).toString(), "[2,4]");
    }
}
