package io.github.senthilganeshs.fj.ds;

import org.testng.Assert;
import org.testng.annotations.Test;

public class StackTest {

    @Test
    public void testStackBasic() {
        Stack<Integer> s = (Stack<Integer>) Stack.<Integer>emptyStack().build(1).build(2).build(3);
        // Stack build is push, so it's LIFO. 3 is top.
        Assert.assertEquals(s.head().fromMaybe(-1), Integer.valueOf(3));
        Assert.assertEquals(s.length(), 3);
    }

    @Test
    public void testStackPop() {
        Stack<Integer> s = (Stack<Integer>) Stack.<Integer>emptyStack().build(1).build(2);
        
        Assert.assertEquals(s.head().fromMaybe(-1), Integer.valueOf(2));
        s = s.tail().fromMaybe(Stack.emptyStack());
        
        Assert.assertEquals(s.head().fromMaybe(-1), Integer.valueOf(1));
        s = s.tail().fromMaybe(Stack.emptyStack());
        
        Assert.assertTrue(s.head().isNothing());
    }

    @Test
    public void testStackReverse() {
        Stack<Integer> s = (Stack<Integer>) Stack.<Integer>emptyStack().build(1).build(2).build(3);
        Stack<Integer> reversed = s.reverse();
        
        Assert.assertEquals(reversed.head().fromMaybe(-1), Integer.valueOf(1));
        Assert.assertEquals(reversed.toString(), "[1,2,3]"); // foldl order
    }

    @Test
    public void testStackEmpty() {
        Stack<Integer> empty = Stack.emptyStack();
        Assert.assertTrue(empty.head().isNothing());
        Assert.assertTrue(empty.tail().isNothing());
        Assert.assertEquals(empty.length(), 0);
    }

    @Test
    public void testStackCollectionAPIs() {
        Stack<Integer> s = (Stack<Integer>) Stack.<Integer>emptyStack().build(1).build(2);
        // foldl on [2,1] visits 2 then 1. map(i*2) results in [2,4] (2 is top)
        Assert.assertEquals(s.map(i -> i * 2).find(i -> true).fromMaybe(-1), Integer.valueOf(2));
        Assert.assertEquals(s.filter(i -> i > 1).find(i -> true).fromMaybe(-1), Integer.valueOf(2));
    }
}
