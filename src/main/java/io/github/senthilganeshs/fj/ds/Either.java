package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.optic.Prism;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Represents a value of one of two possible types (a disjoint union).
 * 
 * <p>An instance of Either is either an instance of {@link Left} or {@link Right}.
 * By convention, {@code Left} is used for failure and {@code Right} is used for success.</p>
 * 
 * @param <A> The type of the Left value (usually representing an error).
 * @param <B> The type of the Right value (usually representing a success).
 */
public interface Either<A, B> extends Collection<B> {

    @Override Either<A,B> build (final B value);
    
    /**
     * Applies either the left function or the right function to the contained value.
     * 
     * @param <R> The resulting type.
     * @param fa Function to apply if this is Left.
     * @param fb Function to apply if this is Right.
     * @return The result of applying the appropriate function.
     */
    <R> R either (final Function<A, R> fa, final Function<B, R> fb);
    
    /**
     * Creates a Left instance.
     * 
     * @param <P> Left type.
     * @param <Q> Right type.
     * @param value The value.
     * @return A Left Either.
     */
    public static <P, Q> Either <P, Q> left (final P value) {
        return new Left<>(value);
    }

    /**
     * Creates a Right instance.
     * 
     * @param <P> Left type.
     * @param <Q> Right type.
     * @param value The value.
     * @return A Right Either.
     */
    public static <P, Q> Either <P, Q> right (final Q value) {
        return new Right<>(value);
    }

    /**
     * Extracts all Left values from a collection of Eithers.
     * 
     * @param <A> Left type.
     * @param <B> Right type.
     * @param es Collection of Eithers.
     * @return Collection of Left values.
     */
    @SuppressWarnings("unchecked")
    public static <A, B> Collection<A> lefts (final Collection<Either<A, B>> es) {
        return es.mapMaybe(e -> e.isLeft() ? Maybe.some(e.fromLeft(null)) : Maybe.nothing());
    }

    /**
     * Extracts all Right values from a collection of Eithers.
     * 
     * @param <A> Left type.
     * @param <B> Right type.
     * @param es Collection of Eithers.
     * @return Collection of Right values.
     */
    @SuppressWarnings("unchecked")
    public static <A, B> Collection<B> rights (final Collection<Either<A, B>> es) {
        return es.mapMaybe(e -> e.isRight() ? Maybe.some(e.orElse(null)) : Maybe.nothing());
    }

    /**
     * @deprecated Use {@link #orElse(Object)} instead.
     */
    @Deprecated
    default B fromRight (final B def) {
        return either (a -> def, b -> b);
    }

    /**
     * Returns the Right value if present, otherwise returns the provided default.
     * 
     * @param def The fallback value.
     * @return The Right value or def.
     */
    default B orElse (final B def) {
        return fromRight(def);
    }

    /**
     * Converts the Either into a Maybe.
     */
    default Maybe<B> toMaybe() {
        return isRight() ? Maybe.some(orElse(null)) : Maybe.nothing();
    }

    /**
     * Converts the Either into a Validation.
     */
    default Validation<A, B> toValidation() {
        return either(Validation::invalid, Validation::valid);
    }

    /**
     * Returns a Prism that focuses on the Right (success) value.
     */
    static <L, R> Prism<Either<L, R>, R> rightP() {
        return Prism.of(e -> e.isRight() ? Maybe.some(e.orElse(null)) : Maybe.nothing(), Either::right);
    }

    /**
     * Returns a Prism that focuses on the Left (error) value.
     */
    static <L, R> Prism<Either<L, R>, L> leftP() {
        return Prism.of(e -> e.isLeft() ? Maybe.some(e.fromLeft(null)) : Maybe.nothing(), Either::left);
    }

    final static class Left <A, B> implements Either <A, B> {
        private final A value;

        Left (final A value) {
            this.value = value;
        }
        
        @Override
        public <R> Collection<R> empty() {
            return Maybe.nothing();
        }

        @Override
        public <R> R foldl(R seed, BiFunction<R, B, R> fn) {
            return seed;
        }

        @Override
        public Either<A, B> build(B value) {
            return right(value);
        }

        @Override
        public <R> R either(Function<A, R> fa, Function<B, R> fb) {
            return fa.apply(value);
        }
        
        @Override
        public String toString() {
            return "Left " + value;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(final Object other) {
            if (other == null) return false;
            if (other == this) return true;
            if (other instanceof Left) {
                Left<A, B> lOther = ((Left<A, B>) other);
                return lOther.value.equals(value);
            }
            return false;
        }
        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }
    
    final static class Right<A, B> implements Either <A, B> {

        private final B value;

        Right (final B value) {
            this.value = value;
        }
        
        @Override
        public <R> Collection<R> empty() {
            return Maybe.nothing();
        }

        @Override
        public <R> R foldl(R seed, BiFunction<R, B, R> fn) {
            return fn.apply(seed, value);
        }

        @Override
        public Either<A, B> build(B value) {
            return right(value);
        }

        @Override
        public <R> R either(Function<A, R> fa, Function<B, R> fb) {
            return fb.apply(value);
        }
        
        @Override
        public String toString() {
            return "Right " + value;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(final Object other) {
            if (other == null) return false;
            if (other == this) return true;
            if (other instanceof Right) {
                Right<A, B> rOther = ((Right<A,B>) other);
                return rOther.value.equals(value);
            }
            return false;
        }
        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }
}
