package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.typeclass.Monad;
import io.github.senthilganeshs.fj.typeclass.Applicative;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A data structure representing an optional value.
 */
public interface Maybe<T> extends Collection<T> {

    final static Maybe<Void> NOTHING = new Nothing<>();

    @SuppressWarnings("unchecked")
    static <R> Maybe<R> nothing() {
        return (Maybe<R>) NOTHING;
    }

    static <R> Maybe<R> some(final R value) {
        return new Some<>(value);
    }

    static <R> Maybe<R> of(final R value) {
        return value == null ? nothing() : some(value);
    }

    default boolean isSome() {
        return this instanceof Some;
    }

    default boolean isNothing() {
        return this instanceof Nothing;
    }

    default T orElse(T def) {
        return foldl(def, (__, t) -> t);
    }

    @Override
    default <R> Collection<R> empty() {
        return nothing();
    }

    @Override
    default Collection<T> build(T input) {
        return some(input);
    }

    // --- Typeclass Instances ---

    static final Monad<Collection.µ> monad = new Monad<Collection.µ>() {
        @Override public <A> Higher<Collection.µ, A> pure(A a) { return some(a); }
        @Override public <A, B> Higher<Collection.µ, B> flatMap(Function<A, Higher<Collection.µ, B>> fn, Higher<Collection.µ, A> fa) {
            // Use TRIAD directly here to avoid infinite recursion with ergonomic handles
            Maybe<A> src = (Maybe<A>) fa;
            return src.foldl(nothing(), (acc, a) -> (Maybe<B>) fn.apply(a));
        }
    };

    static <T> io.github.senthilganeshs.fj.optic.Prism<Maybe<T>, T> someP() {
        return io.github.senthilganeshs.fj.optic.Prism.of(
            m -> m.isSome() ? Maybe.some(m.orElse(null)) : Maybe.nothing(),
            Maybe::some
        );
    }

    @SuppressWarnings("unchecked")
    default <R> Maybe<R> flatMapMaybe(Function<T, Maybe<R>> fn) {
        return isSome() ? fn.apply(orElse(null)) : nothing();
    }

    @SuppressWarnings("unchecked")
    default <R> Maybe<R> map(Function<T, R> fn) {
        return isSome() ? some(fn.apply(orElse(null))) : nothing();
    }

    @SuppressWarnings("unchecked")
    default <R> Maybe<R> flatMap(Function<T, Collection<R>> fn) {
        if (isNothing()) return nothing();
        Collection<R> res = fn.apply(orElse(null));
        if (res instanceof Maybe) return (Maybe<R>) res;
        return (Maybe<R>) res.foldl(Maybe.<R>nothing(), (acc, r) -> (Maybe<R>) acc.build(r));
    }

    default <R> R either(java.util.function.Supplier<R> onNothing, Function<T, R> onSome) {
        return isSome() ? onSome.apply(orElse(null)) : onNothing.get();
    }

    default T fromMaybe(T def) {
        return orElse(def);
    }

    final static class Nothing<T> implements Maybe<T> {
        @Override public <R> R foldl(R seed, BiFunction<R, T, R> fn) { return seed; }
        @Override public String toString() { return "Nothing"; }
        @Override public boolean equals(Object other) { return other instanceof Nothing; }
        @Override public int hashCode() { return 0; }
        // Implement build/empty explicitly to avoid pickiness about default methods
        @Override public <R> Collection<R> empty() { return nothing(); }
        @Override public Collection<T> build(T input) { return some(input); }
    }

    final static class Some<T> implements Maybe<T> {
        private final T value;
        Some(T value) { this.value = value; }
        @Override public <R> R foldl(R seed, BiFunction<R, T, R> fn) { return fn.apply(seed, value); }
        @Override public String toString() { return "Some(" + value + ")"; }
        @Override public boolean equals(Object other) {
            return (other instanceof Some) && java.util.Objects.equals(((Some<?>) other).value, value);
        }
        @Override public int hashCode() { return java.util.Objects.hashCode(value); }
        @Override public <R> Collection<R> empty() { return nothing(); }
        @Override public Collection<T> build(T input) { return some(input); }
    }
}
