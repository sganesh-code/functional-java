package io.github.senthilganeshs.fj.codec;

import io.github.senthilganeshs.fj.ds.Either;
import java.io.DataOutputStream;

/**
 * A functional encoder that writes a value to a DataOutputStream.
 * @param <A> The type of the value to encode.
 */
@FunctionalInterface
public interface Encoder<A> {
    Either<String, Void> encode(DataOutputStream out, A value);
}
