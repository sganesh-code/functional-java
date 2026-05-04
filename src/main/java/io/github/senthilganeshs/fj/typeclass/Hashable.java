package io.github.senthilganeshs.fj.typeclass;

/**
 * A typeclass for values that can be converted to a canonical, stable hash string.
 * Used for content-addressed memoization and fingerprinting.
 */
public interface Hashable<T> {
    /**
     * @return A stable, canonical representation of the value.
     */
    String hash(T value);
}
