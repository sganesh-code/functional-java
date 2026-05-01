package io.github.senthilganeshs.fj.stream;

import io.github.senthilganeshs.fj.hkt.Higher;
import java.util.function.Function;

/**
 * A stream consumer that performs an effect for each element in the stream.
 */
@FunctionalInterface
public interface Sink<F, I> extends Function<Stream<F, I>, Higher<F, Void>> {
}
