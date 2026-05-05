package io.github.senthilganeshs.fj.ds;

import org.testng.annotations.Test;
import static org.testng.Assert.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RefTest {

    @Test
    public void testAtomicUpdate() throws InterruptedException {
        Ref<Integer> count = Ref.of(0);
        int threads = 10;
        int iterations = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads * iterations; i++) {
            executor.submit(() -> count.update(c -> c + 1));
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(count.get().intValue(), threads * iterations);
    }

    @Test
    public void testModify() {
        Ref<List<String>> log = Ref.of(List.nil());
        String result = log.modify(l -> {
            List<String> next = List.from(l.build("event"));
            return Tuple.of(next, "processed");
        });

        assertEquals(result, "processed");
        assertEquals(log.get().length(), 1);
        assertEquals(log.get().headMaybe().orElse(""), "event");
    }
}
