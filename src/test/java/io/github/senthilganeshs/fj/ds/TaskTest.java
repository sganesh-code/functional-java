package io.github.senthilganeshs.fj.ds;

import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

public class TaskTest {

    @Test
    public void testTaskBasic() {
        Task<Integer> t = Task.of(() -> 10).map(i -> i * 2);
        Assert.assertEquals(t.run(), Integer.valueOf(20));
    }

    @Test
    public void testTaskFlatMap() {
        Task<Integer> t = Task.of(() -> 10).flatMap(i -> Task.of(() -> i + 5));
        Assert.assertEquals(t.run(), Integer.valueOf(15));
    }

    @Test
    public void testTaskLiftA2() {
        Task<Integer> t1 = Task.of(() -> 10);
        Task<Integer> t2 = Task.of(() -> 20);
        Task<Integer> t3 = t1.liftA2(Integer::sum, t2);
        Assert.assertEquals(t3.run(), Integer.valueOf(30));
    }

    @Test
    public void testParTraverse() {
        List<Integer> list = List.of(1, 2, 3, 4, 5);
        AtomicInteger counter = new AtomicInteger(0);
        
        Task<List<Integer>> result = Task.parTraverse(list, i -> Task.of(() -> {
            counter.incrementAndGet();
            return i * 2;
        }));

        List<Integer> doubled = result.run();
        Assert.assertEquals(doubled.length(), 5);
        Assert.assertEquals(counter.get(), 5);
        Assert.assertEquals(doubled.drop(0).headMaybe().orElse(0), Integer.valueOf(2));
    }

    @Test
    public void testRetry() {
        AtomicInteger attempts = new AtomicInteger(0);
        Task<Integer> failingTask = Task.of(() -> {
            if (attempts.incrementAndGet() < 3) throw new RuntimeException("Fail");
            return 100;
        });

        Task<Integer> retrying = failingTask.retry(5);
        Assert.assertEquals(retrying.run(), Integer.valueOf(100));
        Assert.assertEquals(attempts.get(), 3);
    }

    @Test
    public void testBracket() {
        AtomicBoolean released = new AtomicBoolean(false);
        Task<String> resource = Task.succeed("resource");
        
        Task<Integer> result = Task.bracket(
            resource,
            r -> Task.succeed(r.length()),
            r -> Task.of(() -> { released.set(true); return null; })
        );

        Assert.assertEquals(result.run(), Integer.valueOf(8));
        Assert.assertTrue(released.get());
    }

    @Test
    public void testRace() {
        Task<Integer> t1 = Task.of(() -> {
            try { Thread.sleep(100); } catch (Exception e) {}
            return 1;
        });
        Task<Integer> t2 = Task.of(() -> 2);
        
        Task<Integer> winner = Task.race(List.of(t1, t2));
        Assert.assertEquals(winner.run(), Integer.valueOf(2));
    }

    @Test
    public void testCollectionParMap() {
        List<Integer> list = List.of(1, 2, 3);
        Task<Collection<Integer>> task = list.parMap(i -> i + 1);
        Collection<Integer> result = task.run();
        Assert.assertEquals(result.length(), 3);
    }
}
