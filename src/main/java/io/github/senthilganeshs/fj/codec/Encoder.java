package io.github.senthilganeshs.fj.codec;

import io.github.senthilganeshs.fj.ds.Either;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A functional encoder that writes a value to a DataOutput.
 * @param <A> The type of the value to encode.
 */
@FunctionalInterface
public interface Encoder<A> {
    Either<String, Void> encode(DataOutput out, A value);

    default Either<String, Void> tryWrite(DataOutput out, A value) {
        try {
            return encode(out, value);
        } catch (Exception e) {
            return Either.left(e.getMessage());
        }
    }
}
