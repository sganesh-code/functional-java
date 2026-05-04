package io.github.senthilganeshs.fj.test;

import io.github.senthilganeshs.fj.ds.Collection;
import io.github.senthilganeshs.fj.ds.List;
import io.github.senthilganeshs.fj.ds.Maybe;
import io.github.senthilganeshs.fj.ds.Tuple;
import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.typeclass.Monad;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;

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

    /**
     * Filters generated values. Be careful with predicates that are hard to satisfy
     * as this may result in many retries.
     */
    public Gen<A> filter(Predicate<A> pred) {
        return new Gen<>(r -> {
            A val = sample.apply(r);
            while (!pred.test(val)) {
                val = sample.apply(r);
            }
            return val;
        });
    }

    public static final Monad<µ> monad = new Monad<>() {
        @Override
        public <A> Higher<µ, A> pure(A a) { return Gen.pure(a); }

        @Override
        public <A, B> Higher<µ, B> flatMap(Function<A, Higher<µ, B>> fn, Higher<µ, A> fa) {
            return narrowK(fa).flatMap(a -> narrowK(fn.apply(a)));
        }

        @Override
        public <A, B> Higher<µ, B> map(Function<A, B> fn, Higher<µ, A> fa) {
            return narrowK(fa).map(fn);
        }
    };

    // --- Common Generators ---

    public static Gen<Boolean> booleanGen() {
        return new Gen<>(Random::nextBoolean);
    }

    public static Gen<Integer> integer() {
        return new Gen<>(Random::nextInt);
    }

    public static Gen<Long> longGen() {
        return new Gen<>(Random::nextLong);
    }

    public static Gen<Double> doubleGen() {
        return new Gen<>(Random::nextDouble);
    }

    public static Gen<Float> floatGen() {
        return new Gen<>(Random::nextFloat);
    }

    public static Gen<Integer> choose(int min, int max) {
        if (min >= max) return Gen.pure(min);
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

    @SafeVarargs
    public static <A> Gen<A> elements(A... values) {
        return choose(0, values.length).map(i -> values[i]);
    }

    public static <A> Gen<A> oneOf(List<Gen<A>> gens) {
        return choose(0, gens.length()).flatMap(i -> gens.drop(i).headMaybe().orElse(gens.headMaybe().orElse(null)));
    }

    public static <A> Gen<A> frequency(List<Tuple<Integer, Gen<A>>> gens) {
        int total = gens.foldl(0, (acc, t) -> acc + t.getA().orElse(0));
        return choose(0, total).flatMap(n -> {
            return gens.foldl(Tuple.of(0, Maybe.<Gen<A>>nothing()), (acc, t) -> {
                if (acc.getB().orElse(null).isSome()) return acc;
                int current = acc.getA().orElse(0) + t.getA().orElse(0);
                if (n < current) return Tuple.of(current, t.getB());
                return Tuple.of(current, Maybe.<Gen<A>>nothing());
            }).getB().orElse(null).orElse(gens.headMaybe().flatMapMaybe(Tuple::getB).orElse(null));
        });
    }

    public static <A> Gen<Collection<A>> list(Gen<A> gen, int maxLength) {
        return new Gen<>(r -> {
            if (maxLength <= 0) return List.nil();
            int len = r.nextInt(maxLength);
            Collection<A> res = List.nil();
            for (int i = 0; i < len; i++) {
                res = res.build(gen.sample().apply(r));
            }
            return res;
        });
    }
}

