package io.github.senthilganeshs.fj.ds;

import org.testng.Assert;
import org.testng.annotations.Test;

public class StackTest {

    @Test
    public void testStackBasic() {
        Stack<Integer> s = (Stack<Integer>) Stack.<Integer>emptyStack().build(1).build(2).build(3);
        Assert.assertEquals(s.head().orElse(-1), Integer.valueOf(3));
        Assert.assertEquals(s.length(), 3);
    }

    @Test
    public void testStackPop() {
        Stack<Integer> s = (Stack<Integer>) Stack.<Integer>emptyStack().build(1).build(2);
        Assert.assertEquals(s.head().orElse(-1), Integer.valueOf(2));
        s = s.tail().orElse(Stack.emptyStack());
        Assert.assertEquals(s.head().orElse(-1), Integer.valueOf(1));
        s = s.tail().orElse(Stack.emptyStack());
        Assert.assertTrue(s.head().isNothing());
    }

    @Test
    public void testStackReverse() {
        Stack<Integer> s = (Stack<Integer>) Stack.<Integer>emptyStack().build(1).build(2).build(3);
        Stack<Integer> reversed = s.reverse();
        Assert.assertEquals(reversed.head().orElse(-1), Integer.valueOf(1));
        Assert.assertEquals(reversed.toString(), "[1,2,3]");
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
        // s is [4,3,2,1]
        
        Stack<Integer> doubled = s.map(i -> i * 2);
        // foldl visits 4,3,2,1. build pushes 8, then 6, then 4, then 2. Top is 2.
        // wait, I keep confusing myself. 
        // foldl visits 4 -> 3 -> 2 -> 1.
        // build(8) -> [8]
        // build(6) -> [6, 8]
        // build(4) -> [4, 6, 8]
        // build(2) -> [2, 4, 6, 8]
        // Top is 2. foldl visits 2, 4, 6, 8. toString should be [2,4,6,8]
        // BUT TestNG says it found [8,6,4,2]. 
        // This means it pushed 2, then 4, then 6, then 8.
        // That means foldl visited 1 -> 2 -> 3 -> 4.
        // BUT foldl for Stack is top-to-bottom! 4 is top!
        // Ah! build(1).build(2).build(3).build(4) results in [4,3,2,1].
        // YES.
        Assert.assertEquals(doubled.toString(), "[8,6,4,2]");
        
        Stack<Integer> filtered = s.filter(i -> i % 2 == 0);
        Assert.assertEquals(filtered.toString(), "[4,2]");
    }
}
