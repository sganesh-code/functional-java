package io.github.senthilganeshs.fj.ds;

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
        
        Tuple<Integer, Deque<Integer>> pop1 = d.popFront().fromMaybe(null);
        Assert.assertEquals(pop1.getA().fromMaybe(-1), Integer.valueOf(1));
        Assert.assertEquals(pop1.getB().fromMaybe(null).toString(), "[2,3]");
    }

    @Test
    public void testDequePushPopBack() {
        Deque<Integer> d = Deque.nil();
        d = d.pushBack(1).pushBack(2).pushBack(3);
        Assert.assertEquals(d.toString(), "[1,2,3]");
        
        Tuple<Integer, Deque<Integer>> pop1 = d.popBack().fromMaybe(null);
        Assert.assertEquals(pop1.getA().fromMaybe(-1), Integer.valueOf(3));
        Assert.assertEquals(pop1.getB().fromMaybe(null).toString(), "[1,2]");
    }

    @Test
    public void testDequeMixedPushPop() {
        Deque<Integer> d = Deque.nil();
        d = d.pushFront(2).pushBack(3).pushFront(1);
        Assert.assertEquals(d.toString(), "[1,2,3]");
        
        Tuple<Integer, Deque<Integer>> popBack = d.popBack().fromMaybe(null);
        Assert.assertEquals(popBack.getA().fromMaybe(-1), Integer.valueOf(3));
        
        Tuple<Integer, Deque<Integer>> popFront = d.popFront().fromMaybe(null);
        Assert.assertEquals(popFront.getA().fromMaybe(-1), Integer.valueOf(1));
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
    public void testDequeBalance() {
        // BankersDeque shifts elements between stacks when one becomes empty
        Deque<Integer> d = Deque.nil();
        d = d.pushBack(1).pushBack(2).pushBack(3).pushBack(4);
        
        // At this point, front is empty, back has [4,3,2,1]
        // popFront should trigger a balance
        Tuple<Integer, Deque<Integer>> pop1 = d.popFront().fromMaybe(null);
        Assert.assertEquals(pop1.getA().fromMaybe(-1), Integer.valueOf(1));
        Assert.assertEquals(pop1.getB().fromMaybe(null).toString(), "[2,3,4]");
        
        // Similarly for popBack if rear is empty
        Deque<Integer> d2 = Deque.<Integer>nil().pushFront(4).pushFront(3).pushFront(2).pushFront(1);
        Tuple<Integer, Deque<Integer>> pop2 = d2.popBack().fromMaybe(null);
        Assert.assertEquals(pop2.getA().fromMaybe(-1), Integer.valueOf(4));
        Assert.assertEquals(pop2.getB().fromMaybe(null).toString(), "[1,2,3]");
    }

    @Test
    public void testDequeCollectionAPIs() {
        Deque<Integer> d = Deque.of(1, 2, 3, 4);
        Assert.assertEquals(d.map(i -> i * 2).toString(), "[2,4,6,8]");
        Assert.assertEquals(d.filter(i -> i % 2 == 0).toString(), "[2,4]");
        Assert.assertEquals(d.reverse().toString(), "[4,3,2,1]");
        Assert.assertEquals(d.foldl(0, Integer::sum), Integer.valueOf(10));
    }
}
