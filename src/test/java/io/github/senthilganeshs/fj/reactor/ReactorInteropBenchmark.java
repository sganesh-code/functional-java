package io.github.senthilganeshs.fj.reactor;

import io.github.senthilganeshs.fj.ds.List;
import io.github.senthilganeshs.fj.ds.Task;
import io.github.senthilganeshs.fj.stream.AsyncStream;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
public class ReactorInteropBenchmark {

    @Param({"1000"})
    int size;

    Mono<Integer> mono;
    Task<Integer> task;
    Flux<Integer> flux;
    AsyncStream<Integer> stream;

    @Setup
    public void setup() {
        mono = Mono.just(42);
        task = Task.succeed(42);
        flux = Flux.range(0, size);
        stream = AsyncStream.fromList(List.from(java.util.stream.IntStream.range(0, size).boxed().toList()));
    }

    @Benchmark
    public Integer monoToTask() {
        return ReactorInterop.monoToTask(mono).run();
    }

    @Benchmark
    public Integer taskToMono() {
        return ReactorInterop.taskToMono(task).block();
    }

    @Benchmark
    public String fluxToAsyncStream() {
        return ReactorInterop.fluxToAsyncStream(flux).toList().run().toString();
    }

    @Benchmark
    public String asyncStreamToFlux() {
        return ReactorInterop.asyncStreamToFlux(stream).collectList().block().toString();
    }
}
