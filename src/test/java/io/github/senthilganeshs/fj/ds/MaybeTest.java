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
    public void testMaybeToString() {
        Assert.assertEquals(Maybe.some(10).toString(), "Some (10)");
        Assert.assertEquals(Maybe.nothing().toString(), "Nothing");
    }
}
