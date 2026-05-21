package io.github.senthilganeshs.fj.stream;

import io.github.senthilganeshs.fj.ds.List;
import io.github.senthilganeshs.fj.ds.Maybe;
import io.github.senthilganeshs.fj.ds.Task;
import io.github.senthilganeshs.fj.ds.Tuple;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A thin functional wrapper over {@link Stream} specialized for {@link Task}.
 *
 * @param <A> The type of elements in the stream.
 */
public final class AsyncStream<A> {
    private final Stream<Task.µ, A> stream;
    private final Task<Void> finalizer;

    private AsyncStream(Stream<Task.µ, A> stream, Task<Void> finalizer) {
        this.stream = Objects.requireNonNull(stream, "stream");
        this.finalizer = Objects.requireNonNull(finalizer, "finalizer");
    }

    public static <A> AsyncStream<A> empty() {
        return new AsyncStream<>(Stream.empty(Task.monad), Task.succeed(null));
    }

    public static <A> AsyncStream<A> emit(A value) {
        return new AsyncStream<>(Stream.emit(value, Task.monad), Task.succeed(null));
    }

    public static <A> AsyncStream<A> fromList(List<A> list) {
        return new AsyncStream<>(Stream.fromList(list, Task.monad), Task.succeed(null));
    }

    public static <S, A> AsyncStream<A> unfoldTask(S seed, Function<S, Task<Maybe<Tuple<A, S>>>> fn) {
        Objects.requireNonNull(fn, "fn");
        return new AsyncStream<>(unfold(seed, fn), Task.succeed(null));
    }

    public AsyncStream<A> onFinalize(Task<Void> extraFinalizer) {
        Objects.requireNonNull(extraFinalizer, "extraFinalizer");
        return new AsyncStream<>(stream, finalizer.flatMap(__ -> extraFinalizer));
    }

    public <B> AsyncStream<B> map(Function<A, B> fn) {
        Objects.requireNonNull(fn, "fn");
        return new AsyncStream<>(stream.map(fn, Task.monad), finalizer);
    }

    public <B> AsyncStream<B> flatMap(Function<A, AsyncStream<B>> fn) {
        Objects.requireNonNull(fn, "fn");
        return new AsyncStream<>(stream.flatMap(a -> {
            AsyncStream<B> next = Objects.requireNonNull(fn.apply(a), "fn result");
            return next.materialized();
        }, Task.monad), finalizer);
    }

    public AsyncStream<A> concat(AsyncStream<A> other) {
        Objects.requireNonNull(other, "other");
        return new AsyncStream<>(Stream.concat(materialized(), other.materialized(), Task.monad), Task.succeed(null));
    }

    public <B> Task<B> foldl(B seed, BiFunction<B, A, B> fn) {
        Objects.requireNonNull(fn, "fn");
        return Task.bracket(
            Task.succeed(null),
            __ -> Task.narrowK(stream.foldl(seed, fn, Task.monad)),
            __ -> finalizer
        );
    }

    /**
     * Advances the stream by one element, returning the current value and the remaining stream.
     */
    public Task<Maybe<Tuple<A, AsyncStream<A>>>> step() {
        return Task.narrowK(stream.step()).map(maybe ->
            maybe.map(tuple -> Tuple.of(
                tuple.getA().orElse(null),
                new AsyncStream<>(tuple.getB().orElse(null), finalizer)
            ))
        );
    }

    /**
     * Exposes the finalizer associated with this stream.
     */
    public Task<Void> finalizer() {
        return finalizer;
    }

    public Task<List<A>> toList() {
        return foldl(List.<A>nil(), (acc, a) -> acc.build(a));
    }

    Stream<Task.µ, A> materialized() {
        return stream.onFinalize(finalizer, Task.monad);
    }

    private static <S, A> Stream<Task.µ, A> unfold(S seed, Function<S, Task<Maybe<Tuple<A, S>>>> fn) {
        return new Stream<>(Task.defer(() ->
            fn.apply(seed).map(maybe ->
                maybe.map(tuple -> Tuple.of(
                    tuple.getA().orElse(null),
                    unfold(tuple.getB().orElse(null), fn)
                ))
            )
        ));
    }
}
