package io.github.senthilganeshs.fj.reactor;

import io.github.senthilganeshs.fj.ds.Either;
import io.github.senthilganeshs.fj.ds.Fiber;
import io.github.senthilganeshs.fj.ds.List;
import io.github.senthilganeshs.fj.ds.Maybe;
import io.github.senthilganeshs.fj.ds.Task;
import io.github.senthilganeshs.fj.ds.TaskEither;
import io.github.senthilganeshs.fj.ds.Tuple;
import io.github.senthilganeshs.fj.stream.AsyncStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.Assert;
import org.testng.annotations.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.core.scheduler.Schedulers;

public class ReactorInteropTest {

    @Test
    public void testMonoToTaskAndMaybeTask() {
        Assert.assertEquals(ReactorInterop.monoToTask(Mono.just("ok")).run(), "ok");
        Assert.assertTrue(ReactorInterop.monoToMaybeTask(Mono.empty()).run().isNothing());
    }

    @Test
    public void testMonoToTaskIsLazyAndSingleSubscription() {
        AtomicInteger subscriptions = new AtomicInteger(0);
        Task<Integer> task = ReactorInterop.monoToTask(Mono.defer(() -> {
            subscriptions.incrementAndGet();
            return Mono.just(1);
        }));

        Assert.assertEquals(subscriptions.get(), 0);

        StepVerifier.create(task.toMono(Maybe.nothing()))
            .expectNext(1)
            .verifyComplete();

        Assert.assertEquals(subscriptions.get(), 1);
    }

