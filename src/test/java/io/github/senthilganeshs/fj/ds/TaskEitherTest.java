package io.github.senthilganeshs.fj.ds;

import org.testng.Assert;
import org.testng.annotations.Test;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class TaskEitherTest {

    @Test
    public void testFromTaskMapsFailures() {
        TaskEither<String, Integer> right = TaskEither.fromTask(Task.succeed(10), Throwable::getMessage);
        Assert.assertEquals(right.run().fromRight(0), Integer.valueOf(10));

        TaskEither<String, Integer> left = TaskEither.fromTask(
            Task.fail(new IllegalStateException("boom")),
            Throwable::getMessage
        );
        Assert.assertEquals(left.run().fromLeft(""), "boom");
    }

    @Test
    public void testTimeoutDuration() {
        TaskEither<String, Long> slow = TaskEither.fromTask(
            Task.fromMono(Mono.delay(Duration.ofMillis(200))),
            Throwable::getMessage
        );

        try {
            slow.timeout(Duration.ofMillis(20)).run();
            Assert.fail("Expected timeout");
        } catch (RuntimeException ex) {
            Assert.assertTrue(ex.getCause() instanceof TimeoutException);
        }
    }

    @Test
    public void testSequenceCollectsRightsAndReturnsFirstLeftInOrder() {
        TaskEither<String, List<Integer>> rights = TaskEither.sequence(List.of(
            TaskEither.right(1),
            TaskEither.right(2),
            TaskEither.right(3)
        ));

        Assert.assertEquals(rights.run().fromRight(List.nil()).toString(), "[1,2,3]");

        TaskEither<String, List<Integer>> lefts = TaskEither.sequence(List.of(
            TaskEither.<String, Integer>right(1),
            TaskEither.<String, Integer>left("first"),
            TaskEither.<String, Integer>left("second")
        ));

        Assert.assertEquals(lefts.run().fromLeft(""), "first");
    }

    @Test
    public void testParTraverseAndBoundedParTraverse() {
        TaskEither<String, List<Integer>> doubled = TaskEither.parTraverse(
            List.of(1, 2, 3),
            value -> TaskEither.right(value * 2)
        );

        Assert.assertEquals(doubled.run().fromRight(List.nil()).toString(), "[2,4,6]");

        AtomicInteger active = new AtomicInteger(0);
        AtomicInteger maxActive = new AtomicInteger(0);

        TaskEither<String, List<Integer>> bounded = TaskEither.boundedParTraverse(
            2,
            List.range(0, 8),
            value -> TaskEither.fromTask(Task.fromMono(Mono.fromSupplier(() -> {
                int current = active.incrementAndGet();
                maxActive.updateAndGet(previous -> Math.max(previous, current));
                try {
                    Thread.sleep(30);
                    return value;
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ex);
                } finally {
                    active.decrementAndGet();
                }
            }).subscribeOn(Schedulers.boundedElastic())), Throwable::getMessage)
        );

        Assert.assertEquals(bounded.run().fromRight(List.nil()).length(), 8);
        Assert.assertTrue(maxActive.get() <= 2, "max concurrency was " + maxActive.get());
    }

    @Test
    public void testZipAndParZipRights() {
        Either<String, Tuple<Integer, String>> zipped = TaskEither.zip(
            TaskEither.<String, Integer>right(1),
            TaskEither.<String, String>right("a")
        ).run();

        Assert.assertTrue(zipped.isRight());
        Assert.assertEquals(zipped.fromRight(null).getA().orElse(0), Integer.valueOf(1));
        Assert.assertEquals(zipped.fromRight(null).getB().orElse(""), "a");

        Either<String, Tuple<Integer, String>> parZipped = TaskEither.parZip(
            TaskEither.<String, Integer>right(2),
            TaskEither.<String, String>right("b")
        ).run();

        Assert.assertEquals(parZipped.fromRight(null).getA().orElse(0), Integer.valueOf(2));
        Assert.assertEquals(parZipped.fromRight(null).getB().orElse(""), "b");
    }

    @Test
    public void testZipReturnsFirstLeftInTupleOrderAfterBothComplete() {
        AtomicInteger completed = new AtomicInteger(0);
        TaskEither<String, Integer> first = TaskEither.of(Task.fromMono(
            Mono.just(Either.<String, Integer>left("first"))
                .delayElement(Duration.ofMillis(50))
                .doOnNext(__ -> completed.incrementAndGet())
        ));
        TaskEither<String, Integer> second = TaskEither.of(Task.fromMono(
            Mono.just(Either.<String, Integer>left("second"))
                .doOnNext(__ -> completed.incrementAndGet())
        ));

        Either<String, Tuple<Integer, Integer>> result = TaskEither.zip(first, second).run();

        Assert.assertEquals(result.fromLeft(""), "first");
        Assert.assertEquals(completed.get(), 2);
    }
}
