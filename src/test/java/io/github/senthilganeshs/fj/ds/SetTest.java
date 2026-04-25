package io.github.senthilganeshs.fj.ds;

import org.testng.Assert;
import org.testng.annotations.Test;

public class SetTest {

    @Test
    public void testSetBasic() {
        Set<Integer> s = Set.of(3, 1, 2);
        Assert.assertEquals(s.length(), 3);
        Assert.assertTrue(s.contains(1));
        Assert.assertTrue(s.contains(2));
        Assert.assertTrue(s.contains(3));
        Assert.assertFalse(s.contains(4));
    }

    @Test
    public void testSetDuplicates() {
        Set<Integer> s = Set.of(1, 1, 1);
        Assert.assertEquals(s.length(), 1);
    }

    @Test
    public void testSetBalance() {
        // AVL Tree should balance
        Set<Integer> s = Set.of(1, 2, 3, 4, 5, 6, 7);
        Assert.assertEquals(s.length(), 7);
        for (int i = 1; i <= 7; i++) {
            Assert.assertTrue(s.contains(i));
        }
    }

    @Test
    public void testSetSort() {
        List<Integer> list = List.of(5, 2, 4, 1, 3);
        Collection<Integer> sorted = Set.sort(list);
        Assert.assertEquals(sorted.toString(), "[1,2,3,4,5]");
    }

    @Test
    public void testSetEmpty() {
        Set<Integer> empty = Set.emptyNatural();
        Assert.assertEquals(empty.length(), 0);
        Assert.assertFalse(empty.contains(1));
    }

    @Test
    public void testSetBalanceExhaustive() {
        // Simple Left (1-2-3)
        Set<Integer> s1 = Set.of(1, 2, 3);
        // Simple Right (3-2-1)
        Set<Integer> s2 = Set.of(3, 2, 1);
        // Left-Right (3-1-2)
        Set<Integer> s3 = Set.of(3, 1, 2);
        // Right-Left (1-3-2)
        Set<Integer> s4 = Set.of(1, 3, 2);
        
        Assert.assertTrue(s1.contains(2));
        Assert.assertTrue(s2.contains(2));
        Assert.assertTrue(s3.contains(2));
        Assert.assertTrue(s4.contains(2));

        // Deeper balancing
        Set<Integer> s5 = Set.of(10, 20, 30, 40, 50, 25);
        Assert.assertEquals(s5.length(), 6);
    }

    @Test
    public void testSetAlgebra() {
        Set<Integer> s1 = Set.of(1, 2, 3);
        Set<Integer> s2 = Set.of(3, 4, 5);
        
        Assert.assertEquals(s1.union(s2).length(), 5);
        Assert.assertEquals(s1.intersect(s2).length(), 1);
        Assert.assertTrue(s1.intersect(s2).contains(3));
        
        Assert.assertEquals(s1.difference(s2).length(), 2);
        Assert.assertTrue(s1.difference(s2).contains(1));
        Assert.assertFalse(s1.difference(s2).contains(3));
    }
}
