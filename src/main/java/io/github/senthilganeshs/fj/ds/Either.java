package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.typeclass.Monad;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A data structure representing a value of one of two possible types.
 */
public interface Either<A, B> extends Collection<B> {

    @SuppressWarnings("unchecked")
    static <A, B> Either<A, Collection<B>> sequence(Collection<Either<A, B>> es) {
        return es.foldl(Either.right(es.empty()), (acc, e) -> 
            acc.flatMapEither(rs -> e.map(r -> (Collection<B>) rs.build(r)))
        );
    }

    @SuppressWarnings("unchecked")
    static <A, B> Collection<B> rights(Collection<Either<A, B>> es) {
        return es.mapMaybe(e -> e.isRight() ? Maybe.some(e.fromRight(null)) : Maybe.nothing());
    }

    @SuppressWarnings("unchecked")
    static <A, B> Collection<A> lefts(Collection<Either<A, B>> es) {
        return es.mapMaybe(e -> e.isLeft() ? Maybe.some(e.fromLeft(null)) : Maybe.nothing());
    }

    static <A, B> Either<A, B> left(final A value) {
        return new Left<>(value);
    }

    static <A, B> Either<A, B> right(final B value) {
        return new Right<>(value);
    }

    /**
     * Captures an exception thrown by a supplier and wraps it in an Either.left.
     */
    static <B> Either<Throwable, B> tryCatch(CheckedSupplier<B> s) {
        try { return right(s.get()); }
        catch (Throwable t) { return left(t); }
    }

    @FunctionalInterface
    interface CheckedSupplier<T> {
        T get() throws Throwable;
    }

    default boolean isLeft() { return this instanceof Left; }
    default boolean isRight() { return this instanceof Right; }

    default A fromLeft(A def) {
        return isLeft() ? ((Left<A, B>) this).value : def;
    }

    default B fromRight(B def) {
        return isRight() ? ((Right<A, B>) this).value : def;
    }

    default B orElse(B def) {
        return fromRight(def);
    }

    default <E extends Exception> B orElseThrow(java.util.function.Supplier<E> s) throws E {
        if (isRight()) return fromRight(null);
        throw s.get();
    }

    @Override
    default <R> Collection<R> empty() {
        return (Collection<R>) left(null);
    }

    @Override
    default Collection<B> build(B input) {
        return right(input);
    }

    // --- Typeclass Instances ---

    @SuppressWarnings("unchecked")
    static <E> Monad<Collection.µ> monad() {
        return new Monad<Collection.µ>() {
            @Override public <A> Higher<Collection.µ, A> pure(A a) { return (Either<E, A>) right(a); }
            @Override public <A, B> Higher<Collection.µ, B> flatMap(Function<A, Higher<Collection.µ, B>> fn, Higher<Collection.µ, A> fa) {
                Either<E, A> src = (Either<E, A>) fa;
                return src.isRight() ? fn.apply(src.fromRight(null)) : (Either<E, B>) src;
            }
        };
    }

    @SuppressWarnings("unchecked")
    default <R> Either<A, R> map(Function<B, R> fn) {
        return isRight() ? right(fn.apply(fromRight(null))) : (Either<A, R>) this;
    }

    @SuppressWarnings("unchecked")
    default <R> Either<A, R> flatMap(Function<B, Collection<R>> fn) {
        if (isLeft()) return (Either<A, R>) this;
        Collection<R> res = fn.apply(fromRight(null));
        if (res instanceof Either) return (Either<A, R>) res;
        return (Either<A, R>) res.foldl(empty(), (acc, r) -> acc.build(r));
    }

    @SuppressWarnings("unchecked")
    default <C> Either<A, C> flatMapEither(Function<B, Either<A, C>> fn) {
        return isRight() ? fn.apply(fromRight(null)) : (Either<A, C>) this;
    }

    @SuppressWarnings("unchecked")
    default <C> Either<C, B> mapLeft(Function<A, C> fn) {
        return isLeft() ? left(fn.apply(((Left<A, B>) this).value)) : (Either<C, B>) this;
    }

    default <R> R either(Function<A, R> onLeft, Function<B, R> onRight) {
        return isLeft() ? onLeft.apply(((Left<A, B>) this).value) : onRight.apply(((Right<A, B>) this).value);
    }

    default Either<B, A> swap() {
        return either(Either::right, Either::left);
    }

    @SuppressWarnings("unchecked")
    default <C, D> Either<C, D> bimap(Function<A, C> fa, Function<B, D> fb) {
        return isLeft() ? (Either<C, D>) left(fa.apply(((Left<A, B>) this).value)) : (Either<C, D>) right(fb.apply(((Right<A, B>) this).value));
    }

    static <A, B> io.github.senthilganeshs.fj.optic.Prism<Either<A, B>, B> rightP() {
        return io.github.senthilganeshs.fj.optic.Prism.of(
            e -> e.isRight() ? Maybe.some(e.fromRight(null)) : Maybe.nothing(),
            Either::right
        );
    }

    default Maybe<B> toMaybe() {
        return isRight() ? Maybe.some(fromRight(null)) : Maybe.nothing();
    }

    @Override
    @SuppressWarnings("unchecked")
    default Collection<B> filter(Predicate<B> pred) {
        if (isLeft()) return this;
        B val = fromRight(null);
        if (pred.test(val)) return this;
        return (Collection<B>) left(val);
    }

    default Validation<List<A>, B> toValidation() {
        return isRight() ? Validation.valid(fromRight(null)) : Validation.invalid(List.of(fromLeft(null)));
    }

    final static class Left<A, B> implements Either<A, B> {
        private final A value;
        Left(A value) { this.value = value; }
        @Override public <R> R foldl(R seed, BiFunction<R, B, R> fn) { return seed; }
        @Override public String toString() { return "Left(" + value + ")"; }
        @Override public boolean equals(Object other) {
            return other instanceof Left && java.util.Objects.equals(((Left<?, ?>) other).value, value);
        }
        @Override public int hashCode() { return java.util.Objects.hashCode(value); }
        @Override public <R> Collection<R> empty() { return (Collection<R>) left(null); }
        @Override public Collection<B> build(B input) { return right(input); }
    }

    final static class Right<A, B> implements Either<A, B> {
        private final B value;
        Right(B value) { this.value = value; }
        @Override public <R> R foldl(R seed, BiFunction<R, B, R> fn) { return fn.apply(seed, value); }
        @Override public String toString() { return "Right(" + value + ")"; }
        @Override public boolean equals(Object other) {
            return other instanceof Right && java.util.Objects.equals(((Right<?, ?>) other).value, value);
        }
        @Override public int hashCode() { return java.util.Objects.hashCode(value); }
        @Override public <R> Collection<R> empty() { return (Collection<R>) left(null); }
        @Override public Collection<B> build(B input) { return right(input); }
    }
}
