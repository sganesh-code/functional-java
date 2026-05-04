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
        List<Integer> l = List.nil();
        l = (List<Integer>) l.build(1).build(2).build(3);
        Assert.assertEquals(l.toString(), "[1,2,3]");
    }

    @Test
    public void testListUnzipReal() {
        List<Tuple<Integer, String>> l = List.of(Tuple.of(1, "a"), Tuple.of(2, "b"));
        Tuple<Collection<Integer>, Collection<String>> unzipped = l.unzip();
        
        Assert.assertEquals(List.from(unzipped.getA().orElse(List.nil())).toString(), "[1,2]");
        Assert.assertEquals(List.from(unzipped.getB().orElse(List.nil())).toString(), "[a,b]");
    }

    @Test
    public void testListCollectionAPIs() {
        List<Integer> l = List.of(1, 2, 3, 4, 5);
        Assert.assertEquals(l.filter(i -> i % 2 == 0).toString(), "[2,4]");
        Assert.assertEquals(l.map(i -> i * 10).toString(), "[10,20,30,40,50]");
    }
}
