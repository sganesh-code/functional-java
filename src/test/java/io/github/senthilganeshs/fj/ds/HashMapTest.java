package io.github.senthilganeshs.fj.ds;

import org.testng.Assert;
import org.testng.annotations.Test;

public class HashMapTest {

    @Test
    public void testHashMapBasic() {
        HashMap<String, Integer> map = HashMap.nil();
        map = map.put("a", 1).put("b", 2).put("c", 3);
        
        Assert.assertEquals(map.get("a").fromMaybe(-1), Integer.valueOf(1));
        Assert.assertEquals(map.get("b").fromMaybe(-1), Integer.valueOf(2));
        Assert.assertEquals(map.get("c").fromMaybe(-1), Integer.valueOf(3));
        Assert.assertEquals(map.length(), 3);
    }

    @Test
    public void testHashMapUpdate() {
        HashMap<String, Integer> map = HashMap.<String, Integer>nil().put("a", 1);
        map = map.put("a", 10);
        
        Assert.assertEquals(map.get("a").fromMaybe(-1), Integer.valueOf(10));
        Assert.assertEquals(map.length(), 1);
    }

    @Test
    public void testHashMapRemove() {
        HashMap<String, Integer> map = HashMap.<String, Integer>nil().put("a", 1).put("b", 2);
        HashMap<String, Integer> removed = map.remove("a");
        
        Assert.assertTrue(removed.get("a").isNothing());
        Assert.assertEquals(removed.get("b").fromMaybe(-1), Integer.valueOf(2));
        Assert.assertEquals(removed.length(), 1);
        
        // Remove non-existent
        HashMap<String, Integer> removed2 = map.remove("c");
        Assert.assertEquals(removed2.length(), 2);
    }

    @Test
    public void testHashMapPersistence() {
        HashMap<String, Integer> m1 = HashMap.<String, Integer>nil().put("a", 1);
        HashMap<String, Integer> m2 = m1.put("b", 2);
        
        Assert.assertTrue(m1.get("b").isNothing());
        Assert.assertTrue(m2.get("b").isSome());
    }

    @Test
    public void testHashMapLarge() {
        HashMap<Integer, String> map = HashMap.nil();
        int limit = 1000;
        for (int i = 0; i < limit; i++) {
            map = map.put(i, "val" + i);
        }
        
        Assert.assertEquals(map.length(), limit);
        for (int i = 0; i < limit; i++) {
            Assert.assertEquals(map.get(i).fromMaybe(""), "val" + i);
        }
        
        // Remove some
        for (int i = 0; i < limit; i += 2) {
            map = map.remove(i);
        }
        Assert.assertEquals(map.length(), limit / 2);
        for (int i = 1; i < limit; i += 2) {
            Assert.assertEquals(map.get(i).fromMaybe(""), "val" + i);
        }
    }

    @Test
    public void testHashMapCollision() {
        // Create keys with same hash code
        class CollisionKey {
            final int id;
            CollisionKey(int id) { this.id = id; }
            @Override public int hashCode() { return 42; }
            @Override public boolean equals(Object obj) {
                return obj instanceof CollisionKey && ((CollisionKey)obj).id == id;
            }
        }
        
        HashMap<CollisionKey, String> map = HashMap.nil();
        CollisionKey k1 = new CollisionKey(1);
        CollisionKey k2 = new CollisionKey(2);
        
        map = map.put(k1, "v1").put(k2, "v2");
        
        Assert.assertEquals(map.get(k1).fromMaybe(""), "v1");
        Assert.assertEquals(map.get(k2).fromMaybe(""), "v2");
        Assert.assertEquals(map.length(), 2);
        
        map = map.remove(k1);
        Assert.assertTrue(map.get(k1).isNothing());
        Assert.assertEquals(map.get(k2).fromMaybe(""), "v2");
    }

    @Test
    public void testHashMapDeepTrieRemoval() {
        HashMap<Integer, String> map = HashMap.nil();
        // Add enough to create many levels of IndexedNodes
        int limit = 100; 
        for (int i = 0; i < limit; i++) {
            map = map.put(i, "v" + i);
        }
        
        // Remove one by one and verify
        for (int i = 0; i < limit; i++) {
            map = map.remove(i);
            Assert.assertTrue(map.get(i).isNothing());
            Assert.assertEquals(map.length(), limit - i - 1);
        }
    }

    @Test
    public void testHashMapCollisionRemoval() {
        class CollisionKey {
            final int id;
            final int h;
            CollisionKey(int id, int h) { this.id = id; this.h = h; }
            @Override public int hashCode() { return h; }
            @Override public boolean equals(Object obj) {
                return obj instanceof CollisionKey && ((CollisionKey)obj).id == id;
            }
        }
        
        int commonHash = 42;
        HashMap<CollisionKey, String> map = HashMap.nil();
        CollisionKey k1 = new CollisionKey(1, commonHash);
        CollisionKey k2 = new CollisionKey(2, commonHash);
        CollisionKey k3 = new CollisionKey(3, commonHash);
        
        map = map.put(k1, "v1").put(k2, "v2").put(k3, "v3");
        
        // Remove from collision
        map = map.remove(k2);
        Assert.assertEquals(map.length(), 2);
        Assert.assertEquals(map.get(k1).fromMaybe(""), "v1");
        Assert.assertEquals(map.get(k3).fromMaybe(""), "v3");
        
        // Remove until 1 left (should convert back to LeafNode)
        map = map.remove(k1);
        Assert.assertEquals(map.length(), 1);
        Assert.assertEquals(map.get(k3).fromMaybe(""), "v3");
        
        // Final removal
        map = map.remove(k3);
        Assert.assertEquals(map.length(), 0);
    }
}
