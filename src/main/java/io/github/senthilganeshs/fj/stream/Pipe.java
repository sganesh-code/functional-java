package io.github.senthilganeshs.fj.stream;

import java.util.function.Function;

/**
 * A stream transducer that transforms a stream of inputs to a stream of outputs.
 */
@FunctionalInterface
public interface Pipe<F, I, O> extends Function<Stream<F, I>, Stream<F, O>> {
}
