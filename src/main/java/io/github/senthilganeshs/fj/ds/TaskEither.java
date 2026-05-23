package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.typeclass.Monad;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Represents an asynchronous computation that can fail with an error of type E or succeed with a value of type A.
 */
public record TaskEither<E, A>(Task<Either<E, A>> task) implements Higher<Higher<TaskEither.µ, E>, A> {
    public final static class µ {}

    @SuppressWarnings("unchecked")
    public static <E, A> TaskEither<E, A> narrowK(Higher<Higher<µ, E>, A> hka) {
        return (TaskEither<E, A>) hka;
    }

    public static <E, A> TaskEither<E, A> of(Task<Either<E, A>> task) {
        return new TaskEither<>(task);
    }

    public static <E, A> TaskEither<E, A> left(E e) {
        return new TaskEither<>(Task.succeed(Either.left(e)));
    }

    public static <E, A> TaskEither<E, A> right(A a) {
        return new TaskEither<>(Task.succeed(Either.right(a)));
    }

    /**
     * Lifts an untyped failing task into a typed failure channel.
     */
    public static <E, A> TaskEither<E, A> fromTask(Task<A> task, Function<Throwable, E> errorMapper) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(errorMapper, "errorMapper");
        return new TaskEither<>(task
            .map(value -> Either.<E, A>right(value))
            .recover(error -> Either.left(errorMapper.apply(error))));
    }

    /**
     * Runs all task-eithers and collects successful values, returning the first left in list order.
     */
    public static <E, A> TaskEither<E, List<A>> sequence(List<TaskEither<E, A>> tasks) {
        return parTraverse(tasks, Function.identity());
    }

    /**
     * Traverses values in parallel with typed failure handling.
     */
    public static <E, A, B> TaskEither<E, List<B>> parTraverse(
        List<A> items,
        Function<A, TaskEither<E, B>> fn
    ) {
        Objects.requireNonNull(items, "items");
        int concurrency = Math.max(1, items.length());
        return boundedParTraverse(concurrency, items, fn);
    }

    /**
     * Traverses values with bounded parallelism and typed failure handling.
     */
    public static <E, A, B> TaskEither<E, List<B>> boundedParTraverse(
        int concurrency,
        List<A> items,
        Function<A, TaskEither<E, B>> fn
    ) {
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(fn, "fn");
        if (concurrency <= 0) {
            return TaskEither.of(Task.fail(new IllegalArgumentException("concurrency must be positive")));
        }
        return TaskEither.of(Task.boundedParTraverse(concurrency, items, item -> fn.apply(item).task())
            .map(TaskEither::collectResults));
    }

    /**
     * Zips two independent task-eithers concurrently and returns both successful values.
     * If either side is left, returns the first left in tuple order after both complete.
     */
    public static <E, A, B> TaskEither<E, Tuple<A, B>> zip(
        TaskEither<E, A> first,
        TaskEither<E, B> second
    ) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        return TaskEither.of(Task.zip(first.task(), second.task()).map(tuple -> {
            Either<E, A> left = tuple.getA().orElse(null);
            Either<E, B> right = tuple.getB().orElse(null);
            if (left.isLeft()) {
                return Either.left(left.fromLeft(null));
            }
            if (right.isLeft()) {
                return Either.left(right.fromLeft(null));
            }
            return Either.right(Tuple.of(left.fromRight(null), right.fromRight(null)));
        }));
    }

    /**
     * Zips two independent task-eithers concurrently and returns both successful values.
     */
    public static <E, A, B> TaskEither<E, Tuple<A, B>> parZip(
        TaskEither<E, A> first,
        TaskEither<E, B> second
    ) {
        return zip(first, second);
    }

    public <B> TaskEither<E, B> map(Function<A, B> fn) {
        return new TaskEither<>(task.map(e -> (Either<E, B>) e.map(fn)));
    }

    public <B> TaskEither<B, A> mapLeft(Function<E, B> fn) {
        return new TaskEither<>(task.map(e -> (Either<B, A>) e.mapLeft(fn)));
    }

    public <B> TaskEither<E, B> flatMap(Function<A, TaskEither<E, B>> fn) {
        return new TaskEither<>(task.flatMap(either -> 
            either.either(
                e -> Task.succeed((Either<E, B>) Either.left(e)),
                a -> fn.apply(a).task()
            )
        ));
    }

    public TaskEither<E, A> timeout(Duration duration) {
        return new TaskEither<>(task.timeout(duration));
    }

    /**
     * Adds a timeout to the underlying task.
     *
     * @deprecated Prefer {@link #timeout(Duration)} for safer, modern time handling.
     */
    @Deprecated(since = "2.0.20", forRemoval = false)
    public TaskEither<E, A> timeout(long timeout, TimeUnit unit) {
        return new TaskEither<>(task.timeout(timeout, unit));
    }

    /**
     * Executes the task asynchronously and provides the result to a callback.
     */
    public void runAsync(java.util.function.Consumer<Either<Throwable, Either<E, A>>> callback) {
        task.runAsync(callback);
    }

    public void runAsync(Maybe<CancellationToken> token, java.util.function.Consumer<Either<Throwable, Either<E, A>>> callback) {
        task.runAsync(token, callback);
    }

    public Either<E, A> run() {
        return task.run();
    }

    public Either<E, A> runSync() {
        return run();
    }

    public void runSync(java.util.function.Consumer<Either<E, A>> callback) {
        callback.accept(run());
    }

    public Either<E, A> run(Maybe<CancellationToken> token) {
        return task.run(token);
    }

    public static <E> Monad<Higher<µ, E>> monad() {
        return new Monad<>() {
            @Override
            @SuppressWarnings("unchecked")
            public <A> Higher<Higher<µ, E>, A> pure(A a) { return (Higher) TaskEither.right(a); }

            @Override
            @SuppressWarnings("unchecked")
            public <A, B> Higher<Higher<µ, E>, B> flatMap(Function<A, Higher<Higher<µ, E>, B>> fn, Higher<Higher<µ, E>, A> fa) {
                return (Higher) narrowK(fa).flatMap(a -> narrowK(fn.apply(a)));
            }

            @Override
            @SuppressWarnings("unchecked")
            public <A, B> Higher<Higher<µ, E>, B> map(Function<A, B> fn, Higher<Higher<µ, E>, A> fa) {
                return (Higher) narrowK(fa).map(fn);
            }
        };
    }

    private static <E, A> Either<E, List<A>> collectResults(List<Either<E, A>> results) {
        List<A> values = List.nil();
        for (Either<E, A> result : results) {
            if (result.isLeft()) {
                return Either.left(result.fromLeft(null));
            }
            values = values.build(result.fromRight(null));
        }
        return Either.right(values);
    }
}
