package io.github.senthilganeshs.fj.ds;

import java.util.concurrent.atomic.AtomicReference;
import reactor.core.publisher.Sinks;

/**
 * A one-shot completion cell.
 *
 * @param <A> The type of value carried by the deferred.
 */
public final class Deferred<A> {
    private static final Object UNSET = new Object();
    private static final Object NULL_VALUE = new Object();

    private final AtomicReference<Object> state = new AtomicReference<>(UNSET);
    private final Sinks.One<Task.Value<A>> sink = Sinks.one();

    public static <A> Deferred<A> of() {
        return new Deferred<>();
    }

    /**
     * Completes the deferred once. Returns {@code true} for the winning completion.
     */
    public boolean complete(A value) {
        Object encoded = encode(value);
        if (state.compareAndSet(UNSET, encoded)) {
            sink.tryEmitValue(Task.Value.of(value));
            return true;
        }
        return false;
    }

    public Maybe<A> tryGet() {
        Object current = state.get();
        if (current == UNSET) {
            return Maybe.nothing();
        }
        return Maybe.some(decode(current));
    }

    /**
     * Returns a task that completes when the deferred is completed.
     */
    public Task<A> get() {
        return Task.defer(() -> {
            Object current = state.get();
            if (current != UNSET) {
                return Task.succeed(decode(current));
            }
            return Task.fromNullableMono(sink.asMono());
        });
    }

    private static Object encode(Object value) {
        return value == null ? NULL_VALUE : value;
    }

    @SuppressWarnings("unchecked")
    private A decode(Object value) {
        return value == NULL_VALUE ? null : (A) value;
    }
}
