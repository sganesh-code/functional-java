package io.github.senthilganeshs.fj.stream;

import io.github.senthilganeshs.fj.ds.Maybe;
import io.github.senthilganeshs.fj.ds.Tuple;
import io.github.senthilganeshs.fj.ds.Task;
import io.github.senthilganeshs.fj.ds.List;
import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.typeclass.Monad;
import java.util.function.Function;

/**
 * A lazy, resource-safe data stream evaluated within an effect context F.
 * 
 * @param <F> The effect witness (e.g., IO.µ, Task.µ).
 * @param <A> The type of elements in the stream.
 */
public record Stream<F, A>(Higher<F, Maybe<Tuple<A, Stream<F, A>>>> step) {

    public static <F, A> Stream<F, A> empty(Monad<F> monad) {
        return new Stream<>(monad.pure(Maybe.nothing()));
    }

    public static <F, A> Stream<F, A> emit(A a, Monad<F> monad) {
        return new Stream<>(monad.pure(Maybe.some(Tuple.of(a, empty(monad)))));
    }

    /**
     * Creates a stream from a collection of elements.
     */
    public static <F, A> Stream<F, A> fromList(List<A> list, Monad<F> monad) {
        return list.headMaybe().either(
            () -> empty(monad),
            head -> new Stream<>(monad.pure(Maybe.some(Tuple.of(head, fromList(List.from(list.drop(1)), monad)))))
        );
    }

    public <B> Stream<F, B> map(Function<A, B> fn, Monad<F> monad) {
        return new Stream<>(monad.map(maybe -> 
            maybe.map(t -> Tuple.of(fn.apply(t.getA().orElse(null)), t.getB().orElse(null).map(fn, monad))),
            step
        ));
    }

    public <B> Stream<F, B> flatMap(Function<A, Stream<F, B>> fn, Monad<F> monad) {
        return new Stream<>(monad.flatMap(maybe -> 
            maybe.either(
                () -> monad.pure(Maybe.nothing()),
                t -> {
                    Stream<F, B> first = fn.apply(t.getA().orElse(null));
                    Stream<F, B> rest = t.getB().orElse(null).flatMap(fn, monad);
                    return concat(first, rest, monad).step();
                }
            ),
            step
        ));
    }

    /**
     * Resource-safe bracket operation for streams.
     */
    public static <F, R, A> Stream<F, A> bracket(
            Higher<F, R> acquire, 
            Function<R, Stream<F, A>> use, 
            Function<R, Higher<F, Void>> release, 
            Monad<F> monad) {
        return new Stream<>(monad.flatMap(resource -> 
            monad.map(maybe -> 
                maybe.map(t -> Tuple.of(
                    t.getA().orElse(null), 
                    t.getB().orElse(null).onFinalize(release.apply(resource), monad)
                )),
                use.apply(resource).step()
            ),
            acquire
        ));
    }

    /**
     * Attaches a finalizer to be executed when the stream ends.
     */
    public Stream<F, A> onFinalize(Higher<F, Void> finalizer, Monad<F> monad) {
        return new Stream<>(monad.flatMap(maybe -> 
            maybe.either(
                () -> monad.map(__ -> Maybe.nothing(), finalizer),
                t -> monad.pure(Maybe.some(Tuple.of(t.getA().orElse(null), t.getB().orElse(null).onFinalize(finalizer, monad))))
            ),
            step
        ));
    }

    /**
     * Concurrently evaluates effects within a stream while preserving order.
     * Specific to Task context.
     */
    @SuppressWarnings("unchecked")
    public <B> Stream<Task.µ, B> parEvalMap(int parallelism, Function<A, Task<B>> fn) {
        Monad<Task.µ> monad = Task.monad;
        Higher<Task.µ, Maybe<Tuple<A, Stream<F, A>>>> hStep = (Higher<Task.µ, Maybe<Tuple<A, Stream<F, A>>>>) step;
        
        return new Stream<>(monad.flatMap(maybe -> 
            maybe.either(
                () -> monad.pure(Maybe.nothing()),
                t -> {
                    Task<B> first = fn.apply(t.getA().orElse(null));
                    return first.map(b -> Maybe.some(Tuple.of(b, t.getB().orElse(null).parEvalMap(parallelism, fn))));
                }
            ),
            hStep
        ));
    }
    
    // Concat helper
    public static <F, A> Stream<F, A> concat(Stream<F, A> s1, Stream<F, A> s2, Monad<F> monad) {
        return new Stream<>(monad.flatMap(maybe -> 
            maybe.either(
                () -> s2.step(),
                t -> monad.pure(Maybe.some(Tuple.of(t.getA().orElse(null), concat(t.getB().orElse(null), s2, monad))))
            ),
            s1.step()
        ));
    }

    /**
     * Folds the stream into a single value within the effect context.
     */
    public <B> Higher<F, B> foldl(B seed, java.util.function.BiFunction<B, A, B> fn, Monad<F> monad) {
        return monad.flatMap(maybe -> 
            maybe.either(
                () -> monad.pure(seed),
                t -> t.getB().orElse(null).foldl(fn.apply(seed, t.getA().orElse(null)), fn, monad)
            ),
            step
        );
    }
}
