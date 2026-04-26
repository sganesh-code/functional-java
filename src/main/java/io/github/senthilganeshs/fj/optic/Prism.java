package io.github.senthilganeshs.fj.optic;

import io.github.senthilganeshs.fj.ds.Collection;
import io.github.senthilganeshs.fj.ds.List;
import io.github.senthilganeshs.fj.ds.Maybe;
import java.util.function.Function;

/**
 * An optic that focuses on a case or a potentially missing field.
 */
public interface Prism<S, A> extends AffineTraversal<S, A> {
    Maybe<A> getMaybe(S source);
    S reverseGet(A value);

    @Override
    default S set(A value, S source) {
        return modify(source, __ -> value);
    }

    @Override
    default S modify(S source, Function<A, A> fn) {
        return getMaybe(source).map(a -> reverseGet(fn.apply(a))).orElse(source);
    }

    /**
     * Composes this prism with another prism.
     */
    default <B> Prism<S, B> compose(Prism<A, B> other) {
        return new Prism<S, B>() {
            @Override public Maybe<B> getMaybe(S s) {
                return (Maybe<B>) Prism.this.getMaybe(s).flatMapMaybe(other::getMaybe);
            }
            @Override public S reverseGet(B b) {
                return Prism.this.reverseGet(other.reverseGet(b));
            }
        };
    }

    /**
     * Composes this prism with a traversal.
     */
    default <B> Traversal<S, B> compose(Traversal<A, B> other) {
        return new Traversal<S, B>() {
            @Override public Collection<B> getAll(S s) {
                return Prism.this.getMaybe(s).map(other::getAll).orElse(List.nil());
            }
            @Override public S modify(S s, java.util.function.Function<B, B> fn) {
                return Prism.this.modify(s, a -> other.modify(a, fn));
            }
        };
    }

    /**
     * Composes this prism with an affine traversal.
     */
    @Override
    default <B> AffineTraversal<S, B> compose(AffineTraversal<A, B> other) {
        return new AffineTraversal<S, B>() {
            @Override public Maybe<B> getMaybe(S s) {
                return (Maybe<B>) Prism.this.getMaybe(s).flatMapMaybe(other::getMaybe);
            }
            @Override public S set(B b, S s) {
                return Prism.this.modify(s, a -> other.set(b, a));
            }
        };
    }

    /**
     * Views this prism as a read-only Fold.
     */
    @SuppressWarnings("unchecked")
    default Fold<S, A> asFold() {
        return s -> (Collection<A>) getMaybe(s).foldl((Collection<A>) List.<A>nil(), Collection::build);
    }

    /**
     * Composes this prism with a getter.
     */
    @Override
    @SuppressWarnings("unchecked")
    default <B> Fold<S, B> compose(Getter<A, B> other) {
        return s -> (Collection<B>) getMaybe(s).map(other::get).foldl((Collection<B>) List.<B>nil(), Collection::build);
    }

    /**
     * Composes this prism with a fold.
     */
    default <B> Fold<S, B> compose(Fold<A, B> other) {
        return s -> getMaybe(s).map(other::getAll).orElse(List.nil());
    }

    static <S, A> Prism<S, A> of(Function<S, Maybe<A>> preview, Function<A, S> review) {
        return new Prism<S, A>() {
            @Override public Maybe<A> getMaybe(S s) { return preview.apply(s); }
            @Override public S reverseGet(A a) { return review.apply(a); }
        };
    }
}
