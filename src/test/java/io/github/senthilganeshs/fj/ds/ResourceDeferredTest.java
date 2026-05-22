package io.github.senthilganeshs.fj.ds;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class ResourceDeferredTest {

    @Test
    public void testResourceReleaseOrder() {
        StringBuilder log = new StringBuilder();

        Resource<String> outer = Resource.make(
            Task.succeed("outer"),
            __ -> Task.of(() -> {
                log.append("O");
                return null;
            })
        );

        Resource<String> nested = outer.flatMap(o -> Resource.make(
            Task.succeed(o + "-inner"),
            __ -> Task.of(() -> {
                log.append("I");
                return null;
            })
        ));

        Assert.assertEquals(nested.use(value -> Task.succeed(value.length())).run(), Integer.valueOf("outer-inner".length()));
        Assert.assertEquals(log.toString(), "IO");
    }

    @Test
    public void testResourceReleaseOnFailureAndCancellation() {
        AtomicInteger releases = new AtomicInteger(0);
        Resource<String> resource = Resource.make(
            Task.succeed("value"),
            __ -> Task.of(() -> {
                releases.incrementAndGet();
                return null;
            })
        );

        try {
            resource.use(__ -> Task.<Integer>fail(new IllegalStateException("boom"))).run();
            Assert.fail("Expected failure");
        } catch (RuntimeException ex) {
            Assert.assertTrue(ex.getCause() instanceof IllegalStateException || ex.getCause() == null);
        }
        Assert.assertEquals(releases.get(), 1);

        releases.set(0);
        Fiber<Throwable, Integer> fiber = resource.use(__ -> Task.<Integer>asyncCancelable(callback -> () -> { })).start();
        fiber.cancel();
        Assert.assertTrue(fiber.join().run().isCancelled());
        Assert.assertEquals(releases.get(), 1);
    }

    @Test
    public void testDeferredCompletionSemantics() {
        Deferred<Integer> deferred = Deferred.of();
        Assert.assertTrue(deferred.tryGet().isNothing());
        Assert.assertTrue(deferred.complete(7));
        Assert.assertFalse(deferred.complete(9));
        Assert.assertEquals(deferred.tryGet().orElse(0), Integer.valueOf(7));
        Assert.assertEquals(deferred.get().run(), Integer.valueOf(7));
    }

    @Test
    public void testDeferredNullValueAndLateSubscribers() {
        Deferred<String> deferred = Deferred.of();

        Assert.assertTrue(deferred.complete(null));
        Assert.assertTrue(deferred.tryGet().isSome());
        Assert.assertNull(deferred.tryGet().orElse("fallback"));
        Assert.assertNull(deferred.get().run());
        Assert.assertNull(deferred.get().run());
    }

    @Test
    public void testDeferredCancelledWaitDoesNotCorruptValue() {
        Deferred<Integer> deferred = Deferred.of();

        Fiber<Throwable, Integer> waiter = deferred.get().start();
        waiter.cancel();

        Assert.assertTrue(deferred.complete(11));
        Assert.assertEquals(deferred.tryGet().orElse(0), Integer.valueOf(11));
        Assert.assertEquals(deferred.get().run(), Integer.valueOf(11));
        Assert.assertTrue(waiter.join().run().isCancelled());
    }
}
