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
    public void testStackStatic() {
        Stack<Integer> s = Stack.newStack(new Integer[]{1, 2, 3});
        Assert.assertEquals(s.length(), 3);
    }

    @Test
    public void testStackFunctionalAPIs() {
        Stack<Integer> s = Stack.newStack(new Integer[]{1, 2, 3, 4});
        
        // map (covariant). [1,2,3,4] mapped and built sequentially: push 2, then 4, then 6, then 8. Top is 8.
        // toString() uses foldl, which starts from top. So [8,6,4,2]
        Stack<Integer> doubled = s.map(i -> i * 2);
        Assert.assertEquals(doubled.toString(), "[8,6,4,2]");
        
        // filter (covariant). [1,2,3,4] filtered: push 2, then 4. Top is 4.
        Stack<Integer> filtered = s.filter(i -> i % 2 == 0);
        Assert.assertEquals(filtered.toString(), "[4,2]");
    }
}
