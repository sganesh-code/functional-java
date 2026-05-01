package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.typeclass.Monad;
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

    public <B> TaskEither<E, B> map(Function<A, B> fn) {
        return new TaskEither<>(task.map(e -> e.map(fn)));
    }

    public <B> TaskEither<B, A> mapLeft(Function<E, B> fn) {
        return new TaskEither<>(task.map(e -> e.mapLeft(fn)));
    }

    public <B> TaskEither<E, B> flatMap(Function<A, TaskEither<E, B>> fn) {
        return new TaskEither<>(task.flatMap(either -> 
            either.either(
                e -> Task.succeed(Either.left(e)),
                a -> fn.apply(a).task()
            )
        ));
    }

    public TaskEither<E, A> timeout(long timeout, TimeUnit unit) {
        return new TaskEither<>(task.timeout(timeout, unit));
    }

    public Either<E, A> run() {
        return task.run();
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
}
