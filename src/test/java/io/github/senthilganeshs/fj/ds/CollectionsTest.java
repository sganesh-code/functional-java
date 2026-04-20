package io.github.senthilganeshs.fj.ds;

import org.testng.Assert;
import org.testng.annotations.Test;


public class CollectionsTest {

    
    @Test
    public void testQueue() throws Exception {
        Queue<Integer> q = Queue.of(1, 2, 3);
        Assert.assertEquals(q.toString(), "[1,2,3]");
        
        Maybe<Tuple<Integer, Queue<Integer>>> d1 = q.dequeue();
        Tuple<Integer, Queue<Integer>> t1 = d1.fromMaybe(null);
        Assert.assertNotNull(t1);
        Assert.assertEquals(t1.getA().fromMaybe(-1), Integer.valueOf(1));
        
        Queue<Integer> q2 = t1.getB().fromMaybe(Queue.nil());
        Assert.assertEquals(q2.toString(), "[2,3]");
        
        Queue<Integer> q3 = (Queue<Integer>) q2.build(4);
        Assert.assertEquals(q3.toString(), "[2,3,4]");
        
        // Test empty queue dequeue
        Assert.assertNull(Queue.nil().dequeue().fromMaybe(null));
        
        // Test single element queue
        Queue<Integer> single = Queue.of(10);
        Maybe<Tuple<Integer, Queue<Integer>>> dSingle = single.dequeue();
        Tuple<Integer, Queue<Integer>> tSingle = dSingle.fromMaybe(null);
        Assert.assertNotNull(tSingle);
        Assert.assertEquals(tSingle.getA().fromMaybe(-1), Integer.valueOf(10));
        Assert.assertEquals(tSingle.getB().fromMaybe(null).length(), 0);
    }

    @Test
    public void testVector() throws Exception {
        Vector<Integer> v = Vector.of(1, 2, 3);
        Assert.assertEquals(v.toString(), "[1,2,3]");
        Assert.assertEquals(v.at(0).fromMaybe(-1), Integer.valueOf(1));
        Assert.assertEquals(v.at(2).fromMaybe(-1), Integer.valueOf(3));
        Assert.assertNull(v.at(3).fromMaybe(null));
        
        Vector<Integer> v2 = v.update(1, 20);
        Assert.assertEquals(v2.toString(), "[1,20,3]");
        Assert.assertEquals(v.toString(), "[1,2,3]"); // Immutability check
        
        // Test large vector to trigger trie
        Vector<Integer> large = Vector.nil();
        for (int i = 0; i < 100; i++) {
            large = (Vector<Integer>) large.build(i);
        }
        Assert.assertEquals(large.length(), 100);
        Assert.assertEquals(large.at(50).fromMaybe(-1), Integer.valueOf(50));
        Assert.assertEquals(large.at(99).fromMaybe(-1), Integer.valueOf(99));
    }

    @Test
    public void testLazyList() throws Exception {
        LazyList<Integer> l = LazyList.of(1, 2, 3);
        Assert.assertEquals(l.toString(), "[1,2,3]");
        
        // Infinite list test
        LazyList<Integer> infinite = LazyList.iterate(1, i -> i + 1);
        Collection<Integer> firstFive = infinite.take(5);
        Assert.assertEquals(firstFive.toString(), "[1,2,3,4,5]");
        
        // Map on infinite list (lazy)
        Collection<Integer> doubled = infinite.map(i -> i * 2).take(3);
        Assert.assertEquals(doubled.toString(), "[2,4,6]");
    }

    @Test
    public void testGenericCollectionAPIs() throws Exception {
        List<Integer> list = List.of(1, 2, 3, 4, 5);
        
        // any / all
        Assert.assertTrue(list.any(i -> i > 4));
        Assert.assertFalse(list.any(i -> i > 5));
        Assert.assertTrue(list.all(i -> i > 0));
        Assert.assertFalse(list.all(i -> i < 5));
        
        // reduce
        Assert.assertEquals(list.reduce(Integer::sum).fromMaybe(-1), Integer.valueOf(15));
        Assert.assertNull(List.<Integer>nil().reduce(Integer::sum).fromMaybe(null));
        
        // mkString
        Assert.assertEquals(list.mkString(","), "1,2,3,4,5");
        Assert.assertEquals(list.mkString("<", "|", ">"), "<1|2|3|4|5>");
    }

