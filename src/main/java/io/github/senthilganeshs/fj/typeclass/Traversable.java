package io.github.senthilganeshs.fj.typeclass;

import io.github.senthilganeshs.fj.hkt.Higher;
import java.util.function.Function;

/**
 * A typeclass for functors that can be traversed from left to right, performing an action on each element.
 * 
 * @param <W> The witness type of the traversable functor.
 */
public interface Traversable<W> extends Functor<W>, Foldable<W> {
    /**
     * Traverses the structure, applying an effectful function to each element and collecting the results.
     */
    <G, A, B> Higher<G, Higher<W, B>> traverse(Applicative<G> applicative, Function<A, Higher<G, B>> fn, Higher<W, A> fa);

    /**
     * Static utility for traverse.
     */
    static <W, G, A, B> Higher<G, Higher<W, B>> traverse(Traversable<W> t, Applicative<G> app, Function<A, Higher<G, B>> fn, Higher<W, A> fa) {
        return t.traverse(app, fn, fa);
    }

    /**
     * Static utility for sequence.
     */
    static <W, G, A> Higher<G, Higher<W, A>> sequence(Traversable<W> t, Applicative<G> app, Higher<W, Higher<G, A>> fga) {
        return t.sequence(app, fga);
    }

    /**
     * Flips a nested structure (e.g., F&lt;G&lt;A&gt;&gt; to G&lt;F&lt;A&gt;&gt;).
     */
    default <G, A> Higher<G, Higher<W, A>> sequence(Applicative<G> applicative, Higher<W, Higher<G, A>> fga) {
        return traverse(applicative, Function.identity(), fga);
    }
}
