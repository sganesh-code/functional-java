package io.github.senthilganeshs.fj.ds;

import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.function.Function;

public class CollectionDefaultsTest {

    @Test
    public void testTraverse() {
        List<Integer> list = List.of(1, 2, 3);
        // traverse turns List<T> -> (T -> Maybe<R>) -> Maybe<List<R>>
        Collection<Collection<Integer>> traversed = list.traverse(i -> Maybe.some(i * 2));
        
        Assert.assertTrue(traversed instanceof Maybe);
        Maybe<List<Integer>> result = (Maybe<List<Integer>>) (Collection<?>) traversed;
        Assert.assertTrue(result.isSome());
        Assert.assertEquals(result.fromMaybe(List.nil()).toString(), "[2,4,6]");
        
        // Failure case
        Collection<Collection<Integer>> failed = list.traverse(i -> i == 2 ? Maybe.nothing() : Maybe.some(i));
        Assert.assertTrue(((Maybe<?>) (Collection<?>) failed).isNothing());
    }

    @Test
    public void testSequence() {
        List<Maybe<Integer>> list = List.of(Maybe.some(1), Maybe.some(2));
        Collection<Collection<Integer>> sequenced = Collection.sequence((Collection<Collection<Integer>>) (Collection<?>) list);
        
        Assert.assertTrue(sequenced instanceof Maybe);
        Assert.assertEquals(((Maybe<List<Integer>>) (Collection<?>) sequenced).fromMaybe(List.nil()).toString(), "[1,2]");
    }

    @Test
    public void testLiftA2() {
        Maybe<Integer> m1 = Maybe.some(10);
        Maybe<Integer> m2 = Maybe.some(20);
        
        Collection<Integer> lifted = m1.liftA2((a, b) -> a + b, m2);
        Assert.assertEquals(((Maybe<Integer>)lifted).fromMaybe(0), Integer.valueOf(30));
        
        Assert.assertTrue(((Maybe<Integer>) m1.liftA2((a, b) -> a + b, Maybe.<Integer>nothing())).isNothing());
    }

    @Test
    public void testFoldr() {
        List<Integer> list = List.of(1, 2, 3);
        // foldr (1, (2, (3, 0))) = 1 - (2 - (3 - 0)) = 1 - (2 - 3) = 1 - (-1) = 2
        int res = list.foldr(0, (t, acc) -> t - acc);
        Assert.assertEquals(res, 2);
    }

    @Test
    public void testTakeDropSlice() {
        List<Integer> list = List.of(1, 2, 3, 4, 5);
        Assert.assertEquals(list.slice(1, 3).toString(), "[2,3,4]");
        Assert.assertEquals(list.slice(0, 0).length(), 0);
        Assert.assertEquals(list.slice(0, 10).length(), 5);
    }

    @Test
    public void testAnyAll() {
        List<Integer> list = List.of(1, 2, 3);
        Assert.assertTrue(list.any(i -> i == 2));
        Assert.assertFalse(list.any(i -> i == 4));
        Assert.assertTrue(list.all(i -> i > 0));
        Assert.assertFalse(list.all(i -> i > 1));
    }

    @Test
    public void testMkString() {
        List<Integer> list = List.of(1, 2, 3);
        Assert.assertEquals(list.mkString("[", "-", "]"), "[1-2-3]");
    }
}
