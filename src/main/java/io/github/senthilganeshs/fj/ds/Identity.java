package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.typeclass.Monad;
import java.util.function.Function;

/**
 * The identity functor, which simply wraps a value.
 */
public record Identity<A>(A value) implements Higher<Identity.µ, A> {
    public final static class µ {}

    @SuppressWarnings("unchecked")
    public static <A> Identity<A> narrowK(Higher<µ, A> hka) {
        return (Identity<A>) hka;
    }

    public <B> Identity<B> map(Function<A, B> fn) {
        return new Identity<>(fn.apply(value));
    }

    public <B> Identity<B> flatMap(Function<A, Identity<B>> fn) {
        return fn.apply(value);
    }

    public static final Monad<µ> monad = new Monad<>() {
        @Override
        public <A> Higher<µ, A> pure(A a) { return new Identity<>(a); }

        @Override
        public <A, B> Higher<µ, B> flatMap(Function<A, Higher<µ, B>> fn, Higher<µ, A> fa) {
            return narrowK(fa).flatMap(a -> narrowK(fn.apply(a)));
        }

        @Override
        public <A, B> Higher<µ, B> map(Function<A, B> fn, Higher<µ, A> fa) {
            return narrowK(fa).map(fn);
        }
    };
}
