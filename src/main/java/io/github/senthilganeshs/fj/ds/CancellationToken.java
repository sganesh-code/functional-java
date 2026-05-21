package io.github.senthilganeshs.fj.ds;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A token that can be used to request cancellation of an asynchronous operation.
 */
public final class CancellationToken {
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();

    public void cancel() {
        if (cancelled.compareAndSet(false, true)) {
            listeners.forEach(this::runListener);
        }
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public void onCancel(Runnable listener) {
        Objects.requireNonNull(listener, "listener");
        if (cancelled.get()) {
            runListener(listener);
            return;
        }

        listeners.add(listener);
        if (cancelled.get() && listeners.remove(listener)) {
            runListener(listener);
        }
    }

    public void throwIfCancelled() {
        if (isCancelled()) {
            throw new RuntimeException("Operation cancelled");
        }
    }

    private void runListener(Runnable listener) {
        try {
            listener.run();
        } catch (Throwable ex) {
            // Cancellation is best-effort cleanup; listener failures must not prevent other listeners.
        }
    }
}
