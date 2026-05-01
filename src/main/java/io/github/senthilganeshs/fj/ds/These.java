package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.typeclass.Bifunctor;
import io.github.senthilganeshs.fj.typeclass.Monad;
import io.github.senthilganeshs.fj.typeclass.Semigroup;
import java.util.function.Function;

/**
 * Represents values that can be either A, B, or Both A and B.
 */
public sealed interface These<A, B> extends Higher<Higher<These.µ, A>, B> {
    final class µ {}

    @SuppressWarnings("unchecked")
    static <A, B> These<A, B> narrowK(Higher<Higher<µ, A>, B> hka) {
        return (These<A, B>) hka;
    }

    record This<A, B>(A value) implements These<A, B> {}
    record That<A, B>(B value) implements These<A, B> {}
    record Both<A, B>(A a, B b) implements These<A, B> {}

    default <R> R fold(Function<A, R> onThis, Function<B, R> onThat, java.util.function.BiFunction<A, B, R> onBoth) {
        if (this instanceof This<A, B> t) return onThis.apply(t.value);
        if (this instanceof That<A, B> t) return onThat.apply(t.value);
        if (this instanceof Both<A, B> b) return onBoth.apply(b.a, b.b);
        throw new IllegalStateException();
    }

    static <A, B> These<A, B> left(A a) { return new This<>(a); }
    static <A, B> These<A, B> right(B b) { return new That<>(b); }
    static <A, B> These<A, B> both(A a, B b) { return new Both<>(a, b); }

    static <A> Monad<Higher<µ, A>> monad(Semigroup<A> semigroup) {
        return new Monad<>() {
            @Override
            public <B> Higher<Higher<µ, A>, B> pure(B b) { return right(b); }

            @Override
            public <B, C> Higher<Higher<µ, A>, C> flatMap(Function<B, Higher<Higher<µ, A>, C>> fn, Higher<Higher<µ, A>, B> fa) {
                These<A, B> ta = narrowK(fa);
                return ta.fold(
                    a -> left(a),
                    b -> narrowK(fn.apply(b)),
                    (a1, b) -> {
                        These<A, C> tc = narrowK(fn.apply(b));
                        return tc.fold(
                            a2 -> left(semigroup.combine(a1, a2)),
                            c -> both(a1, c),
                            (a2, c) -> both(semigroup.combine(a1, a2), c)
                        );
                    }
                );
            }
        };
    }

    Bifunctor<µ> bifunctor = new Bifunctor<>() {
        @Override
        public <A, B, C, D> Higher<Higher<µ, C>, D> bimap(Function<A, C> fa, Function<B, D> fb, Higher<Higher<µ, A>, B> h) {
            These<A, B> t = narrowK(h);
            return t.fold(
                a -> left(fa.apply(a)),
                b -> right(fb.apply(b)),
                (a, b) -> both(fa.apply(a), fb.apply(b))
            );
        }
    };
}
