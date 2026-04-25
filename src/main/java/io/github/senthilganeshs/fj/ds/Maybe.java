package io.github.senthilganeshs.fj.ds;

import java.util.function.BiFunction;


public interface Maybe<T> extends Collection<T> {

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

    default <R> Maybe<R> safeCast(Class<R> clazz) {
        return filter(clazz::isInstance).map(clazz::cast);
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