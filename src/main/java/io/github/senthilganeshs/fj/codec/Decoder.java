package io.github.senthilganeshs.fj.codec;

import io.github.senthilganeshs.fj.ds.Either;
import java.io.DataInput;
import java.io.IOException;

/**
 * A functional decoder that reads a value from a DataInput.
 * @param <A> The type of the value to decode.
 */
@FunctionalInterface
public interface Decoder<A> {
    Either<String, A> decode(DataInput in);

    default Either<String, A> tryRead(DataInput in) {
        try {
            return decode(in);
        } catch (Exception e) {
            return Either.left(e.getMessage());
        }
    }
}
