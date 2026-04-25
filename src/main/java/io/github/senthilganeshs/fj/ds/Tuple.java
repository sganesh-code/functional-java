package io.github.senthilganeshs.fj.ds;

import java.util.function.Function;

/**
 * A purely functional Tuple (Pair) of two values.
 * 
 * @param <A> Type of the first element.
 * @param <B> Type of the second element.
 */
public interface Tuple<A, B> {

    Maybe<A> getA();
    Maybe<B> getB();

    @SuppressWarnings("unchecked")
    default <C, D> Tuple<C, D> bimap (final Function<A, C> fa, final Function<B, D> fb) {
        return Tuple.of(getA().map(fa).orElse(null), getB().map(fb).orElse(null));
    }

    default Tuple<B, A> swap () {
        return Tuple.of(getB().orElse(null), getA().orElse(null));
    }

    static <A, B> Tuple<A, B> of (final A a, final B b) {
        return new TupleImpl<>(Maybe.some(a), Maybe.some(b));
    }

    /**
     * Internal helper to create a tuple from Maybe values.
     */
    @SuppressWarnings("rawtypes")
    static <A, B> Tuple<A, B> fromMaybe (final Maybe<A> a, final Maybe<B> b) {
        return new TupleImpl<>(a, b);
    }

    final static class TupleImpl<A, B> implements Tuple<A, B> {

        private final Maybe<A> a;
        private final Maybe<B> b;

        TupleImpl(final Maybe<A> a, final Maybe<B> b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public Maybe<A> getA() {
            return a;
        }

        @Override
        public Maybe<B> getB() {
            return b;
        }

        @Override
        public boolean equals(final Object other) {
            if (other == null) return false;
            if (other == this) return true;
            if (other instanceof Tuple) {
                Tuple<?, ?> tOther = (Tuple<?, ?>) other;
                return tOther.getA().equals(a) && tOther.getB().equals(b);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return a.hashCode() ^ b.hashCode();
        }

        @Override
        public String toString() {
            return "(" + a.orElse(null) + "," + b.orElse(null) + ")";
        }
    }

}