    @Test
    public void testGraph() throws Exception {
        Graph<String> g = Graph.<String>nil()
            .addEdge("A", "B")
            .addEdge("A", "C")
            .addEdge("B", "D")
            .addEdge("C", "D");
            
        Assert.assertEquals(g.nodes().length(), 4);
        Assert.assertTrue(g.successors("A").contains("B"));
        Assert.assertTrue(g.successors("A").contains("C"));
        
        // BFS traversal
        List<String> bfsOrder = g.bfs("A");
        Assert.assertTrue(bfsOrder.any(v -> v.equals("A")));
        Assert.assertTrue(bfsOrder.any(v -> v.equals("B")));
        Assert.assertTrue(bfsOrder.any(v -> v.equals("C")));
        Assert.assertTrue(bfsOrder.any(v -> v.equals("D")));
        Assert.assertEquals(bfsOrder.length(), 4);
        
        // DFS traversal
        List<String> dfsOrder = g.dfs("A");
        Assert.assertEquals(dfsOrder.length(), 4);
        
        // Topological Sort
        Maybe<List<String>> topo = g.topologicalSort();
        Assert.assertTrue(topo.isSome());
        // One valid order is A, B, C, D (or A, C, B, D)
        // Given our foldl/Set order, let's verify it starts with A and ends with D
        List<String> sorted = topo.fromMaybe(List.nil());
        String first = ((Maybe<String>) sorted.unzip().getA().flatMap(id -> (Maybe<String>)id)).fromMaybe("");
        Assert.assertEquals(first, "A");
        
        // Cycle detection
        Graph<String> cyclic = g.addEdge("D", "A");
        Assert.assertTrue(cyclic.topologicalSort().isNothing());
    }

    @Test
    public void testPriorityQueue() throws Exception {
        PriorityQueue<Integer> pq = PriorityQueue.of(3, 1, 4, 1, 5);
        Assert.assertEquals(pq.findMin().fromMaybe(-1), Integer.valueOf(1));
        
        PriorityQueue<Integer> pq2 = pq.deleteMin().fromMaybe(PriorityQueue.nil());
        // There were two 1s, so the next min is still 1
        Assert.assertEquals(pq2.findMin().fromMaybe(-1), Integer.valueOf(1));
        
        PriorityQueue<Integer> pq3 = pq2.deleteMin().fromMaybe(PriorityQueue.nil());
        Assert.assertEquals(pq3.findMin().fromMaybe(-1), Integer.valueOf(3));
        
        PriorityQueue<Integer> pq4 = pq3.deleteMin().fromMaybe(PriorityQueue.nil());
        Assert.assertEquals(pq4.findMin().fromMaybe(-1), Integer.valueOf(4));
        
        PriorityQueue<Integer> pq5 = pq4.deleteMin().fromMaybe(PriorityQueue.nil());
        Assert.assertEquals(pq5.findMin().fromMaybe(-1), Integer.valueOf(5));
        
        PriorityQueue<Integer> pq6 = pq5.deleteMin().fromMaybe(PriorityQueue.nil());
        Assert.assertNull(pq6.findMin().fromMaybe(null));
    }

    @Test
    public void testHashMap() throws Exception {
        HashMap<String, Integer> map = HashMap.nil();
        map = map.put("one", 1).put("two", 2).put("three", 3);
        
        Assert.assertEquals(map.get("one").fromMaybe(-1), Integer.valueOf(1));
        Assert.assertEquals(map.get("two").fromMaybe(-1), Integer.valueOf(2));
        Assert.assertEquals(map.get("three").fromMaybe(-1), Integer.valueOf(3));
        Assert.assertNull(map.get("four").fromMaybe(null));
        
        HashMap<String, Integer> map2 = map.remove("two");
        Assert.assertNull(map2.get("two").fromMaybe(null));
        Assert.assertEquals(map.get("two").fromMaybe(-1), Integer.valueOf(2)); // Immutability
        
        // Test update
        HashMap<String, Integer> map3 = map.put("one", 10);
        Assert.assertEquals(map3.get("one").fromMaybe(-1), Integer.valueOf(10));
        Assert.assertEquals(map.get("one").fromMaybe(-1), Integer.valueOf(1));
    }

