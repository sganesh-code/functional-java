package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.optic.Prism;
import io.github.senthilganeshs.fj.typeclass.Monad;
import io.github.senthilganeshs.fj.typeclass.Traversable;
import io.github.senthilganeshs.fj.typeclass.Applicative;
import java.util.function.BiFunction;
import java.util.function.Function;


public interface Maybe<T> extends Collection<T>, Higher<Maybe.µ, T> {

    /**
     * Witness type for Higher-Kinded Type encoding.
     */
    final class µ {}

    /**
     * Safely downcasts a Higher-Kinded Type to a Maybe.
     */
    @SuppressWarnings("unchecked")
    static <T> Maybe<T> narrowK(Higher<µ, T> hka) {
        return (Maybe<T>) hka;
    }

    final static Maybe<Void> NOTHING = new Nothing<>();

    @SuppressWarnings("unchecked")
    public static <R> Maybe<R> nothing () {
        return (Maybe<R>) NOTHING;
    }

    public static <R> Maybe<R> some (final R value) {
        return new Some<>(value);
    }

    default boolean isSome() {
        return this instanceof Some;
    }

    default boolean isNothing() {
        return this instanceof Nothing;
    }

    /**
     * @deprecated Use {@link #orElse(Object)} instead.
     */
    @Deprecated
    default T fromMaybe(T def) {
        return foldl(def, (__, t) -> t);
    }

    /**
     * Returns the contained value if present, otherwise returns the provided default.
     * 
     * @param def The fallback value.
     * @return The value or def.
     */
    default T orElse(T def) {
        return fromMaybe(def);
    }

    /**
     * Returns the contained value if present, otherwise returns the result of the supplier.
     * 
     * @param supplier The supplier of the fallback value.
     * @return The value or supplier result.
     */
    default T orElseGet(java.util.function.Supplier<? extends T> supplier) {
        return isSome() ? fromMaybe(null) : supplier.get();
    }

    /**
     * Returns the contained value if present, otherwise throws the exception from the supplier.
     * 
     * @param <X> Type of the exception.
     * @param exceptionSupplier The supplier of the exception.
     * @return The value.
     * @throws X if no value is present.
     */
    default <X extends Throwable> T orElseThrow(java.util.function.Supplier<? extends X> exceptionSupplier) throws X {
        if (isSome()) return fromMaybe(null);
        throw exceptionSupplier.get();
    }

    @SuppressWarnings("unchecked")
    @Override
    default <R> Maybe<R> map(java.util.function.Function<T, R> fn) {
        return isSome() ? Maybe.some(fn.apply(orElse(null))) : Maybe.nothing();
    }

    @SuppressWarnings("unchecked")
    @Override
    default Maybe<T> filter(java.util.function.Predicate<T> pred) {
        return (isSome() && pred.test(orElse(null))) ? this : Maybe.nothing();
    }

    @SuppressWarnings("unchecked")
    default <R> Maybe<R> flatMapMaybe(java.util.function.Function<T, Maybe<R>> fn) {
        return isSome() ? fn.apply(orElse(null)) : Maybe.nothing();
    }

    default <R> Maybe<R> safeCast(Class<R> clazz) {
        return filter(clazz::isInstance).map(clazz::cast);
    }

    /**
     * Returns a Prism that focuses on the value inside a Some.
     */
    static <T> Prism<Maybe<T>, T> someP() {
        return Prism.of(m -> m, Maybe::some);
    }
    
    // --- Typeclass Instances ---

    Monad<µ> monad = new Monad<>() {
        @Override
        public <A> Higher<µ, A> pure(A a) { return Maybe.some(a); }

        @Override
        public <A, B> Higher<µ, B> flatMap(Function<A, Higher<µ, B>> fn, Higher<µ, A> fa) {
            return Maybe.narrowK(fa).flatMapMaybe(a -> Maybe.narrowK(fn.apply(a)));
        }

        @Override
        public <A, B> Higher<µ, B> map(Function<A, B> fn, Higher<µ, A> fa) {
            return Maybe.narrowK(fa).map(fn);
        }
    };

    Traversable<µ> traversable = new Traversable<>() {
        @Override
        public <G, A, B> Higher<G, Higher<µ, B>> traverse(Applicative<G> app, Function<A, Higher<G, B>> fn, Higher<µ, A> fa) {
            Maybe<A> ma = Maybe.narrowK(fa);
            return ma.either(
                () -> app.pure(Maybe.nothing()),
                a -> app.map(Maybe::some, fn.apply(a))
            );
        }

        @Override
        public <A, B> Higher<µ, B> map(Function<A, B> fn, Higher<µ, A> fa) {
            return Maybe.narrowK(fa).map(fn);
        }
    };

    /**
     * Applies a function if Some, or a default supplier if Nothing.
     */
    default <R> R either(java.util.function.Supplier<R> onNothing, Function<T, R> onSome) {
        return isSome() ? onSome.apply(orElse(null)) : onNothing.get();
    }

    final static class Nothing<T> implements Maybe<T> {

        @Override
        public <R> Maybe<R> empty() {
            return Maybe.nothing();
        }

        @Override
        public Maybe<T> build(T input) {
            return Maybe.some(input);
        }

        @Override
        public <R> R foldl(R seed, BiFunction<R, T, R> fn) {
            return seed;
        }

        @Override
        public String toString() {
            return "Nothing";
        }
        
        @Override
        public boolean equals(final Object other) {
            if (other == null) return false;
            if (other == this) return true;
            if (other instanceof Nothing) {
                return true;
            }
            return false;
        }
        @Override
        public int hashCode() {
            return 0;
        }
    }
    
    final static class Some<T> implements Maybe<T> {

        private T value;

        Some (final T value) {
            this.value = value;
        }

        @Override
        public <R> Maybe<R> empty() {
            return Maybe.nothing();
        }

        @Override
        public Maybe<T> build(T input) {
            return new Some<>(input);
        }

        @Override
        public <R> R foldl(R seed, BiFunction<R, T, R> fn) {
            return fn.apply(seed, value);
        }

        @Override
        public String toString() {
            return "Some (" + value + ")";
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(final Object other) {
            if (other == null) return false;
            if (other == this) return true;
            if (other instanceof Some) {
                Some<T> sOther = (Some<T>)other;
                return sOther.value.equals(value);
            }
            return false;
        }
        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }
 }
