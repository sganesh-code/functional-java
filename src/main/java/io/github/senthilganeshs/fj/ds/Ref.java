package io.github.senthilganeshs.fj.ds;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * A functional reference cell that holds an immutable value and provides thread-safe, 
 * non-blocking updates using atomic Compare-And-Swap (CAS) operations.
 * 
 * @param <A> The type of the immutable value held by this cell.
 */
public final class Ref<A> {
    private final AtomicReference<A> atomic;

    private Ref(A initial) {
        this.atomic = new AtomicReference<>(initial);
    }

    /**
     * Creates a new Ref with an initial value.
     */
    public static <A> Ref<A> of(A initial) {
        return new Ref<>(initial);
    }

    /**
     * Atomically reads the current value.
     */
    public A get() {
        return atomic.get();
    }

    /**
     * Atomically overwrites the current value.
     */
    public void set(A value) {
        atomic.set(value);
    }

    /**
     * Atomically updates the value using a pure function and returns the NEW value.
     */
    public A update(Function<A, A> fn) {
        while (true) {
            A current = atomic.get();
            A next = fn.apply(current);
            if (atomic.compareAndSet(current, next)) {
                return next;
            }
        }
    }

    /**
     * Atomically modifies the value and returns a computed side-result.
     * 
     * @param fn A function that takes the current state and returns a Tuple of (NextState, Result).
     * @param <B> The type of the side-result.
     * @return The computed side-result.
     */
    public <B> B modify(Function<A, Tuple<A, B>> fn) {
        while (true) {
            A current = atomic.get();
            Tuple<A, B> res = fn.apply(current);
            A next = res.getA().orElse(current);
            B out = res.getB().orElse(null);
            if (atomic.compareAndSet(current, next)) {
                return out;
            }
        }
    }

    @Override
    public String toString() {
        return "Ref(" + get() + ")";
    }
}
