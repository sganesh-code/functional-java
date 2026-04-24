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

    default T fromMaybe(T def) {
        return foldl(def, (__, t) -> t);
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