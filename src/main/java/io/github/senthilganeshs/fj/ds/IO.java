package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.typeclass.Monad;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents a synchronous side-effecting computation that is deferred.
 */
public record IO<A>(Supplier<A> run) implements Higher<IO.µ, A> {
    public final static class µ {}

    @SuppressWarnings("unchecked")
    public static <A> IO<A> narrowK(Higher<µ, A> hka) {
        return (IO<A>) hka;
    }

    public static <A> IO<A> of(Supplier<A> supplier) {
        return new IO<>(supplier);
    }

    public static <A> IO<A> pure(A a) {
        return new IO<>(() -> a);
    }

    public <B> IO<B> map(Function<A, B> fn) {
        return new IO<>(() -> fn.apply(run.get()));
    }

    public <B> IO<B> flatMap(Function<A, IO<B>> fn) {
        return new IO<>(() -> fn.apply(run.get()).run().get());
    }

    public static final Monad<µ> monad = new Monad<>() {
        @Override
        public <A> Higher<µ, A> pure(A a) { return IO.pure(a); }

        @Override
        public <A, B> Higher<µ, B> flatMap(Function<A, Higher<µ, B>> fn, Higher<µ, A> fa) {
            return narrowK(fa).flatMap(a -> narrowK(fn.apply(a)));
        }

        @Override
        public <A, B> Higher<µ, B> map(Function<A, B> fn, Higher<µ, A> fa) {
            return narrowK(fa).map(fn);
        }
    };

    /**
     * Executes the side effect.
     */
    public A unsafeRun() {
        return run.get();
    }
}
