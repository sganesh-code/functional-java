package io.github.senthilganeshs.fj.ds;

import java.util.Objects;

/**
 * A started task that can be awaited or cancelled.
 *
 * @param <E> The failure type.
 * @param <A> The success type.
 */
public final class Fiber<E, A> {
    private final CancellationToken cancellationToken;
    private final Task<Outcome<E, A>> joinTask;

    Fiber(CancellationToken cancellationToken, Task<Outcome<E, A>> joinTask) {
        this.cancellationToken = Objects.requireNonNull(cancellationToken, "cancellationToken");
        this.joinTask = Objects.requireNonNull(joinTask, "joinTask");
    }

    public Task<Outcome<E, A>> join() {
        return joinTask;
    }

    public void cancel() {
        cancellationToken.cancel();
    }
}
