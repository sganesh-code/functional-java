package io.github.senthilganeshs.fj.ds;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.Function;

/**
 * The terminal state of a started task.
 *
 * @param <E> The failure type.
 * @param <A> The success type.
 */
public sealed interface Outcome<E, A> permits Outcome.Succeeded, Outcome.Failed, Outcome.Cancelled {

    static <E, A> Outcome<E, A> succeeded(A value) {
        return new Succeeded<>(value);
    }

    static <E, A> Outcome<E, A> failed(E error) {
        return new Failed<>(error);
    }

    static <E, A> Outcome<E, A> cancelled() {
        return new Cancelled<>();
    }

    default boolean isSucceeded() {
        return this instanceof Succeeded;
    }

    default boolean isFailed() {
        return this instanceof Failed;
    }

    default boolean isCancelled() {
        return this instanceof Cancelled;
    }

    default <R> R fold(Supplier<R> onCancelled, Function<E, R> onFailed, Function<A, R> onSucceeded) {
        Objects.requireNonNull(onCancelled, "onCancelled");
        Objects.requireNonNull(onFailed, "onFailed");
        Objects.requireNonNull(onSucceeded, "onSucceeded");
        if (this instanceof Cancelled<E, A>) {
            return onCancelled.get();
        }
        if (this instanceof Failed<E, A> failed) {
            return onFailed.apply(failed.error());
        }
        return onSucceeded.apply(((Succeeded<E, A>) this).value());
    }

    record Succeeded<E, A>(A value) implements Outcome<E, A> {
        public Succeeded {
            // value may be null if the computation produced null intentionally
        }
    }

    record Failed<E, A>(E error) implements Outcome<E, A> {
        public Failed {
            Objects.requireNonNull(error, "error");
        }
    }

    record Cancelled<E, A>() implements Outcome<E, A> {}
}
