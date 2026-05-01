package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.typeclass.Monad;
import java.util.function.Function;

/**
 * Represents a computation that reads and writes to a state of type S.
 */
public record State<S, A>(Function<S, Tuple<S, A>> run) implements Higher<Higher<State.µ, S>, A> {
    public final static class µ {}

    @SuppressWarnings("unchecked")
    public static <S, A> State<S, A> narrowK(Higher<Higher<µ, S>, A> hka) {
        return (State<S, A>) hka;
    }

    public static <S, A> State<S, A> pure(A a) {
        return new State<>(s -> Tuple.of(s, a));
    }

    public static <S> State<S, S> get() {
        return new State<>(s -> Tuple.of(s, s));
    }

    public static <S> State<S, Void> set(S s) {
        return new State<>(__ -> Tuple.of(s, null));
    }

    public static <S> State<S, Void> modify(Function<S, S> fn) {
        return new State<>(s -> Tuple.of(fn.apply(s), null));
    }

    public <B> State<S, B> map(Function<A, B> fn) {
        return new State<>(s -> {
            Tuple<S, A> res = run.apply(s);
            return Tuple.of(res.getA().orElse(null), fn.apply(res.getB().orElse(null)));
        });
    }

    public <B> State<S, B> flatMap(Function<A, State<S, B>> fn) {
        return new State<>(s -> {
            Tuple<S, A> res = run.apply(s);
            return fn.apply(res.getB().orElse(null)).run().apply(res.getA().orElse(null));
        });
    }

    public static <S> Monad<Higher<µ, S>> monad() {
        return new Monad<>() {
            @Override
            public <A> Higher<Higher<µ, S>, A> pure(A a) { return State.pure(a); }

            @Override
            public <A, B> Higher<Higher<µ, S>, B> flatMap(Function<A, Higher<Higher<µ, S>, B>> fn, Higher<Higher<µ, S>, A> fa) {
                return narrowK(fa).flatMap(a -> narrowK(fn.apply(a)));
            }

            @Override
            public <A, B> Higher<Higher<µ, S>, B> map(Function<A, B> fn, Higher<Higher<µ, S>, A> fa) {
                return narrowK(fa).map(fn);
            }
        };
    }
}
