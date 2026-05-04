package io.github.senthilganeshs.fj.codec;

import io.github.senthilganeshs.fj.ds.Either;
import io.github.senthilganeshs.fj.parser.JsonValue;
import java.util.function.Function;

/**
 * A functional codec for JSON serialization and deserialization.
 * 
 * @param <A> The type to encode/decode.
 */
public interface JsonCodec<A> {
    
    /**
     * Encodes a value to its JSON AST representation.
     */
    JsonValue encode(A value);

    /**
     * Decodes a value from its JSON AST representation.
     */
    Either<String, A> decode(JsonValue json);

    /**
     * Maps the codec to a new type using an isomorphism.
     */
    default <B> JsonCodec<B> bimap(Function<B, A> f, Function<A, B> g) {
        return new JsonCodec<>() {
            @Override public JsonValue encode(B value) { return JsonCodec.this.encode(f.apply(value)); }
            @Override public Either<String, B> decode(JsonValue json) { return (Either<String, B>) JsonCodec.this.decode(json).map(g); }
        };
    }

    /**
     * Creates a simple JsonCodec for values that can be handled by JsonValue.of().
     */
    static <A> JsonCodec<A> of() {
        return new JsonCodec<>() {
            @Override public JsonValue encode(A value) { return JsonValue.of(value); }
            @Override public Either<String, A> decode(JsonValue json) { 
                return Either.left("Generic decoding not implemented. Use specific codec or RecordOptics."); 
            }
        };
    }
}
