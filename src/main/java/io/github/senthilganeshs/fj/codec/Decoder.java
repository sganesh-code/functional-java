package io.github.senthilganeshs.fj.codec;

import io.github.senthilganeshs.fj.ds.Either;
import java.io.DataInputStream;

/**
 * A functional decoder that reads a value from a DataInputStream.
 * @param <A> The type of the value to decode.
 */
@FunctionalInterface
public interface Decoder<A> {
    Either<String, A> decode(DataInputStream in);
}
