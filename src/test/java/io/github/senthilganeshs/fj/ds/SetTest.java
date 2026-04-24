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
        Set<Integer> empty = Set.empty();
        Assert.assertEquals(empty.length(), 0);
        Assert.assertFalse(empty.contains(1));
    }

    @Test
    public void testSetBalanceExhaustive() {
        // Simple Left
        Set<Integer> s1 = Set.of(1, 2, 3);
        // Simple Right
        Set<Integer> s2 = Set.of(3, 2, 1);
        // Left-Right
        Set<Integer> s3 = Set.of(3, 1, 2);
        // Right-Left
        Set<Integer> s4 = Set.of(1, 3, 2);
        
        Assert.assertTrue(s1.contains(2));
        Assert.assertTrue(s2.contains(2));
        Assert.assertTrue(s3.contains(2));
        Assert.assertTrue(s4.contains(2));
    }
}
