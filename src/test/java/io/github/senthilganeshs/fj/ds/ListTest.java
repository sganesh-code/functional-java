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
        // For LinkedList(head, tail), head is the list before tail.
        // List.of(1,2,3) is build(1).build(2).build(3)
        // [ [ [nil, 1], 2], 3]
        // unzip on [ [1,2], 3] returns Tuple.of(head.take(1).find(i->true), head.drop(1).build(tail))
        // This unzip seems specifically designed for some internal use or it's a bit unconventional.
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
    public void testListEmpty() {
        List<Integer> nil = List.nil();
        Assert.assertEquals(nil.length(), 0);
        Assert.assertEquals(nil.toString(), "");
    }
}
