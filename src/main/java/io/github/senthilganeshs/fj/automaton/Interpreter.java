package io.github.senthilganeshs.fj.automaton;

import io.github.senthilganeshs.fj.ds.List;
import io.github.senthilganeshs.fj.hkt.Higher;

/**
 * Represents the side-effect layer of the automaton.
 *
 * <p>An Interpreter maps abstract commands to real-world effects in the context of F.
 * These effects may yield a list of new input messages to be fed back into the automaton.</p>
 *
 * @param <F> The effect type (e.g., Task.µ).
 * @param <O> The abstract command/output type.
 * @param <I> The input message/event type.
 */
@FunctionalInterface
public interface Interpreter<F, O, I> {
    /**
     * Execute an abstract command and return an effect that yields new inputs.
     *
     * @param command The abstract command to execute.
     * @return        An effect F that yields a list of response messages (inputs).
     */
    Higher<F, List<I>> execute(O command);
}
