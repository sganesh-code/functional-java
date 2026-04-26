package io.github.senthilganeshs.fj.optic;

import io.github.senthilganeshs.fj.ds.Maybe;
import java.util.function.Function;

/**
 * An optic that represents a lossless, two-way transformation between types S and A.
 */
public interface Iso<S, A> {
    A get(S s);
    S reverseGet(A a);

    /**
     * Flips the isomorphism.
     */
    default Iso<A, S> reverse() {
        return new Iso<A, S>() {
            @Override public S get(A a) { return Iso.this.reverseGet(a); }
            @Override public A reverseGet(S s) { return Iso.this.get(s); }
        };
    }

    /**
     * Composes this iso with another iso.
     */
    default <B> Iso<S, B> compose(Iso<A, B> other) {
        return new Iso<S, B>() {
            @Override public B get(S s) { return other.get(Iso.this.get(s)); }
            @Override public S reverseGet(B b) { return Iso.this.reverseGet(other.reverseGet(b)); }
        };
    }

    /**
     * Composes this iso with a lens.
     */
    default <B> Lens<S, B> compose(Lens<A, B> other) {
        return asLens().compose(other);
    }

    /**
     * Composes this iso with a prism.
     */
    default <B> Prism<S, B> compose(Prism<A, B> other) {
        return asPrism().compose(other);
    }

    /**
     * Views this isomorphism as a Lens.
     */
    default Lens<S, A> asLens() {
        return Lens.of(this::get, (a, __) -> reverseGet(a));
    }

    /**
     * Views this isomorphism as a Prism.
     */
    default Prism<S, A> asPrism() {
        return Prism.of(s -> Maybe.some(get(s)), this::reverseGet);
    }

    static <S, A> Iso<S, A> of(Function<S, A> get, Function<A, S> reverseGet) {
        return new Iso<S, A>() {
            @Override public A get(S s) { return get.apply(s); }
            @Override public S reverseGet(A a) { return reverseGet.apply(a); }
        };
    }
}
