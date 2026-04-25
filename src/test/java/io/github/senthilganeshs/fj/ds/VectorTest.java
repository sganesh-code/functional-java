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
    public void testVectorFunctionalAPIs() {
        Vector<Integer> v = Vector.of(1, 2, 3, 4);
        
        // map (covariant)
        Vector<Integer> doubled = v.map(i -> i * 2);
        Assert.assertEquals(doubled.toString(), "[2,4,6,8]");
        
        // filter (covariant)
        Vector<Integer> filtered = v.filter(i -> i % 2 == 0);
        Assert.assertEquals(filtered.toString(), "[2,4]");
        
        // takeWhile/dropWhile
        Assert.assertEquals(v.takeWhile(i -> i < 3).toString(), "[1,2]");
        Assert.assertEquals(v.dropWhile(i -> i < 3).toString(), "[3,4]");
        
        // head/last
        Assert.assertEquals(v.headMaybe().fromMaybe(-1), Integer.valueOf(1));
        Assert.assertEquals(v.lastMaybe().fromMaybe(-1), Integer.valueOf(4));
        
        // distinct
        Vector<Integer> dupes = Vector.of(1, 1, 2, 2);
        Assert.assertEquals(dupes.distinct().length(), 2);
    }
}
