package io.github.senthilganeshs.fj.ds;

import org.testng.Assert;
import org.testng.annotations.Test;

public class EitherTest {

    @Test
    public void testEitherBasic() {
        Either<String, Integer> right = Either.right(10);
        Either<String, Integer> left = Either.left("error");
        
        Assert.assertTrue(right.isRight());
        Assert.assertFalse(right.isLeft());
        Assert.assertTrue(left.isLeft());
        Assert.assertFalse(left.isRight());
    }

    @Test
    public void testEitherFrom() {
        Either<String, Integer> right = Either.right(10);
        Either<String, Integer> left = Either.left("error");
        
        Assert.assertEquals(right.fromRight(0), Integer.valueOf(10));
        Assert.assertEquals(right.fromLeft("default"), "default");
        
        Assert.assertEquals(left.fromLeft("default"), "error");
        Assert.assertEquals(left.fromRight(0), Integer.valueOf(0));
    }

    @Test
    public void testEitherMethod() {
        Either<String, Integer> right = Either.right(10);
        Either<String, Integer> left = Either.left("error");
        
        String r = right.either(s -> "L:" + s, i -> "R:" + i);
        Assert.assertEquals(r, "R:10");
        
        String l = left.either(s -> "L:" + s, i -> "R:" + i);
        Assert.assertEquals(l, "L:error");
    }

    @Test
    public void testEitherLeftsRights() {
        List<Either<String, Integer>> list = List.of(
            Either.right(1),
            Either.left("e1"),
            Either.right(2),
            Either.left("e2")
        );
        
        Assert.assertEquals(Either.rights(list).toString(), "[1,2]");
        Assert.assertEquals(Either.lefts(list).toString(), "[e1,e2]");
    }

    @Test
    public void testEitherCollectionAPIs() {
        Either<String, Integer> right = Either.right(10);
        
        Assert.assertEquals(right.map(i -> i * 2), Either.right(20));
        Assert.assertEquals(right.flatMap(i -> Either.right(i + 1)), Either.right(11));
        Assert.assertEquals(right.flatMap(i -> Either.left("fail")), Either.left("fail"));
        
        // Filter: Right(10) filtered by i > 5 stays Right(10)
        Either<String, Integer> f1 = (Either<String, Integer>) right.filter(i -> i > 5);
        Assert.assertTrue(f1.isRight(), "Expected Right but was " + f1);
        Assert.assertEquals(f1.fromRight(0), Integer.valueOf(10));
        // Filter: Right(10) filtered by i > 15 becomes Left(10)
        Either<Integer, Integer> filtered = (Either<Integer, Integer>) (Collection<?>) right.filter(i -> i > 15);
        Assert.assertTrue(filtered.isLeft());
        Assert.assertEquals(filtered.fromLeft(0), Integer.valueOf(10));
    }

    @Test
    public void testEitherLeftCollectionAPIs() {
        Either<String, Integer> left = Either.left("error");
        
        Assert.assertEquals(left.map(i -> i * 2), Either.left("error"));
        Assert.assertEquals(left.flatMap(i -> Either.right(i + 1)), Either.left("error"));
        Assert.assertEquals(left.filter(i -> i > 5), Either.left("error"));
    }

    @Test
    public void testEitherEquals() {
        Either<String, Integer> r1 = Either.right(10);
        Either<String, Integer> r2 = Either.right(10);
        Either<String, Integer> l1 = Either.left("err");
        
        Assert.assertEquals(r1, r1);
        Assert.assertEquals(r1, r2);
        Assert.assertNotEquals(r1, l1);
        Assert.assertNotEquals(r1, null);
        Assert.assertNotEquals(l1, Either.left("other"));
    }

    @Test
    public void testEitherStaticHelpers() {
        List<Either<String, Integer>> list = List.of(Either.right(1), Either.left("err"));
        Assert.assertEquals(Either.lefts(list).length(), 1);
        Assert.assertEquals(Either.rights(list).length(), 1);
    }
}
