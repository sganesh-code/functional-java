package io.github.senthilganeshs.fj.test;

import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.typeclass.Monad;
import java.util.Random;
import java.util.function.Function;

/**
 * A generator for random data of type A.
 */
public record Gen<A>(Function<Random, A> sample) implements Higher<Gen.µ, A> {
    public final static class µ {}

    @SuppressWarnings("unchecked")
    public static <A> Gen<A> narrowK(Higher<µ, A> hka) {
        return (Gen<A>) hka;
    }

    public static <A> Gen<A> pure(A a) {
        return new Gen<>(__ -> a);
    }

    public <B> Gen<B> map(Function<A, B> fn) {
        return new Gen<>(r -> fn.apply(sample.apply(r)));
    }

    public <B> Gen<B> flatMap(Function<A, Gen<B>> fn) {
        return new Gen<>(r -> fn.apply(sample.apply(r)).sample().apply(r));
    }

    public static final Monad<µ> monad = new Monad<>() {
        @Override
        public <A> Higher<µ, A> pure(A a) { return Gen.pure(a); }

        @Override
        public <A, B> Higher<µ, B> flatMap(Function<A, Higher<µ, B>> fn, Higher<µ, A> fa) {
            return narrowK(fa).flatMap(a -> narrowK(fn.apply(a)));
        }
    };

    // --- Common Generators ---

    public static Gen<Integer> integer() {
        return new Gen<>(Random::nextInt);
    }

    public static Gen<Integer> choose(int min, int max) {
        return new Gen<>(r -> r.nextInt(max - min) + min);
    }

    public static Gen<String> string(int length) {
        return new Gen<>(r -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < length; i++) {
                sb.append((char) (r.nextInt(26) + 'a'));
            }
            return sb.toString();
        });
    }

    public static <A> Gen<io.github.senthilganeshs.fj.ds.List<A>> list(Gen<A> gen, int maxLength) {
        return new Gen<>(r -> {
            int len = r.nextInt(maxLength);
            io.github.senthilganeshs.fj.ds.List<A> res = io.github.senthilganeshs.fj.ds.List.nil();
            for (int i = 0; i < len; i++) {
                res = res.build(gen.sample().apply(r));
            }
            return res;
        });
    }
}
