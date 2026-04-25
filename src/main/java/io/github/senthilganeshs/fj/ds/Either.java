package io.github.senthilganeshs.fj.ds;

import java.util.function.BiFunction;
import java.util.function.Function;

public interface Either<A, B> extends Collection<B> {

    @Override Either<A,B> build (final B value);
    
    <R> R either (final Function<A, R> fa, final Function<B, R> fb);
    
    public static <P, Q> Either <P, Q> left (final P value) {
        return new Left<>(value);
    }
    
    public static <P, Q> Either <P, Q> right (final Q value) {
        return new Right<>(value);
    }

    public static <A, B> Collection<A> lefts (final Collection<Either<A, B>> es) {
        return es.foldl (es.empty(), 
            (rs, t) -> t.either(a -> rs.build(a), b -> rs));
    }
    
    public static <A, B> Collection<B> rights (final Collection<Either<A, B>> es) {
        return es.foldl(es.empty(), 
            (rs, t) -> t.either(a -> rs, b -> rs.build(b)));
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
     * Returns the Right value if present, otherwise returns the result of the supplier.
     * 
     * @param supplier The supplier of the fallback value.
     * @return The Right value or supplier result.
     */
    default B orElseGet(java.util.function.Supplier<? extends B> supplier) {
        return isRight() ? fromRight(null) : supplier.get();
    }

    /**
     * Returns the Right value if present, otherwise throws the exception from the supplier.
     * 
     * @param <X> Type of the exception.
     * @param exceptionSupplier The supplier of the exception.
     * @return The Right value.
     * @throws X if this is a Left.
     */
    default <X extends Throwable> B orElseThrow(java.util.function.Supplier<? extends X> exceptionSupplier) throws X {
        if (isRight()) return fromRight(null);
        throw exceptionSupplier.get();
    }
    
    default A fromLeft (final A def) {
        return either(a -> a, b -> def);
    }
    
    default boolean isLeft () {
        return either (a -> true, b -> false);
    }
    
    default boolean isRight() {
        return either (a -> false, b -> true);
    }
    
    @Override
    default <R> Either<A, R> map(Function<B, R> fn) {
        return (Either<A, R>) Collection.super.map(fn);
    }

    @Override
    default <R> Collection<R> flatMap(Function<B, Collection<R>> fn) {
        return either(Either::left, fn::apply);
    }

    default <C, D> Either<C, D> bimap (final Function<A, C> fa, final Function<B, D> fb) {
        return either(a -> left(fa.apply(a)), b -> right(fb.apply(b)));
    }

    default Either<B, A> swap () {
        return either(Either::right, Either::left);
    }

    final static class Left <A, B> implements Either <A, B> {
        private final A value;

        Left (final A value) {
            this.value = value;
        }

        @Override
        public <R> Collection<R> empty() {
            return new Left<>(value);
        }

        @Override
        public <R> R foldl(final R seed, final BiFunction<R, B, R> fn) {
            return seed;
        }

        @Override
        public Either<A, B> build(final B value) {
            return new Right<>(value);
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
            return new Left<>(value);
        }

        @Override
        public <R> R foldl(final R seed, final BiFunction<R, B, R> fn) {
            return fn.apply(seed, value);
        }

        @Override
        public Either<A, B> build(final B value) {
            //lose the old value.
            return new Right<>(value);
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
        public boolean equals (final Object other) {
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