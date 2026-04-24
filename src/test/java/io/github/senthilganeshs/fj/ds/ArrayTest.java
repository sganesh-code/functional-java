package io.github.senthilganeshs.fj.ds;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ArrayTest {

    @Test
    public void testArrayBasic() {
        Array<Integer> arr = new Array.NonEmpty<>(5);
        arr.build(1).build(2).build(3);
        
        Assert.assertEquals(arr.length(), 3);
        Assert.assertEquals(arr.at(0).fromMaybe(-1), Integer.valueOf(1));
        Assert.assertEquals(arr.at(1).fromMaybe(-1), Integer.valueOf(2));
        Assert.assertEquals(arr.at(2).fromMaybe(-1), Integer.valueOf(3));
        Assert.assertEquals(arr.at(3), Maybe.nothing());
    }

    @Test
    public void testArrayRemove() {
        Array<Integer> arr = new Array.NonEmpty<>(5);
        arr.build(1).build(2).build(3);
        
        Maybe<Integer> removed = arr.remove(1);
        Assert.assertEquals(removed.fromMaybe(-1), Integer.valueOf(2));
        Assert.assertEquals(arr.length(), 2);
        Assert.assertEquals(arr.at(0).fromMaybe(-1), Integer.valueOf(1));
        Assert.assertEquals(arr.at(1).fromMaybe(-1), Integer.valueOf(3));
    }

    @Test
    public void testArrayShift() {
        Array<Integer> arr = new Array.NonEmpty<>(5);
        arr.build(1).build(2).build(3);
        
        arr.shift();
        Assert.assertEquals(arr.length(), 2);
        Assert.assertEquals(arr.at(0).fromMaybe(-1), Integer.valueOf(2));
        Assert.assertEquals(arr.at(1).fromMaybe(-1), Integer.valueOf(3));
        
        arr.shift();
        Assert.assertEquals(arr.length(), 1);
        Assert.assertEquals(arr.at(0).fromMaybe(-1), Integer.valueOf(3));
        
        arr.shift();
        Assert.assertEquals(arr.length(), 0);
    }

    @Test
    public void testArrayEdgeCases() {
        Array<Integer> empty = new Array.NonEmpty<>(2);
        Assert.assertEquals(empty.length(), 0);
        Assert.assertEquals(empty.at(0), Maybe.nothing());
        Assert.assertEquals(empty.remove(0), Maybe.nothing());
        Assert.assertEquals(empty.shift().length(), 0);
        
        // Out of bounds
        Array<Integer> arr = new Array.NonEmpty<>(2);
        arr.build(1);
        Assert.assertEquals(arr.at(-1), Maybe.nothing());
        Assert.assertEquals(arr.at(5), Maybe.nothing());
        Assert.assertEquals(arr.remove(-1), Maybe.nothing());
        Assert.assertEquals(arr.remove(5), Maybe.nothing());
    }

    @Test
    public void testArrayResizing() {
        Array<Integer> arr = new Array.NonEmpty<>(2);
        for (int i = 0; i < 10; i++) {
            arr.build(i);
        }
        Assert.assertEquals(arr.length(), 10);
        for (int i = 0; i < 10; i++) {
            Assert.assertEquals(arr.at(i).fromMaybe(-1), Integer.valueOf(i));
        }
        
        // Shrinking
        for (int i = 0; i < 8; i++) {
            arr.remove(0);
        }
        Assert.assertEquals(arr.length(), 2);
    }

    @Test
    public void testArrayCollectionAPIs() {
        Array<Integer> arr = new Array.NonEmpty<>(5);
        arr.build(1).build(2).build(3);
        
        Collection<Integer> doubled = arr.map(i -> i * 2);
        Assert.assertEquals(doubled.length(), 3);
        // Note: Array.map returns a Collection which might be a different type 
        // depending on implementation. In Array.java, it uses empty().build(...)
        // which returns an Array.NonEmpty.
        Assert.assertEquals(doubled.foldl(0, (acc, i) -> acc + i), Integer.valueOf(12));
        
        Collection<Integer> filtered = arr.filter(i -> i > 1);
        Assert.assertEquals(filtered.length(), 2);
        Assert.assertEquals(filtered.foldl(0, (acc, i) -> acc + i), Integer.valueOf(5));
    }
}
