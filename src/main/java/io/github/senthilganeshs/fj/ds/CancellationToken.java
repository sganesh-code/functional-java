package io.github.senthilganeshs.fj.ds;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A token that can be used to request cancellation of an asynchronous operation.
 */
public final class CancellationToken {
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public void cancel() {
        cancelled.set(true);
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public void throwIfCancelled() {
        if (isCancelled()) {
            throw new RuntimeException("Operation cancelled");
        }
    }
}
