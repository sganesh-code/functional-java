package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.stream.AsyncStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AsyncStreamResourceLawTest {

    @Test
    public void testAsyncStreamMapAndFlatMapAcrossSamples() {
        Random random = new Random(42);

        for (int trial = 0; trial < 25; trial++) {
            int size = 1 + random.nextInt(8);
            java.util.List<Integer> input = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                input.add(random.nextInt(20) - 5);
            }

            AsyncStream<Integer> stream = AsyncStream.fromList(List.from(input))
                .map(x -> x + 1)
                .flatMap(x -> AsyncStream.fromList(List.of(x, x * 2)));

            java.util.List<Integer> expected = new ArrayList<>();
            for (Integer value : input) {
                int mapped = value + 1;
                expected.add(mapped);
                expected.add(mapped * 2);
            }

            Assert.assertEquals(stream.toList().run(), List.from(expected));
        }
    }

    @Test
    public void testResourceReleaseRunsExactlyOnceAcrossOutcomes() {
        for (int trial = 0; trial < 10; trial++) {
            AtomicInteger releases = new AtomicInteger(0);
            Resource<String> resource = Resource.make(
                Task.succeed("value-" + trial),
                __ -> Task.of(() -> {
                    releases.incrementAndGet();
                    return null;
                })
            );

            Assert.assertEquals(resource.use(v -> Task.succeed(v.length())).run(), Integer.valueOf(("value-" + trial).length()));
            Assert.assertEquals(releases.get(), 1);

            releases.set(0);
            try {
                resource.use(__ -> Task.<Integer>fail(new IllegalStateException("boom"))).run();
                Assert.fail("Expected failure");
            } catch (RuntimeException ex) {
                Assert.assertTrue(ex.getCause() instanceof IllegalStateException);
            }
            Assert.assertEquals(releases.get(), 1);

            releases.set(0);
            Fiber<Throwable, Integer> fiber = resource.use(__ -> Task.<Integer>asyncCancelable(callback -> () -> { })).start();
            fiber.cancel();
            Assert.assertTrue(fiber.join().run().isCancelled());
            Assert.assertEquals(releases.get(), 1);
        }
    }
}
