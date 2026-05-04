package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.test.MonoidLaws;
import io.github.senthilganeshs.fj.test.Gen;
import io.github.senthilganeshs.fj.typeclass.Monoid;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.function.Function;

public class CollectionsTest {

    @Test
    public void testQueue() {
        Queue<Integer> q = Queue.of(1, 2, 3);
        Assert.assertEquals(q.toString(), "[1,2,3]");
        
        Tuple<Integer, Queue<Integer>> t1 = q.dequeue().orElse(null);
        Assert.assertEquals(t1.getA().orElse(-1), Integer.valueOf(1));
        
        Queue<Integer> q2 = t1.getB().orElse(Queue.nil());
        Assert.assertEquals(q2.toString(), "[2,3]");
        
        Queue<Integer> q3 = (Queue<Integer>) q2.build(4);
        Assert.assertEquals(q3.toString(), "[2,3,4]");
    }

    @Test
    public void testStack() {
        Stack<Integer> s = Stack.of(1, 2, 3);
        Assert.assertEquals(s.toString(), "[3,2,1]");
        
        Assert.assertEquals(s.head().orElse(-1), Integer.valueOf(3));
        Stack<Integer> s2 = s.tail().orElse(Stack.emptyStack());
        Assert.assertEquals(s2.toString(), "[2,1]");
    }

    @Test
    public void testApply() {
        // List.apply(Maybe) -> List. Sum(0, 1, 2) = 3
        Assert.assertEquals(
            (int)List.of(1,2,3)
            .apply(
                Maybe.some((Integer i) -> i - 1))
            .foldl(0, (r, i) -> r + (Integer) i), 
            3);
        
        // Maybe.apply(List) -> Maybe (keeps last). f2(3) = 1.
        Assert.assertEquals(
            (int)Maybe.some(3)
            .apply(
                List.of(i -> i - 1, i -> i - 2))
            .foldl(0, (r, i) -> r + (Integer) i),
            1);
    }

    @Test
    public void testSequence() {
        List<Maybe<Integer>> list = List.of(Maybe.some(1), Maybe.some(2));
        Maybe<Collection<Integer>> sequenced = (Maybe<Collection<Integer>>) (Maybe) Collection.sequence(list);
        Assert.assertEquals(sequenced.orElse(List.nil()).toString(), "[1,2]");
    }

    @Test
    public void testList() {
        List<Integer> list = List.of(1, 2, 3, 4, 5);
        Assert.assertEquals(list.length(), 5);
        Assert.assertEquals(list.reverse().toString(), "[5,4,3,2,1]");
    }

    @Test
    public void testHashMap() {
        HashMap<String, Integer> map = HashMap.nil();
        map = map.put("one", 1).put("two", 2).put("three", 3);
        Assert.assertEquals(map.length(), 3);
    }

    @Test
    public void testRoseTree() {
        RoseTree<Integer> tree = RoseTree.of(1, List.of(RoseTree.of(2), RoseTree.of(3)));
        Assert.assertEquals(tree.length(), 3);
        Assert.assertEquals(tree.toString(), "RoseTree(1, [RoseTree(2, []), RoseTree(3, [])])");
    }

    @Test
    public void testGraph() {
        Graph<Integer> g = Graph.<Integer>nil().addEdge(1, 2).addEdge(2, 3).addEdge(3, 1);
        Assert.assertEquals(g.vertices().length(), 3);
    }
}
