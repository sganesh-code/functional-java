package io.github.senthilganeshs.fj.automaton;

import io.github.senthilganeshs.fj.ds.Collection;
import io.github.senthilganeshs.fj.ds.List;
import io.github.senthilganeshs.fj.ds.Task;
import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.typeclass.Monad;
import java.util.function.Function;

/**
 * The orchestrator engine that drives the effectful state machine.
 *
 * <p>The Automaton stitches together the pure logic (Machine), the side-effects (Interpreter),
 * and the persistence (Repository) into a cohesive execution loop.</p>
 *
 * @param <F> The effect type (e.g., Task.µ).
 * @param <K> The key type used to identify the state.
 * @param <S> The state type.
 * @param <I> The input message/event type.
 * @param <O> The abstract command/output type.
 */
public final class Automaton<F, K, S, I, O> {
    private final Machine<S, I, O> machine;
    private final Interpreter<F, O, I> interpreter;
    private final Repository<F, K, S> repository;
    private final Monad<F> monad;

    /**
     * Creates a new Automaton orchestrator.
     *
     * @param machine     The pure logic of the state machine.
     * @param interpreter The side-effect interpreter.
     * @param repository  The state persistence layer.
     * @param monad       The monad instance for the effect type F.
     */
    public Automaton(
            Machine<S, I, O> machine,
            Interpreter<F, O, I> interpreter,
            Repository<F, K, S> repository,
            Monad<F> monad) {
        this.machine = machine;
        this.interpreter = interpreter;
        this.repository = repository;
        this.monad = monad;
    }

    /**
     * Ergonomic factory method for creating an Automaton using the library's Task type.
     *
     * @param machine     The pure logic of the state machine.
     * @param interpreter The side-effect interpreter.
     * @param repository  The state persistence layer.
     * @param <K>         The key type.
     * @param <S>         The state type.
     * @param <I>         The input message type.
     * @param <O>         The command type.
     * @return            A new Automaton instance using Task.µ as the effect type.
     */
    public static <K, S, I, O> Automaton<Task.µ, K, S, I, O> ofTask(
            Machine<S, I, O> machine,
            Interpreter<Task.µ, O, I> interpreter,
            Repository<Task.µ, K, S> repository) {
        return new Automaton<>(machine, interpreter, repository, Task.monad);
    }

    /**
     * Drives the automaton for a single input, potentially triggering a recursive loop.
     *
     * <p>This method performs the following steps:
     * 1. Loads the current state from the repository.
     * 2. Executes the state transition logic.
     * 3. Persists the new state (checkpointing).
     * 4. Executes emitted commands via the interpreter.
     * 5. Sequentially processes any new inputs yielded by the interpreter.</p>
     *
     * @param key   The key identifying the state.
     * @param input The incoming message/event.
     * @return      An effect F that yields the final state after all transitions.
     */
    public Higher<F, S> run(K key, I input) {
        return monad.flatMap(state -> {
            // 1. Transition to next state
            Machine.Result<S, O> result = machine.transition(state, input);

            // 2. Persist state immediately (Checkpointing)
            return monad.flatMap(v ->
                // 3. Execute side-effects
                monad.flatMap(newInputsList -> {
                    List<I> flattenedInputs = List.from(Collection.flatten(newInputsList));
                    return processInputs(key, result.state(), flattenedInputs);
                }, traverse(result.commands(), interpreter::execute)),
                repository.save(key, result.state())
            );
        }, repository.load(key));
    }

    /**
     * Recursively processes a list of inputs sequentially.
     */
    private Higher<F, S> processInputs(K key, S currentState, List<I> inputs) {
        if (inputs.isEmpty()) {
            return monad.pure(currentState);
        }

        // Sequential feedback loop - each input transition must see the state from the previous one
        return inputs.foldl(
            monad.pure(currentState),
            (accF, nextInput) -> monad.flatMap(s -> run(key, nextInput), accF)
        );
    }

    /**
     * Helper to traverse a list of values with an effectful function.
     */
    private <A, B> Higher<F, List<B>> traverse(List<A> items, Function<A, Higher<F, B>> fn) {
        return items.foldl(
            monad.pure(List.nil()),
            (accF, a) -> monad.flatMap(bs -> 
                monad.map(b -> List.from(bs.build(b)), fn.apply(a)),
                accF
            )
        );
    }
}
