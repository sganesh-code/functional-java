package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.typeclass.Monad;
import java.util.function.Function;

/**
 * The ultimate effect for modern applications.
 * A Reader that returns a TaskEither.
 * Supports dependency injection (R), asynchronous failure (E), and successful results (A).
 */
public record ReaderTaskEither<R, E, A>(Reader<R, TaskEither<E, A>> reader) implements Higher<Higher<Higher<ReaderTaskEither.µ, R>, E>, A> {
    public final static class µ {}

    @SuppressWarnings("unchecked")
    public static <R, E, A> ReaderTaskEither<R, E, A> narrowK(Higher<Higher<Higher<µ, R>, E>, A> hka) {
        return (ReaderTaskEither<R, E, A>) hka;
    }

    public static <R, E, A> ReaderTaskEither<R, E, A> right(A a) {
        return new ReaderTaskEither<>(Reader.pure(TaskEither.right(a)));
    }

    public static <R, E, A> ReaderTaskEither<R, E, A> left(E e) {
        return new ReaderTaskEither<>(Reader.pure(TaskEither.left(e)));
    }

    public static <R, E> ReaderTaskEither<R, E, R> ask() {
        return new ReaderTaskEither<>(new Reader<>(r -> TaskEither.right(r)));
    }

    public <B> ReaderTaskEither<R, E, B> map(Function<A, B> fn) {
        return new ReaderTaskEither<>(reader.map(te -> te.map(fn)));
    }

    public <B> ReaderTaskEither<R, E, B> flatMap(Function<A, ReaderTaskEither<R, E, B>> fn) {
        return new ReaderTaskEither<>(new Reader<>(r -> 
            reader.run().apply(r).flatMap(a -> fn.apply(a).reader().run().apply(r))
        ));
    }

    public static <R, E> Monad<Higher<Higher<µ, R>, E>> monad() {
        return new Monad<>() {
            @Override
            public <A> Higher<Higher<Higher<µ, R>, E>, A> pure(A a) { return ReaderTaskEither.right(a); }

            @Override
            public <A, B> Higher<Higher<Higher<µ, R>, E>, B> flatMap(Function<A, Higher<Higher<Higher<µ, R>, E>, B>> fn, Higher<Higher<Higher<µ, R>, E>, A> fa) {
                return narrowK(fa).flatMap(a -> narrowK(fn.apply(a)));
            }

            @Override
            public <A, B> Higher<Higher<Higher<µ, R>, E>, B> map(Function<A, B> fn, Higher<Higher<Higher<µ, R>, E>, A> fa) {
                return narrowK(fa).map(fn);
            }
        };
    }
}
