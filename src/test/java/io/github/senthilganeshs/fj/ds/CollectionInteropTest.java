package io.github.senthilganeshs.fj.ds;

import org.testng.Assert;
import org.testng.annotations.Test;

public class CollectionInteropTest {

    @Test
    public void testCollectionPipelineInterop() {
        // Start with a List
        Collection<Integer> c = List.of(1, 2, 3);
        
        // Map it (returns generic Collection)
        c = c.map(i -> i * 10);
        
        // Concat with a Stack (returns generic Collection)
        c = c.concat(Stack.of(40, 50));
        
        // Filter it
        c = c.filter(i -> i > 15);
        
        // c should be [20, 30, 50, 40] (List part 20, 30; Stack part 50, 40)
        // Wait, Stack.of(40, 50) has 50 at top. Stack.foldl visits 50, 40.
        Assert.assertEquals(c.toString(), "[20,30,50,40]");
        
        // Narrow it back to a Vector for efficient random access
        Vector<Integer> v = Vector.from(c);
        Assert.assertEquals(v.at(0).orElse(-1), Integer.valueOf(20));
        Assert.assertEquals(v.length(), 4);
    }

    @Test
    public void testMonadicCollectionInterop() {
        // Start with a Maybe
        Collection<Integer> c = Maybe.some(10);
        
        // flatMap into an Either (which is also a Collection)
        c = c.flatMap(i -> Either.right(i + 5));
        
        // Now it's an Either, but we treat it as Collection
        Assert.assertEquals(c.length(), 1);
        Assert.assertEquals(c.foldl(0, Integer::sum), Integer.valueOf(15));
        
        // Narrow back to Maybe
        Maybe<Integer> m = Maybe.from(c);
        Assert.assertTrue(m.isSome());
        Assert.assertEquals(m.orElse(-1), Integer.valueOf(15));
    }
}
