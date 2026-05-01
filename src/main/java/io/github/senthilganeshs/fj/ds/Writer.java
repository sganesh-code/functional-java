package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.typeclass.Monad;
import io.github.senthilganeshs.fj.typeclass.Monoid;
import java.util.function.Function;

/**
 * Represents a computation that produces a value of type A and a log of type W.
 * Requires W to be a Monoid for log accumulation.
 */
public record Writer<W, A>(A value, W log) implements Higher<Higher<Writer.µ, W>, A> {
    public final static class µ {}

    @SuppressWarnings("unchecked")
    public static <W, A> Writer<W, A> narrowK(Higher<Higher<µ, W>, A> hka) {
        return (Writer<W, A>) hka;
    }

    public static <W, A> Writer<W, A> pure(A a, Monoid<W> monoid) {
        return new Writer<>(a, monoid.empty());
    }

    public static <W> Writer<W, Void> tell(W log) {
        return new Writer<>(null, log);
    }

    public <B> Writer<W, B> map(Function<A, B> fn) {
        return new Writer<>(fn.apply(value), log);
    }

    public <B> Writer<W, B> flatMap(Function<A, Writer<W, B>> fn, Monoid<W> monoid) {
        Writer<W, B> next = fn.apply(value);
        return new Writer<>(next.value(), monoid.combine(log, next.log()));
    }

    public static <W> Monad<Higher<µ, W>> monad(Monoid<W> monoid) {
        return new Monad<>() {
            @Override
            public <A> Higher<Higher<µ, W>, A> pure(A a) { return Writer.pure(a, monoid); }

            @Override
            public <A, B> Higher<Higher<µ, W>, B> flatMap(Function<A, Higher<Higher<µ, W>, B>> fn, Higher<Higher<µ, W>, A> fa) {
                return narrowK(fa).flatMap(a -> narrowK(fn.apply(a)), monoid);
            }

            @Override
            public <A, B> Higher<Higher<µ, W>, B> map(Function<A, B> fn, Higher<Higher<µ, W>, A> fa) {
                return narrowK(fa).map(fn);
            }
        };
    }
}
