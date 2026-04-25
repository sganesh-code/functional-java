package io.github.senthilganeshs.fj.ds;

import java.util.function.BiFunction;
import java.util.function.Function;

public interface Tuple<A, B> {

    <R> Collection<R> map (final BiFunction<A, B, R> fn);

    static <A, B> Tuple<A, B> of(A a, B b) {
        return new TupleImpl<>(a, b);
    }

    static <A, B> Tuple<A, B> of() {
        return new TupleImpl<>();
    }

    Maybe<A> getA();
    Maybe<B> getB();

    default <C, D> Tuple<C, D> bimap (final Function<A, C> fa, final Function<B, D> fb) {
        return Tuple.of(((Maybe<C>)getA().map(fa)).fromMaybe(null), ((Maybe<D>)getB().map(fb)).fromMaybe(null));
    }

    default Tuple<B, A> swap () {
        return Tuple.of(getB().fromMaybe(null), getA().fromMaybe(null));
    }

    class TupleImpl<A, B> implements Tuple<A, B> {
        private final Maybe<A> a;
        private final Maybe<B> b;

        TupleImpl(A a, B b) {
            this.a = Maybe.some(a);
            this.b = Maybe.some(b);
        }

        TupleImpl() {
            this.a = Maybe.nothing();
            this.b = Maybe.nothing();
        }

        @Override
        public <R> Collection<R> map(BiFunction<A, B, R> fn) {
            return this.a.flatMap(aa -> b.map(bb -> fn.apply(aa, bb)));
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
        public boolean equals(Object other) {
            if (other == null) return false;
            if (other == this) return true;
            if (!(other instanceof Tuple)) return false;
            Tuple<?, ?> o = (Tuple<?, ?>) other;
            return a.equals(o.getA()) && b.equals(o.getB());
        }

        @Override
        public int hashCode() {
            return a.hashCode() ^ b.hashCode();
        }

        @Override
        public String toString() {
            return "(" + a.fromMaybe(null) + "," + b.fromMaybe(null) + ")";
        }
    }

}
