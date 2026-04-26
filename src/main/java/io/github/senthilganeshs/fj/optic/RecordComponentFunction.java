package io.github.senthilganeshs.fj.optic;

import java.io.Serializable;
import java.util.function.Function;

/**
 * A specialized Function that is Serializable, allowing us to extract 
 * metadata about the method reference at runtime.
 */
@FunctionalInterface
public interface RecordComponentFunction<S, A> extends Function<S, A>, Serializable {}
