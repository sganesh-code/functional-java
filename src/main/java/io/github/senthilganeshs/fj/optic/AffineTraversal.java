package io.github.senthilganeshs.fj.optic;

import io.github.senthilganeshs.fj.ds.Collection;
import io.github.senthilganeshs.fj.ds.List;
import io.github.senthilganeshs.fj.ds.Maybe;
import java.util.function.Function;

/**
 * An optic that focuses on at most one element.
 * Effectively a combination of a Lens and a Prism.
 */
public interface AffineTraversal<S, A> {
    Maybe<A> getMaybe(S source);
    S set(A value, S source);

    default S modify(S source, Function<A, A> fn) {
        return getMaybe(source).map(a -> set(fn.apply(a), source)).orElse(source);
    }

    /**
     * Composes this with a lens.
     */
    default <B> AffineTraversal<S, B> compose(Lens<A, B> other) {
        return new AffineTraversal<S, B>() {
            @Override public Maybe<B> getMaybe(S s) {
                return AffineTraversal.this.getMaybe(s).map(other::get);
            }
            @Override public S set(B b, S s) {
                return AffineTraversal.this.modify(s, a -> other.set(b, a));
            }
        };
    }

    /**
     * Composes this with a prism.
     */
    default <B> AffineTraversal<S, B> compose(Prism<A, B> other) {
        return new AffineTraversal<S, B>() {
            @Override public Maybe<B> getMaybe(S s) {
                return (Maybe<B>) AffineTraversal.this.getMaybe(s).flatMap(other::getMaybe);
            }
            @Override public S set(B b, S s) {
                return AffineTraversal.this.modify(s, a -> other.set(b, a));
            }
        };
    }

    /**
     * Views this affine traversal as a read-only Fold.
     */
    @SuppressWarnings("unchecked")
    default Fold<S, A> asFold() {
        return s -> (Collection<A>) getMaybe(s).foldl((Collection<A>) List.<A>nil(), Collection::build);
    }

    /**
     * Views this affine traversal as a Traversal.
     */
    default Traversal<S, A> asTraversal() {
        return new Traversal<S, A>() {
            @Override public Collection<A> getAll(S s) {
                return (Collection<A>) getMaybe(s).foldl((Collection<A>) List.<A>nil(), Collection::build);
            }
            @Override public S modify(S s, Function<A, A> fn) {
                return AffineTraversal.this.modify(s, fn);
            }
        };
    }

    /**
     * Composes this with a getter.
     */
    @SuppressWarnings("unchecked")
    default <B> Fold<S, B> compose(Getter<A, B> other) {
        return s -> (Collection<B>) getMaybe(s).map(other::get).foldl((Collection<B>) List.<B>nil(), Collection::build);
    }

    /**
     * Composes this with a fold.
     */
    default <B> Fold<S, B> compose(Fold<A, B> other) {
        return s -> getMaybe(s).map(other::getAll).orElse(List.nil());
    }

    /**
     * Converts this optional focus into a mandatory lens by providing a default value.
     */
    default Lens<S, A> withDefault(A defaultValue) {
        return Lens.of(
            s -> getMaybe(s).orElse(defaultValue),
            this::set
        );
    }

    static <S, A> AffineTraversal<S, A> of(Function<S, Maybe<A>> getter, java.util.function.BiFunction<A, S, S> setter) {
        return new AffineTraversal<S, A>() {
            @Override public Maybe<A> getMaybe(S s) { return getter.apply(s); }
            @Override public S set(A a, S s) { return setter.apply(a, s); }
        };
    }
}
