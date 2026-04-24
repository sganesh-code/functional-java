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
    public void testSetCollectionAPIs() {
        Set<Integer> s = Set.of(1, 2, 3);
        Collection<Integer> mapped = s.map(i -> i * 2);
        Assert.assertEquals(mapped.length(), 3);
        
        Collection<Integer> filtered = s.filter(i -> i > 1);
        Assert.assertEquals(filtered.length(), 2);
        Assert.assertTrue(filtered.any(i -> i == 2));
        Assert.assertFalse(filtered.any(i -> i == 1));
    }
}
