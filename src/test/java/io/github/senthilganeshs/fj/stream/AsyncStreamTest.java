package io.github.senthilganeshs.fj.stream;

import io.github.senthilganeshs.fj.ds.List;
import io.github.senthilganeshs.fj.ds.Maybe;
import io.github.senthilganeshs.fj.ds.Task;
import io.github.senthilganeshs.fj.ds.Tuple;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncStreamTest {

    @Test
    public void testFromListMapFlatMapConcatAndToList() {
        AsyncStream<Integer> stream = AsyncStream.fromList(List.of(1, 2))
            .map(i -> i + 1)
            .flatMap(i -> AsyncStream.fromList(List.of(i, i)))
            .concat(AsyncStream.emit(99));

        Assert.assertEquals(stream.toList().run().toString(), "[2,2,3,3,99]");
    }

    @Test
    public void testUnfoldTaskBuildsStream() {
        AsyncStream<Integer> stream = AsyncStream.unfoldTask(0, n ->
            n < 3
                ? Task.succeed(Maybe.some(Tuple.of(n, n + 1)))
                : Task.succeed(Maybe.nothing())
        );

        Assert.assertEquals(stream.toList().run().toString(), "[0,1,2]");
    }

    @Test
    public void testFinalizerRunsOnSuccess() {
        AtomicBoolean finalized = new AtomicBoolean(false);

        AsyncStream<Integer> stream = AsyncStream.fromList(List.of(1, 2))
            .onFinalize(Task.of(() -> {
                finalized.set(true);
                return null;
            }));

        Assert.assertEquals(stream.toList().run().toString(), "[1,2]");
        Assert.assertTrue(finalized.get());
    }

    @Test
    public void testFinalizerRunsOnFailure() {
        AtomicInteger finalized = new AtomicInteger(0);

        AsyncStream<Integer> stream = AsyncStream.unfoldTask(0, n -> {
            if (n == 1) {
                return Task.<Maybe<Tuple<Integer, Integer>>>fail(new IllegalStateException("boom"));
            }
            return Task.succeed(Maybe.some(Tuple.of(n, n + 1)));
        }).onFinalize(Task.of(() -> {
            finalized.incrementAndGet();
            return null;
        }));

        try {
            stream.toList().run();
            Assert.fail("Expected failure");
        } catch (RuntimeException ex) {
            Assert.assertTrue(ex.getCause() instanceof IllegalStateException);
        }

        Assert.assertEquals(finalized.get(), 1);
    }
}
