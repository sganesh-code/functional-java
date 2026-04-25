package io.github.senthilganeshs.fj.ds;

import java.util.function.Function;

/**
 * An optic that focuses on a case or a potentially missing field.
 */
public interface Prism<S, A> {
    Maybe<A> getMaybe(S source);
    S reverseGet(A value);

    default S set(A value, S source) {
        return modify(source, __ -> value);
    }

    default S modify(S source, Function<A, A> fn) {
        return getMaybe(source).map(a -> reverseGet(fn.apply(a))).orElse(source);
    }

    /**
     * Composes this prism with another prism.
     */
    default <B> Prism<S, B> compose(Prism<A, B> other) {
        return new Prism<S, B>() {
            @Override public Maybe<B> getMaybe(S s) {
                return (Maybe<B>) Prism.this.getMaybe(s).flatMap(other::getMaybe);
            }
            @Override public S reverseGet(B b) {
                return Prism.this.reverseGet(other.reverseGet(b));
            }
        };
    }

    static <S, A> Prism<S, A> of(Function<S, Maybe<A>> preview, Function<A, S> review) {
        return new Prism<S, A>() {
            @Override public Maybe<A> getMaybe(S s) { return preview.apply(s); }
            @Override public S reverseGet(A a) { return review.apply(a); }
        };
    }
}