    @Test
    public void testMonoToMaybeTaskFailurePropagates() {
        try {
            ReactorInterop.monoToMaybeTask(Mono.error(new IllegalArgumentException("boom"))).run();
            Assert.fail("Expected failure");
        } catch (RuntimeException ex) {
            Assert.assertTrue(ex.getCause() instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testMonoErrorAndEmptyToTaskEither() {
        try {
            ReactorInterop.monoToTask(Mono.error(new IllegalStateException("boom"))).run();
            Assert.fail("Expected failure");
        } catch (RuntimeException ex) {
            Assert.assertTrue(ex.getCause() instanceof IllegalStateException);
        }

        TaskEither<String, Integer> empty = ReactorInterop.monoToTaskEither(Mono.empty(), error -> "empty:" + error.getClass().getSimpleName());
        Assert.assertEquals(empty.run(), Either.left("empty:NoSuchElementException"));
    }

    @Test
    public void testMonoToTaskEitherAndTaskEitherToMono() {
        TaskEither<String, Integer> fromMono = ReactorInterop.monoToTaskEither(Mono.just(42), Throwable::getMessage);
        Assert.assertEquals(fromMono.run(), Either.right(42));

        StepVerifier.create(ReactorInterop.taskEitherToMono(TaskEither.left("bad"), IllegalStateException::new))
            .expectErrorMatches(error -> error instanceof IllegalStateException && "bad".equals(error.getMessage()))
            .verify();
    }

    @Test
    public void testMonoCancellationDisposesSubscription() {
        AtomicBoolean cancelled = new AtomicBoolean(false);
        Mono<String> mono = Mono.<String>create(sink -> sink.onCancel(() -> cancelled.set(true)));

        Task<String> task = ReactorInterop.monoToTask(mono);
        Fiber<Throwable, String> fiber = task.start();
        fiber.cancel();

        Assert.assertTrue(cancelled.get());
        Assert.assertTrue(fiber.join().run().isCancelled());
    }

    @Test
    public void testTaskMaybeToMonoEmptyCompletesEmpty() {
        StepVerifier.create(ReactorInterop.taskMaybeToMono(Task.succeed(Maybe.nothing())))
            .verifyComplete();
    }

    @Test
    public void testTaskToMonoCancellationDisposesFiber() {
        AtomicBoolean cancelled = new AtomicBoolean(false);
        Task<String> task = Task.asyncCancelable(callback -> () -> cancelled.set(true));

        StepVerifier.create(ReactorInterop.taskToMono(task))
            .thenCancel()
            .verify();

        Assert.assertTrue(cancelled.get());
    }

    @Test
    public void testTaskToMonoSubscribesOnce() {
        AtomicInteger registrations = new AtomicInteger(0);
        Task<Integer> task = Task.asyncCancelable(callback -> {
            registrations.incrementAndGet();
            callback.success(1);
            return () -> { };
        });

        StepVerifier.create(ReactorInterop.taskToMono(task))
            .expectNext(1)
            .verifyComplete();

        Assert.assertEquals(registrations.get(), 1);
    }

    @Test
    public void testTaskToMonoOnNonBlockingScheduler() {
        StepVerifier.create(ReactorInterop.taskToMono(Task.of(() -> "ok")).subscribeOn(Schedulers.parallel()))
            .expectNext("ok")
            .verifyComplete();
    }

    @Test
    public void testFluxErrorAndCancellationToAsyncStream() {
        try {
            ReactorInterop.fluxToAsyncStream(Flux.error(new IllegalStateException("boom"))).toList().run();
            Assert.fail("Expected failure");
        } catch (RuntimeException ex) {
            Assert.assertTrue(ex.getCause() instanceof IllegalStateException);
        }

        AtomicBoolean cancelled = new AtomicBoolean(false);
        Flux<Integer> flux = Flux.<Integer>create(sink -> sink.onCancel(() -> cancelled.set(true)));
        AsyncStream<Integer> stream = ReactorInterop.fluxToAsyncStream(flux);
        Fiber<Throwable, List<Integer>> fiber = stream.toList().start();
        fiber.cancel();

        Assert.assertTrue(cancelled.get());
        Assert.assertTrue(fiber.join().run().isCancelled());
    }

    @Test
    public void testFluxToAsyncStreamIsLazyAndDemandAware() {
        AtomicInteger requested = new AtomicInteger(0);
        AtomicInteger emitted = new AtomicInteger(0);
        Flux<Integer> flux = Flux.create(sink -> sink.onRequest(n -> {
            requested.addAndGet((int) n);
            int next = emitted.incrementAndGet();
            if (next <= 3) {
                sink.next(next);
            } else {
                sink.complete();
            }
        }));

        AsyncStream<Integer> stream = ReactorInterop.fluxToAsyncStream(flux);

        Assert.assertEquals(requested.get(), 0);
        Assert.assertEquals(stream.toList().run().toString(), "[1,2,3]");
        Assert.assertEquals(requested.get(), 4);
    }

    @Test
    public void testAsyncStreamToFluxCompletesAndFinalizesOnSuccess() {
        AtomicInteger finalized = new AtomicInteger(0);
        AsyncStream<Integer> stream = AsyncStream.unfoldTask(0, n ->
            n < 3
                ? Task.succeed(Maybe.some(Tuple.of(n, n + 1)))
                : Task.succeed(Maybe.nothing())
        ).onFinalize(Task.of(() -> {
            finalized.incrementAndGet();
            return null;
        }));

        StepVerifier.create(ReactorInterop.asyncStreamToFlux(stream))
            .expectNext(0, 1, 2)
            .verifyComplete();

        Assert.assertEquals(finalized.get(), 1);
    }

    @Test
    public void testAsyncStreamToFluxFinalizesOnCancel() {
        AtomicInteger finalized = new AtomicInteger(0);
        AsyncStream<Integer> stream = AsyncStream.unfoldTask(0, n ->
            Task.succeed(Maybe.some(Tuple.of(n, n + 1)))
        ).onFinalize(Task.of(() -> {
            finalized.incrementAndGet();
            return null;
        }));

        StepVerifier.create(ReactorInterop.asyncStreamToFlux(stream))
            .expectNext(0, 1, 2)
            .thenCancel()
            .verify();

        Assert.assertEquals(finalized.get(), 1);
    }
}
