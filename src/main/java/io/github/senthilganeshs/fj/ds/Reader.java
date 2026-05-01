package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.typeclass.Monad;
import java.util.function.Function;

/**
 * Represents a computation that reads from an environment of type R.
 */
public record Reader<R, A>(Function<R, A> run) implements Higher<Higher<Reader.µ, R>, A> {
    public final static class µ {}

    @SuppressWarnings("unchecked")
    public static <R, A> Reader<R, A> narrowK(Higher<Higher<µ, R>, A> hka) {
        return (Reader<R, A>) hka;
    }

    public static <R, A> Reader<R, A> pure(A a) {
        return new Reader<>(r -> a);
    }

    public static <R> Reader<R, R> ask() {
        return new Reader<>(Function.identity());
    }

    public <B> Reader<R, B> map(Function<A, B> fn) {
        return new Reader<>(r -> fn.apply(run.apply(r)));
    }

    public <B> Reader<R, B> flatMap(Function<A, Reader<R, B>> fn) {
        return new Reader<>(r -> fn.apply(run.apply(r)).run().apply(r));
    }

    public static <R> Monad<Higher<µ, R>> monad() {
        return new Monad<>() {
            @Override
            public <A> Higher<Higher<µ, R>, A> pure(A a) { return Reader.pure(a); }

            @Override
            public <A, B> Higher<Higher<µ, R>, B> flatMap(Function<A, Higher<Higher<µ, R>, B>> fn, Higher<Higher<µ, R>, A> fa) {
                return narrowK(fa).flatMap(a -> narrowK(fn.apply(a)));
            }

            @Override
            public <A, B> Higher<Higher<µ, R>, B> map(Function<A, B> fn, Higher<Higher<µ, R>, A> fa) {
                return narrowK(fa).map(fn);
            }
        };
    }
}
