package io.github.senthilganeshs.fj;

import io.github.senthilganeshs.fj.ds.*;
import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.parser.*;
import io.github.senthilganeshs.fj.stream.*;
import io.github.senthilganeshs.fj.test.*;
import io.github.senthilganeshs.fj.typeclass.Monad;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public class StrategicExpansionPhase2Test {

    @Test
    public void testParserChainl1() {
        // Simple math parser: integers separated by plus
        Parser<BiFunction<Integer, Integer, Integer>> plus = Parser.character('+').lexeme().map(__ -> Integer::sum);
        Parser<Integer> expr = Parser.integer().chainl1(plus);

        Assert.assertEquals(expr.parse("1+2+3").orElse(0), Integer.valueOf(6));
    }

    @Test
    public void testStreamBracket() {
        AtomicBoolean released = new AtomicBoolean(false);
        Stream<IO.µ, Integer> s = Stream.bracket(
            IO.pure("Resource"),
            r -> Stream.emit(r.length(), IO.monad),
            r -> IO.of(() -> { released.set(true); return null; }),
            IO.monad
        );

        int result = IO.narrowK(s.foldl(0, (acc, i) -> i, IO.monad)).unsafeRun();
        Assert.assertEquals(result, 8);
        Assert.assertTrue(released.get());
    }

    @Test
    public void testTaskCancellation() {
        CancellationToken token = new CancellationToken();
        token.cancel();

        Task<Integer> t = Task.of(() -> 10);
        
        Assert.expectThrows(RuntimeException.class, () -> t.run(Maybe.some(token)));
    }

    @Test
    public void testShrinking() {
        // A property that fails for integers > 5
        Property<Integer> prop = Property.forAll(Gen.choose(0, 100), Shrink.integer(), i -> i <= 5);
        
        Maybe<Integer> failed = prop.check(100);
        Assert.assertTrue(failed.isSome());
        // Shrinking should bring the failed value down to the minimal failure (6)
        Assert.assertEquals(failed.orElse(0), Integer.valueOf(6));
    }

    @Test
    public void testMonadLawsForMaybe() {
        MonadLaws.check(Maybe.monad, Gen.choose(1, 100), 50);
    }

    @Test
    public void testFunctorLawsForList() {
        FunctorLaws.check(List.monad, Gen.list(Gen.integer(), 5).map(h -> (Higher<List.µ, Integer>) h), 50);
    }
}
