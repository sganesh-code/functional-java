package io.github.senthilganeshs.fj.ds;

import org.testng.Assert;
import org.testng.annotations.Test;

public class VectorTest {

    @Test
    public void testVectorBasic() {
        Vector<Integer> v = Vector.of(1, 2, 3);
        Assert.assertEquals(v.at(0).fromMaybe(-1), Integer.valueOf(1));
        Assert.assertEquals(v.at(2).fromMaybe(-1), Integer.valueOf(3));
        Assert.assertTrue(v.at(3).isNothing());
    }

    @Test
    public void testVectorUpdate() {
        Vector<Integer> v = Vector.of(1, 2, 3);
        Vector<Integer> v2 = v.update(1, 20);
        
        Assert.assertEquals(v2.at(1).fromMaybe(-1), Integer.valueOf(20));
        Assert.assertEquals(v.at(1).fromMaybe(-1), Integer.valueOf(2)); // Persistence
    }

    @Test
    public void testVectorLarge() {
        Vector<Integer> v = Vector.nil();
        int limit = 2000;
        for (int i = 0; i < limit; i++) {
            v = (Vector<Integer>) v.build(i);
        }
        
        Assert.assertEquals(v.length(), limit);
        for (int i = 0; i < limit; i++) {
            Assert.assertEquals(v.at(i).fromMaybe(-1), Integer.valueOf(i));
        }
        
        // Update in large vector
        v = v.update(1000, 9999);
        Assert.assertEquals(v.at(1000).fromMaybe(-1), Integer.valueOf(9999));
        Assert.assertEquals(v.at(999).fromMaybe(-1), Integer.valueOf(999));
    }

    @Test
    public void testVectorEdgeCases() {
        Vector<Integer> empty = Vector.nil();
        Assert.assertTrue(empty.at(0).isNothing());
        Assert.assertEquals(empty.update(0, 10).length(), 0); // update OOB returns this
        
        Vector<Integer> v = Vector.of(1);
        Assert.assertTrue(v.at(-1).isNothing());
        Assert.assertTrue(v.at(1).isNothing());
    }

    @Test
    public void testVectorDeepTrie() {
        Vector<Integer> v = Vector.nil();
        // Trigger multi-level trie (WIDTH=32)
        int limit = 1100; // > 32*32
        for (int i = 0; i < limit; i++) v = (Vector<Integer>) v.build(i);
        
        Assert.assertEquals(v.length(), limit);
        // Deep update
        v = v.update(500, -500);
        Assert.assertEquals(v.at(500).fromMaybe(0), Integer.valueOf(-500));
        
        // Out of bound updates
        Assert.assertEquals(v.update(-1, 0).length(), limit);
        Assert.assertEquals(v.update(limit, 0).length(), limit);
    }
}
