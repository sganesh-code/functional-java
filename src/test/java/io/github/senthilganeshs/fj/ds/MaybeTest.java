package io.github.senthilganeshs.fj.ds;

import org.testng.Assert;
import org.testng.annotations.Test;

public class MaybeTest {

    @Test
    public void testMaybeBasic() {
        Maybe<Integer> some = Maybe.some(10);
        Maybe<Integer> nothing = Maybe.nothing();
        
        Assert.assertTrue(some.isSome());
        Assert.assertFalse(some.isNothing());
        Assert.assertTrue(nothing.isNothing());
        Assert.assertFalse(nothing.isSome());
    }

    @Test
    public void testMaybeFrom() {
        Maybe<Integer> some = Maybe.some(10);
        Maybe<Integer> nothing = Maybe.nothing();
        
        Assert.assertEquals(some.fromMaybe(0), Integer.valueOf(10));
        Assert.assertEquals(nothing.fromMaybe(0), Integer.valueOf(0));
    }

    @Test
    public void testMaybeCollectionAPIs() {
        Maybe<Integer> some = Maybe.some(10);
        
        Assert.assertEquals(some.map(i -> i * 2), Maybe.some(20));
        Assert.assertEquals(some.flatMap(i -> Maybe.some(i + 1)), Maybe.some(11));
        Assert.assertEquals(some.flatMap(i -> Maybe.nothing()), Maybe.nothing());
        
        Assert.assertEquals(some.filter(i -> i > 5), Maybe.some(10));
        Assert.assertEquals(some.filter(i -> i > 15), Maybe.nothing());
    }

    @Test
    public void testMaybeNothingCollectionAPIs() {
        Maybe<Integer> nothing = Maybe.nothing();
        
        Assert.assertEquals(nothing.map(i -> i * 2), Maybe.nothing());
        Assert.assertEquals(nothing.flatMap(i -> Maybe.some(i + 1)), Maybe.nothing());
        Assert.assertEquals(nothing.filter(i -> i > 5), Maybe.nothing());
    }

    @Test
    public void testMaybeFoldl() {
        Assert.assertEquals(Maybe.some(10).foldl(0, Integer::sum), Integer.valueOf(10));
        Assert.assertEquals(Maybe.nothing().foldl(0, (acc, i) -> acc + 1), Integer.valueOf(0));
    }

    @Test
    public void testMaybeEquals() {
        Maybe<Integer> some1 = Maybe.some(10);
        Maybe<Integer> some2 = Maybe.some(10);
        Maybe<Integer> some3 = Maybe.some(20);
        Maybe<Integer> nothing = Maybe.nothing();
        
        Assert.assertEquals(some1, some1);
        Assert.assertEquals(some1, some2);
        Assert.assertNotEquals(some1, some3);
        Assert.assertNotEquals(some1, nothing);
        Assert.assertNotEquals(some1, null);
        Assert.assertNotEquals(some1, "not a maybe");
        
        Assert.assertEquals(nothing, Maybe.nothing());
        Assert.assertEquals(nothing, nothing);
        Assert.assertNotEquals(nothing, null);
        Assert.assertNotEquals(nothing, some1);
    }
}
