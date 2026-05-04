package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.typeclass.Monad;
import io.github.senthilganeshs.fj.typeclass.Semigroup;
import java.util.function.Function;

/**
 * A list that is guaranteed to have at least one element.
 */
public record NonEmptyList<A>(A head, List<A> tail) implements Higher<NonEmptyList.µ, A> {
    public final static class µ {}

    @SuppressWarnings("unchecked")
    public static <A> NonEmptyList<A> narrowK(Higher<µ, A> hka) {
        return (NonEmptyList<A>) hka;
    }

    @SafeVarargs
    public static <A> NonEmptyList<A> of(A head, A... rest) {
        return new NonEmptyList<>(head, List.of(rest));
    }

    public List<A> toList() {
        return List.from(List.of(head).concat(tail));
    }

    public <B> NonEmptyList<B> map(Function<A, B> fn) {
        return new NonEmptyList<>(fn.apply(head), (List<B>) tail.map(fn));
    }

    public <B> NonEmptyList<B> flatMap(Function<A, NonEmptyList<B>> fn) {
        NonEmptyList<B> h = fn.apply(head);
        List<B> rest = List.from(tail.flatMap(a -> (Collection<B>) fn.apply(a).toList()));
        return new NonEmptyList<>(h.head, (List<B>) h.tail.concat(rest));
    }

    public static final Monad<µ> monad = new Monad<>() {
        @Override
        public <A> Higher<µ, A> pure(A a) { return new NonEmptyList<>(a, List.nil()); }

        @Override
        public <A, B> Higher<µ, B> flatMap(Function<A, Higher<µ, B>> fn, Higher<µ, A> fa) {
            return narrowK(fa).flatMap(a -> narrowK(fn.apply(a)));
        }
    };

    public static <A> Semigroup<NonEmptyList<A>> semigroup() {
        return (a, b) -> new NonEmptyList<>(a.head, List.from(a.tail.concat(b.toList())));
    }
}
