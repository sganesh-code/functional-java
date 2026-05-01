package io.github.senthilganeshs.fj.stream;

import io.github.senthilganeshs.fj.ds.IO;
import io.github.senthilganeshs.fj.ds.List;
import io.github.senthilganeshs.fj.ds.Task;
import io.github.senthilganeshs.fj.hkt.Higher;
import org.testng.Assert;
import org.testng.annotations.Test;

public class StreamTest {

    @Test
    public void testBasicStream() {
        Stream<IO.µ, Integer> s1 = Stream.emit(1, IO.monad);
        Stream<IO.µ, Integer> s2 = Stream.emit(2, IO.monad);
        Stream<IO.µ, Integer> combined = Stream.concat(s1, s2, IO.monad);
        
        Higher<IO.µ, Integer> sumIO = combined.foldl(0, Integer::sum, IO.monad);
        Assert.assertEquals(IO.narrowK(sumIO).unsafeRun(), Integer.valueOf(3));
    }

    @Test
    public void testMapStream() {
        Stream<IO.µ, Integer> s = Stream.emit(10, IO.monad).map(i -> i * 2, IO.monad);
        Higher<IO.µ, Integer> valIO = s.foldl(0, (acc, i) -> i, IO.monad);
        Assert.assertEquals(IO.narrowK(valIO).unsafeRun(), Integer.valueOf(20));
    }

    @Test
    public void testParEvalMap() {
        Stream<Task.µ, Integer> s = Stream.emit(1, Task.monad);
        Stream<Task.µ, Integer> s2 = Stream.concat(s, Stream.emit(2, Task.monad), Task.monad);
        
        Stream<Task.µ, Integer> parallel = s2.parEvalMap(2, i -> Task.of(() -> i * 10));
        
        Higher<Task.µ, Integer> sumTask = parallel.foldl(0, Integer::sum, Task.monad);
        Assert.assertEquals(Task.narrowK(sumTask).run(), Integer.valueOf(30));
    }
}
