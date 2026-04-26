package io.github.senthilganeshs.fj.codec;

import io.github.senthilganeshs.fj.ds.*;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Primitives and combinators for functional serialization.
 */
public final class Codec {
    private Codec() {}

    // --- Encoders ---

    public static Encoder<Integer> intEncoder() {
        return (out, val) -> {
            try { out.writeInt(val); return Either.right(null); }
            catch (IOException e) { return Either.left(e.getMessage()); }
        };
    }

    public static Encoder<Double> doubleEncoder() {
        return (out, val) -> {
            try { out.writeDouble(val); return Either.right(null); }
            catch (IOException e) { return Either.left(e.getMessage()); }
        };
    }

    public static Encoder<Boolean> booleanEncoder() {
        return (out, val) -> {
            try { out.writeBoolean(val); return Either.right(null); }
            catch (IOException e) { return Either.left(e.getMessage()); }
        };
    }

    public static Encoder<String> stringEncoder() {
        return (out, val) -> {
            try {
                if (val == null) {
                    out.writeBoolean(false);
                } else {
                    out.writeBoolean(true);
                    out.writeUTF(val);
                }
                return Either.right(null);
            } catch (IOException e) { return Either.left(e.getMessage()); }
        };
    }

    public static <A> Encoder<List<A>> listEncoder(Encoder<A> enc) {
        return (out, list) -> {
            try {
                out.writeInt(list.length());
                return list.foldl(Either.right(null), (res, a) -> 
                    res.isRight() ? enc.encode(out, a) : res
                );
            } catch (IOException e) { return Either.left(e.getMessage()); }
        };
    }

    public static <A> Encoder<Maybe<A>> maybeEncoder(Encoder<A> enc) {
        return (out, maybe) -> {
            try {
                out.writeBoolean(maybe.isSome());
                return maybe.foldl(Either.right(null), (res, a) -> enc.encode(out, a));
            } catch (IOException e) { return Either.left(e.getMessage()); }
        };
    }

    public static <A, B> Encoder<Tuple<A, B>> tupleEncoder(Encoder<A> encA, Encoder<B> encB) {
        return (out, tuple) -> {
            Either<String, Void> resA = encA.encode(out, tuple.getA().orElse(null));
            if (resA.isLeft()) return resA;
            return encB.encode(out, tuple.getB().orElse(null));
        };
    }

    public static <K, V> Encoder<HashMap<K, V>> mapEncoder(Encoder<K> encK, Encoder<V> encV) {
        return (out, map) -> {
            try {
                out.writeInt(map.size());
                return map.foldl(Either.right(null), (res, entry) -> {
                    if (res.isLeft()) return res;
                    Either<String, Void> resK = encK.encode(out, entry.key());
                    if (resK.isLeft()) return resK;
                    return encV.encode(out, entry.value());
                });
            } catch (IOException e) { return Either.left(e.getMessage()); }
        };
    }

    // --- Decoders ---

    public static Decoder<Integer> intDecoder() {
        return in -> {
            try { return Either.right(in.readInt()); }
            catch (IOException e) { return Either.left(e.getMessage()); }
        };
    }

    public static Decoder<Double> doubleDecoder() {
        return in -> {
            try { return Either.right(in.readDouble()); }
            catch (IOException e) { return Either.left(e.getMessage()); }
        };
    }

    public static Decoder<Boolean> booleanDecoder() {
        return in -> {
            try { return Either.right(in.readBoolean()); }
            catch (IOException e) { return Either.left(e.getMessage()); }
        };
    }

    public static Decoder<String> stringDecoder() {
        return in -> {
            try {
                if (in.readBoolean()) return Either.right(in.readUTF());
                else return Either.right(null);
            } catch (IOException e) { return Either.left(e.getMessage()); }
        };
    }

    public static <A> Decoder<List<A>> listDecoder(Decoder<A> dec) {
        return in -> {
            try {
                int len = in.readInt();
                List<A> list = List.nil();
                for (int i = 0; i < len; i++) {
                    Either<String, A> res = dec.decode(in);
                    if (res.isLeft()) return (Either<String, List<A>>) (Either<?, ?>) res;
                    list = (List<A>) list.build(res.orElse(null));
                }
                return Either.right(list);
            } catch (IOException e) { return Either.left(e.getMessage()); }
        };
    }

    public static <A> Decoder<Maybe<A>> maybeDecoder(Decoder<A> dec) {
        return in -> {
            try {
                if (in.readBoolean()) return dec.decode(in).map(Maybe::some);
                else return Either.right(Maybe.nothing());
            } catch (IOException e) { return Either.left(e.getMessage()); }
        };
    }

    public static <A, B> Decoder<Tuple<A, B>> tupleDecoder(Decoder<A> decA, Decoder<B> decB) {
        return in -> decA.decode(in).flatMapEither(a -> 
            decB.decode(in).map(b -> Tuple.of(a, b))
        );
    }

    public static <K, V> Decoder<HashMap<K, V>> mapDecoder(Decoder<K> decK, Decoder<V> decV) {
        return in -> {
            try {
                int size = in.readInt();
                HashMap<K, V> map = HashMap.nil();
                for (int i = 0; i < size; i++) {
                    Either<String, K> resK = decK.decode(in);
                    if (resK.isLeft()) return (Either<String, HashMap<K, V>>) (Either<?, ?>) resK;
                    Either<String, V> resV = decV.decode(in);
                    if (resV.isLeft()) return (Either<String, HashMap<K, V>>) (Either<?, ?>) resV;
                    map = map.put(resK.orElse(null), resV.orElse(null));
                }
                return Either.right(map);
            } catch (IOException e) { return Either.left(e.getMessage()); }
        };
    }
}
