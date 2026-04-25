package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.typeclass.Semigroup;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A data type that represents either a success (Valid) or an accumulation of errors (Invalid).
 * Unlike Either, Validation does not short-circuit on the first error; it accumulates them
 * using a Semigroup.
 * 
 * @param <E> The type of errors.
 * @param <T> The type of the valid value.
 */
public interface Validation<E, T> extends Collection<T> {

    boolean isValid();

    /**
     * Safely returns the valid value or a default.
     */
    default T orElse(T def) {
        return isValid() ? foldl(null, (__, t) -> t) : def;
    }

    /**
     * Combines this validation with another. If both are invalid, their errors are combined
     * using the provided Semigroup.
     */
    @SuppressWarnings("unchecked")
    default <R, S> Validation<E, S> liftA2(BiFunction<T, R, S> fn, Validation<E, R> other, Semigroup<E> semigroup) {
        if (this.isValid() && other.isValid()) {
            return valid(fn.apply(this.orElse(null), other.orElse(null)));
        }
        if (!this.isValid() && !other.isValid()) {
            E e1 = ((Invalid<E, T>) this).error;
            E e2 = ((Invalid<E, R>) other).error;
            return invalid(semigroup.combine(e1, e2));
        }
        return !this.isValid() ? (Validation<E, S>) this : (Validation<E, S>) other;
    }

    static <E, T> Validation<E, T> valid(T value) {
        return new Valid<>(value);
    }

    static <E, T> Validation<E, T> invalid(E error) {
        return new Invalid<>(error);
    }

    final static class Valid<E, T> implements Validation<E, T> {
        private final T value;

        Valid(T value) { this.value = value; }

        @Override public boolean isValid() { return true; }
        @Override public <R> Collection<R> empty() { return Maybe.nothing(); }
        @Override public Collection<T> build(T input) { return new Valid<>(input); }
        @Override public <R> R foldl(R seed, BiFunction<R, T, R> fn) { return fn.apply(seed, value); }
        
        @Override public String toString() { return "Valid(" + value + ")"; }
        @Override public boolean equals(Object other) {
            return other instanceof Valid && ((Valid<?, ?>)other).value.equals(value);
        }
        @Override public int hashCode() { return value.hashCode(); }
    }

    final static class Invalid<E, T> implements Validation<E, T> {
        private final E error;

        Invalid(E error) { this.error = error; }

        @Override public boolean isValid() { return false; }
        @Override public <R> Collection<R> empty() { return Maybe.nothing(); }
        @Override public Collection<T> build(T input) { return valid(input); }
        @Override public <R> R foldl(R seed, BiFunction<R, T, R> fn) { return seed; }
        
        @Override public String toString() { return "Invalid(" + error + ")"; }
        @Override public boolean equals(Object other) {
            return other instanceof Invalid && ((Invalid<?, ?>)other).error.equals(error);
        }
        @Override public int hashCode() { return error.hashCode(); }
    }
}
