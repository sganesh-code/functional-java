package io.github.senthilganeshs.fj.automaton;

import io.github.senthilganeshs.fj.hkt.Higher;

/**
 * Represents the persistence layer of the automaton.
 *
 * <p>A Repository handles loading and saving the automaton's state from/to a persistent store.</p>
 *
 * @param <F> The effect type (e.g., Task.µ).
 * @param <K> The key type used to identify the state.
 * @param <S> The state type.
 */
public interface Repository<F, K, S> {
    /**
     * Load the state associated with the given key.
     *
     * @param key The key identifying the state.
     * @return    An effect F that yields the state.
     */
    Higher<F, S> load(K key);

    /**
     * Save the state associated with the given key.
     *
     * @param key   The key identifying the state.
     * @param state The state to persist.
     * @return      An effect F that yields nothing (Void) upon completion.
     */
    Higher<F, Void> save(K key, S state);
}
