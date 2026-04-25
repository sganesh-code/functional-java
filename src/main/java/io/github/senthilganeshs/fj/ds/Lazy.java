package io.github.senthilganeshs.fj.ds;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A purely functional container for a deferred, memoized computation.
 * The computation is performed at most once, and only when requested.
 * 
 * @param <T> The type of the value.
 */
public final class Lazy<T> {
    private final Supplier<? extends T> supplier;
    private volatile boolean evaluated = false;
    private T value;

    private Lazy(Supplier<? extends T> supplier) {
        this.supplier = supplier;
    }

    /**
     * Creates a Lazy container from a supplier.
     */
    public static <T> Lazy<T> of(Supplier<? extends T> supplier) {
        return new Lazy<>(supplier);
    }

    /**
     * Returns the computed value. Triggers the computation if not already done.
     * This method is thread-safe.
     */
    public T get() {
        if (!evaluated) {
            synchronized (this) {
                if (!evaluated) {
                    value = supplier.get();
                    evaluated = true;
                }
            }
        }
        return value;
    }

    /**
     * Transforms the value inside the Lazy container without triggering the computation.
     */
    public <R> Lazy<R> map(Function<? super T, ? extends R> fn) {
        return Lazy.of(() -> fn.apply(get()));
    }

    /**
     * Chains computations without triggering them.
     */
    public <R> Lazy<R> flatMap(Function<? super T, Lazy<R>> fn) {
        return Lazy.of(() -> fn.apply(get()).get());
    }

    @Override
    public String toString() {
        return evaluated ? "Lazy(" + value + ")" : "Lazy(?)";
    }
}
