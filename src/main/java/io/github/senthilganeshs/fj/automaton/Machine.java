package io.github.senthilganeshs.fj.automaton;

import io.github.senthilganeshs.fj.ds.List;

/**
 * Represents the pure logic of the automaton.
 *
 * <p>A Machine is a pure function that defines how the state evolves and what commands are emitted
 * in response to an input message.</p>
 *
 * @param <S> The immutable state type.
 * @param <I> The input message/event type.
 * @param <O> The abstract command/output type.
 */
@FunctionalInterface
public interface Machine<S, I, O> {
    /**
     * Transition the state of the automaton based on the given input.
     *
     * @param state  The current immutable state.
     * @param input  The incoming message/event.
     * @return       A Result containing the next state and a list of commands to execute.
     */
    Result<S, O> transition(S state, I input);

    /**
     * The result of a state transition.
     *
     * @param state    The new state of the automaton.
     * @param commands The list of abstract commands to be executed as side-effects.
     * @param <S>      The state type.
     * @param <O>      The command type.
     */
    record Result<S, O>(S state, List<O> commands) {}
}
