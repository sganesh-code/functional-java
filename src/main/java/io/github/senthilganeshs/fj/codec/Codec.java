package io.github.senthilganeshs.fj.codec;

import io.github.senthilganeshs.fj.ds.Collection;
import io.github.senthilganeshs.fj.ds.Either;
import io.github.senthilganeshs.fj.ds.HashMap;
import io.github.senthilganeshs.fj.ds.List;
import io.github.senthilganeshs.fj.ds.Tuple;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Foundation for functional binary and text codecs.
 */
public final class Codec {
    private Codec() {}

    public interface Encoder<A> {
        Either<String, Void> encode(DataOutputStream out, A value);
    }

    public interface Decoder<A> {
        Either<String, A> decode(DataInputStream in);
    }

    public static <A> Either<String, A> leftT(String msg) {
        return Either.left(msg);
    }

    public static <A> Encoder<A> encoder(Encoder<A> enc) { return enc; }
    public static <A> Decoder<A> decoder(Decoder<A> dec) { return dec; }

    // --- Primitive Encoders ---

    public static final Encoder<String> stringEncoder = (out, s) -> {
        try { if (s == null) out.writeUTF("__NULL__"); else out.writeUTF(s); return Either.right(null); }
        catch (IOException e) { return leftT(e.getMessage()); }
    };

    public static final Encoder<Integer> intEncoder = (out, i) -> {
        try { out.writeInt(i); return Either.right(null); }
        catch (IOException e) { return leftT(e.getMessage()); }
    };

    public static final Encoder<Double> doubleEncoder = (out, d) -> {
        try { out.writeDouble(d); return Either.right(null); }
        catch (IOException e) { return leftT(e.getMessage()); }
    };

    public static final Encoder<Boolean> booleanEncoder = (out, b) -> {
        try { out.writeBoolean(b); return Either.right(null); }
        catch (IOException e) { return leftT(e.getMessage()); }
    };

    public static Encoder<String> stringEncoder() { return stringEncoder; }
    public static Encoder<Integer> intEncoder() { return intEncoder; }
    public static Encoder<Double> doubleEncoder() { return doubleEncoder; }
    public static Encoder<Boolean> booleanEncoder() { return booleanEncoder; }

    // --- Primitive Decoders ---

    public static final Decoder<String> stringDecoder = in -> {
        try { String s = in.readUTF(); return s.equals("__NULL__") ? Either.right(null) : Either.right(s); }
        catch (IOException e) { return leftT(e.getMessage()); }
    };

    public static final Decoder<Integer> intDecoder = in -> {
        try { return Either.right(in.readInt()); }
        catch (IOException e) { return leftT(e.getMessage()); }
    };

    public static final Decoder<Double> doubleDecoder = in -> {
        try { return Either.right(in.readDouble()); }
        catch (IOException e) { return leftT(e.getMessage()); }
    };

    public static final Decoder<Boolean> booleanDecoder = in -> {
        try { return Either.right(in.readBoolean()); }
        catch (IOException e) { return leftT(e.getMessage()); }
    };

    public static Decoder<String> stringDecoder() { return stringDecoder; }
    public static Decoder<Integer> intDecoder() { return intDecoder; }
    public static Decoder<Double> doubleDecoder() { return doubleDecoder; }
    public static Decoder<Boolean> booleanDecoder() { return booleanDecoder; }

    // --- Combinators ---

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
                out.writeInt(map.length());
                for (HashMap.Entry<K, V> entry : map) {
                    Either<String, Void> resK = encK.encode(out, entry.key());
                    if (resK.isLeft()) return resK;
                    Either<String, Void> resV = encV.encode(out, entry.value());
                    if (resV.isLeft()) return resV;
                }
                return Either.right(null);
            } catch (IOException e) { return leftT(e.getMessage()); }
        };
    }

    public static <A> Encoder<List<A>> listEncoder(Encoder<A> enc) {
        return (out, list) -> {
            try {
                out.writeInt(list.length());
                for (A a : list) {
                    Either<String, Void> res = enc.encode(out, a);
                    if (res.isLeft()) return res;
                }
                return Either.right(null);
            } catch (IOException e) { return leftT(e.getMessage()); }
        };
    }

    public static <A, B> Decoder<Tuple<A, B>> tupleDecoder(Decoder<A> decA, Decoder<B> decB) {
        return in -> {
            Either<String, A> resA = decA.decode(in);
            if (resA.isLeft()) return (Either<String, Tuple<A, B>>) (Either<?, ?>) resA;
            Either<String, B> resB = decB.decode(in);
            if (resB.isLeft()) return (Either<String, Tuple<A, B>>) (Either<?, ?>) resB;
            return Either.right(Tuple.of(resA.fromRight(null), resB.fromRight(null)));
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
                    list = (List<A>) list.build(res.fromRight(null));
                }
                return Either.right(list);
            } catch (IOException e) { return leftT(e.getMessage()); }
        };
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
                    map = map.put(resK.fromRight(null), resV.fromRight(null));
                }
                return Either.right(map);
            } catch (IOException e) { return leftT(e.getMessage()); }
        };
    }
}
