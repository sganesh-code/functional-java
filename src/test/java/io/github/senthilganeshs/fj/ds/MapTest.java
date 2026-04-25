package io.github.senthilganeshs.fj.ds;

import org.testng.Assert;
import org.testng.annotations.Test;

public class MapTest {

    @Test
    public void testMapBasic() {
        Map<String, Integer> map = new Map.BinaryTreeMap<>(Set.emptyNatural());
        map = map.put("a", 1).put("b", 2);
        
        Assert.assertEquals(map.lookup("a").orElse(-1), Integer.valueOf(1));
        Assert.assertEquals(map.lookup("b").orElse(-1), Integer.valueOf(2));
        Assert.assertTrue(map.lookup("c").isNothing());
    }

    @Test
    public void testMapUpdate() {
        Map<String, Integer> map = new Map.BinaryTreeMap<>(Set.emptyNatural());
        map = map.put("a", 1).put("a", 10);
        
        Assert.assertEquals(map.lookup("a").orElse(-1), Integer.valueOf(10));
        Assert.assertEquals(map.keys().length(), 1);
    }

    @Test
    public void testMapKeysValuesEntries() {
        Map<String, Integer> map = new Map.BinaryTreeMap<>(Set.emptyNatural());
        map = map.put("a", 1).put("b", 2);
        
        Assert.assertEquals(map.keys().length(), 2);
        Assert.assertEquals(map.values().length(), 2);
        Assert.assertEquals(map.entries().length(), 2);
        
        Assert.assertTrue(map.keys().any(k -> k.equals("a")));
        Assert.assertTrue(map.values().any(v -> v.equals(2)));
    }
}
