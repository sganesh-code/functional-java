package io.github.senthilganeshs.fj.ds;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ListTest {

    @Test
    public void testListBasic() {
        List<Integer> l = List.of(1, 2, 3);
        Assert.assertEquals(l.length(), 3);
        Assert.assertEquals(l.toString(), "[1,2,3]");
    }

    @Test
    public void testListCons() {
        List<Integer> l = List.cons(List.of(1, 2), 3);
        Assert.assertEquals(l.toString(), "[1,2,3]");
    }

    @Test
    public void testListZipUnzip() {
        List<Integer> l1 = List.of(1, 2, 3);
        List<String> l2 = List.of("a", "b", "c");
        
        List<Tuple<Integer, String>> zipped = l1.zip(l2);
        Assert.assertEquals(zipped.length(), 3);
    }
    
    @Test
    public void testListUnzipReal() {
        List<Integer> l = List.of(1, 2, 3);
        Tuple<Maybe<Integer>, List<Integer>> unzipped = l.unzip();
        
        // head is 1, tail is [2, 3]
        Assert.assertEquals(unzipped.getA().fromMaybe(Maybe.nothing()).fromMaybe(-1), Integer.valueOf(1));
        Assert.assertEquals(unzipped.getB().fromMaybe(List.nil()).toString(), "[2,3]");
    }

    @Test
    public void testListFrom() {
        Collection<Integer> c = Maybe.some(10);
        List<Integer> l = List.from(c);
        Assert.assertEquals(l.toString(), "[10]");
        
        Collection<Integer> c2 = List.of(1, 2, 3);
        Assert.assertEquals(List.from(c2).length(), 3);
    }

    @Test
    public void testListCollectionAPIs() {
        List<Integer> l = List.of(1, 2, 3, 4, 5);
        Assert.assertEquals(l.take(2).toString(), "[1,2]");
        Assert.assertEquals(l.drop(2).toString(), "[3,4,5]");
        Assert.assertEquals(l.reverse().toString(), "[5,4,3,2,1]");
        Assert.assertEquals(l.intersperse(0).toString(), "[1,0,2,0,3,0,4,0,5]");
    }

    @Test
    public void testListEquals() {
        List<Integer> l1 = List.of(1, 2);
        List<Integer> l2 = List.of(1, 2);
        List<Integer> l3 = List.of(1);
        
        Assert.assertEquals(l1, l1);
        Assert.assertEquals(l1, l2);
        Assert.assertNotEquals(l1, l3);
        Assert.assertNotEquals(l1, null);
    }

    @Test
    public void testListStaticHelpers() {
        List<Integer> l = List.of(java.util.Arrays.asList(1, 2, 3));
        Assert.assertEquals(l.length(), 3);
        
        Collection<Integer> q = List.newQueue(new Integer[]{1, 2});
        Assert.assertEquals(q.length(), 2);
    }
}
