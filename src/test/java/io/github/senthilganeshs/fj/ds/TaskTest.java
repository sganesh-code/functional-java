package io.github.senthilganeshs.fj.ds;

import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.concurrent.atomic.AtomicInteger;

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
    public void testCollectionParMap() {
        List<Integer> list = List.of(1, 2, 3);
        Task<Collection<Integer>> task = list.parMap(i -> i + 1);
        Collection<Integer> result = task.run();
        Assert.assertEquals(result.length(), 3);
    }
}