    @Test
    public void testDeque() throws Exception {
        Deque<Integer> d = Deque.of(1, 2, 3);
        Assert.assertEquals(d.toString(), "[1,2,3]");
        
        Deque<Integer> d2 = d.pushFront(0);
        Assert.assertEquals(d2.toString(), "[0,1,2,3]");
        
        Tuple<Integer, Deque<Integer>> pop1 = d2.popFront().fromMaybe(null);
        Assert.assertEquals(pop1.getA().fromMaybe(-1), Integer.valueOf(0));
        Assert.assertEquals(pop1.getB().fromMaybe(null).toString(), "[1,2,3]");
        
        Tuple<Integer, Deque<Integer>> pop2 = d2.popBack().fromMaybe(null);
        Assert.assertEquals(pop2.getA().fromMaybe(-1), Integer.valueOf(3));
        Assert.assertEquals(pop2.getB().fromMaybe(null).toString(), "[0,1,2]");
    }

    @Test
    public void testRoseTree() throws Exception {
        RoseTree<String> tree = RoseTree.of("root", 
            List.of(
                RoseTree.of("child1"),
                RoseTree.of("child2", List.of(RoseTree.of("grandchild1")))
            )
        );
        
        // Test foldl (pre-order: root, child1, child2, grandchild1)
        String traversal = tree.foldl("", (r, t) -> r + (r.isEmpty() ? "" : ",") + t);
        Assert.assertEquals(traversal, "root,child1,child2,grandchild1");
        
        // Test map
        RoseTree<Integer> lengths = (RoseTree<Integer>) tree.map(String::length);
        Assert.assertEquals(lengths.value(), Integer.valueOf(4));
        
        String lengthsTraversal = lengths.foldl("", (r, t) -> r + (r.isEmpty() ? "" : ",") + t);
        Assert.assertEquals(lengthsTraversal, "4,6,6,11");
    }

    @Test
    public void testMap() throws Exception {
        List.of(10).map(i -> i + 10).forEach(i -> Assert.assertTrue(i == 20));
        Maybe.some(10).map(i -> i + 10).forEach(i -> Assert.assertTrue(i == 20));
        Either.right(10).map(i -> i + 10).forEach(i -> Assert.assertTrue(i == 20));
        Set.of(3,1,2,4,5).map(i -> i + 10).equals(List.of(11,12,13,14,15));        
    }
    
    @Test
    public void testFlatMap() throws Exception {
        List.of(10).flatMap(i -> Maybe.some(i + 10)).forEach(i -> Assert.assertTrue(i == 20));
        Maybe.some(10).flatMap(i -> List.of(i + 10)).forEach(i -> Assert.assertTrue(i == 20));
        Either.right(10).flatMap(i -> Maybe.some(i + 10)).forEach(i -> Assert.assertTrue(i == 20));
        Set.of(3,1,2,4,5).flatMap(i -> Maybe.some(i + 10)).equals(List.of(11,12,13,14,15));
    }
    
    @Test
    public void testApply() throws Exception {
        Assert.assertEquals(
            (int)List.of(1,2,3)
            .apply(
                Maybe.some((Integer i) -> i - 1))
            .foldl(0, (r, i) -> r + (Integer) i), 
            2);
        
        Assert.assertEquals(
            (int)Maybe.some(3)
            .apply(
                List.of(i -> i - 1, i -> i - 2))
            .foldl(0, (r, i) -> r + (Integer) i), //sum to be 1 + 2 = 3
            3);
        
        Assert.assertEquals(
            (int)Either.right(3)
            .apply(
                List.of(i -> i - 1, i -> i - 2))
            .foldl(0, (r, i) -> r + (Integer) i), //sum to be 1 + 2 = 3
            3);
        
       Assert.assertEquals(
            Set.of(3,1,2,5,4).apply(Maybe.some(i -> i - 1)), 
            Maybe.some(4));
    }
    
    @Test
    public void testFilter() throws Exception {
        Assert.assertEquals(
            List.of(10,20,30).filter(i -> i == 10).toString(), List.of(10).toString());
        
        Assert.assertEquals(
            Maybe.some(10)
            .filter(i -> i == 20).toString(),
            Maybe.nothing().toString());
        
        Assert.assertEquals(
            Either.right(10)
            .filter(i -> i == 20).toString(),
            Either.left(10).toString());
        
        Assert.assertEquals(
            Set.of(3,1,2,5,4)
            .filter(i -> i == 2),
            Set.of(2));
        
    }
    
