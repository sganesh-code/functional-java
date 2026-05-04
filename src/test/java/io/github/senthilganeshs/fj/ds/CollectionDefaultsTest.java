package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.typeclass.*;

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
        Assert.assertEquals(result.orElse(List.nil()).toString(), "[2,4,6]");
        
        // Failure case
        Collection<Collection<Integer>> failed = list.traverse(i -> i == 2 ? Maybe.nothing() : Maybe.some(i));
        Assert.assertTrue(((Maybe<?>) (Collection<?>) failed).isNothing());
    }

    @Test
    public void testSequence() {
        List<Maybe<Integer>> list = List.of(Maybe.some(1), Maybe.some(2));
        Collection<Collection<Integer>> sequenced = Collection.sequence((Collection<Collection<Integer>>) (Collection<?>) list);
        
        Assert.assertTrue(sequenced instanceof Maybe);
        Assert.assertEquals(((Maybe<List<Integer>>) (Collection<?>) sequenced).orElse(List.nil()).toString(), "[1,2]");
    }

    @Test
    public void testLiftA2() {
        Maybe<Integer> m1 = Maybe.some(10);
        Maybe<Integer> m2 = Maybe.some(20);
        
        Collection<Integer> lifted = m1.liftA2((a, b) -> a + b, m2);
        Assert.assertEquals(((Maybe<Integer>)lifted).orElse(0), Integer.valueOf(30));
        
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
    public void testLiftA3() {
        Maybe<Integer> m1 = Maybe.some(1);
        Maybe<Integer> m2 = Maybe.some(2);
        Maybe<Integer> m3 = Maybe.some(3);

        Collection<Integer> lifted = m1.liftA3((a, b, c) -> a + b + c, m2, m3);
        Assert.assertEquals(lifted.head().orElse(-1), Integer.valueOf(6));
    }

    @Test
    public void testLiftA4() {
        Maybe<Integer> m1 = Maybe.some(1);
        Maybe<Integer> m2 = Maybe.some(2);
        Maybe<Integer> m3 = Maybe.some(3);
        Maybe<Integer> m4 = Maybe.some(4);

        Collection<Integer> lifted = m1.liftA4((a, b, c, d) -> a + b + c + d, m2, m3, m4);
        Assert.assertEquals(lifted.head().orElse(-1), Integer.valueOf(10));
    }

    @Test
    public void testIntersperse() {
        List<String> list = List.of("a", "b", "c");
        Assert.assertEquals(list.intersperse("-").toString(), "[a,-,b,-,c]");
    }

    @Test
    public void testIntercalate() {
        List<List<String>> lists = List.of(List.of("a", "b"), List.of("c", "d"));
        List<String> sep = List.of("-");
        Collection<String> result = sep.intercalate(lists);
        // Result is [a,b,-,c,d]
        Assert.assertTrue(result.toString().contains("-"));
    }

    @Test
    public void testFlatten() {
        List<List<Integer>> nested = List.of(List.of(1, 2), List.of(3, 4));
        Collection<Integer> flattened = Collection.flatten(nested);
        Assert.assertEquals(flattened.length(), 4);
        Assert.assertEquals(flattened.toString(), "[1,2,3,4]");
    }

    @Test
    public void testSum() {
        List<Integer> list = List.of(1, 2, 3, 4);
        Assert.assertEquals(Collection.sum(list), 10.0);
    }

    @Test
    public void testForEach() {
        List<Integer> list = List.of(1, 2, 3);
        java.util.List<Integer> result = new java.util.ArrayList<>();
        list.forEach(result::add);
        Assert.assertEquals(result.size(), 3);
        Assert.assertEquals(result.get(0), Integer.valueOf(1));
    }

    @Test
    public void testReduce() {
        List<Integer> list = List.of(1, 2, 3, 4);
        Assert.assertEquals(list.reduce(Integer::sum).orElse(0), Integer.valueOf(10));
        Assert.assertTrue(List.<Integer>nil().reduce(Integer::sum).isNothing());
    }

    @Test
    public void testSearchUtilities() {
        List<String> list = List.of("a", "b", "c");
        Assert.assertEquals(list.findIndex(s -> s.equals("b")).orElse(-1), Integer.valueOf(1));
        Assert.assertEquals(list.indexOf("c").orElse(-1), Integer.valueOf(2));
        Assert.assertTrue(list.indexOf("z").isNothing());
    }
}
