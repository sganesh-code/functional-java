package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.typeclass.Applicative;
import io.github.senthilganeshs.fj.typeclass.Functor;
import io.github.senthilganeshs.fj.typeclass.Monoid;
import java.util.function.Function;

/**
 * The constant functor, which holds a value of type A and ignores its second type argument B.
 * 
 * @param <A> The type of the constant value.
 * @param <B> The phantom type argument.
 */
public record Const<A, B>(A value) implements Higher<Higher<Const.µ, A>, B> {
    public final static class µ {}

    @SuppressWarnings("unchecked")
    public static <A, B> Const<A, B> narrowK(Higher<Higher<µ, A>, B> hka) {
        return (Const<A, B>) hka;
    }

    public static <A> Functor<Higher<µ, A>> functor() {
        return new Functor<>() {
            @Override
            public <B, C> Higher<Higher<µ, A>, C> map(Function<B, C> fn, Higher<Higher<µ, A>, B> fa) {
                return new Const<>(narrowK(fa).value());
            }
        };
    }

    public static <A> Applicative<Higher<µ, A>> applicative(Monoid<A> monoid) {
        return new Applicative<>() {
            @Override
            public <B> Higher<Higher<µ, A>, B> pure(B b) {
                return new Const<>(monoid.empty());
            }

            @Override
            public <B, C> Higher<Higher<µ, A>, C> ap(Higher<Higher<µ, A>, Function<B, C>> ff, Higher<Higher<µ, A>, B> fa) {
                return new Const<>(monoid.combine(narrowK(ff).value(), narrowK(fa).value()));
            }
        };
    }
}