    @Test
    public void testTraverse() throws Exception {
        Assert.assertEquals(
            (int) List.of(1,1)
            .traverse(i -> List.of('a', 'b', 'c'))
            .flatMap(id -> id)
            .count(),
            (int) Math.pow(3, 2) * 2);
        
        Assert.assertEquals(
            (int) Maybe.some(1)
            .traverse(i -> List.of('a', 'b', 'c'))
            .flatMap(id -> id)
            .count(),
            3);        
        
        Assert.assertEquals(
            (int) List.of('a', 'b', 'c')
            .traverse(i -> Either.right(1))
            .flatMap(id -> id)
            .count(),
            1);
        
        Assert.assertEquals(
            Set.of(3,1,2,5,4)
            .traverse(i -> Maybe.some(i))
            .flatMap(id -> id),
            Maybe.some(5));
    }
    
    @Test
    public void testSequence() throws Exception {
        Assert.assertEquals(
            Collection.sequence(
                List.of(Maybe.some(1), Maybe.some(2))),
            Maybe.some(List.of(1, 2)));
        
        Assert.assertEquals(
            Collection.sequence(
                List.of(Either.right(1), Either.left(2))),
            Either.left(2));                       
    }
    
    @Test
    public void testEither() throws Exception {
        Assert.assertEquals(
            Either.rights(
                List.of(Either.right(10), Either.left(20), Either.right(30))),
            List.of(10,30));
        
        Assert.assertEquals(
            Either.lefts(
                List.of(Either.right(10), Either.left(20), Either.right(30))),
            List.of(20));
        
        Assert.assertTrue(Either.right(10).either(x -> false, b -> b == 10));
        Assert.assertTrue(Either.left(10).either(x -> x == 10, b -> false));
        Assert.assertEquals((int) Either.left(10).fromLeft(11), 10);
        Assert.assertEquals((int) Either.left(10).fromRight(11), 11);
        Assert.assertEquals(Either.right(10).fromLeft(11), 11);
        Assert.assertEquals((int) Either.right(10).fromRight(11), 10);
        
    }

    
    @Test
    public void testList() throws Exception {
        Assert.assertEquals(
            Set.sort(List.of(3,1,2,5,4)),
            List.of(1,2,3,4,5));
        
        Assert.assertEquals(
            List.of(3,1,2,5,4).length(), 5);
        
        Assert.assertEquals(
            List.of(1,2,3,4,5).drop(2), 
            List.of(3,4,5));
        
        Assert.assertEquals(
            List.of(1,2,3,4,5).take(2), 
            List.of(1,2));
        
        Assert.assertEquals(
            List.of(1,2,3,4,5).reverse(),
            List.of(5,4,3,2,1));
        
        Assert.assertEquals(
            List.of('a','b','c','d','e')
            .intersperse(','), 
            List.of('a',',','b',',','c',',','d',',','e'));
        
        Assert.assertEquals(
                List.of(',')
                .intercalate(List.of(List.of('a','b'),List.of('c','d','e'))), 
            List.of(List.of('a','b'),List.of(','),List.of('c','d','e')));
        
    }
    
    @Test
    public void testConcat() throws Exception {
        Assert.assertEquals(
            Maybe.some(5).concat(List.of(1,2,3,4)), 
            Maybe.some(4));

        Assert.assertEquals(
            List.of(1,2,3,4).concat(Maybe.some(5)), 
            List.of(1,2,3,4,5));

    }
    
    @Test
    public void testBinarytree() throws Exception {
        Assert.assertEquals(Set.of(1,2,3).compareTo(2), 0);//left rotation.
        Assert.assertEquals(Set.of(3,1,2).compareTo(2), 0);//left-right rotation
        Assert.assertEquals(Set.of(1,3,2).compareTo(2), 0);//right-left rotation
        Assert.assertEquals(Set.of(3,2,1).compareTo(2), 0);//right rotation
        
        Assert.assertTrue(Set.of(1,2,3,4,5,6,7,8,9).contains(5));
        Assert.assertFalse(Set.of(1,2,3,4,5,6,7,8).contains(9));                
    }
}
