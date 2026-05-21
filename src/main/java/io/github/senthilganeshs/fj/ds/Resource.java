package io.github.senthilganeshs.fj.ds;

import java.util.Objects;
import java.util.function.Function;

/**
 * A composable resource that acquires a value and guarantees release.
 *
 * @param <A> The type of the acquired value.
 */
public final class Resource<A> {
    private final Task<Managed<A>> managed;

    private Resource(Task<Managed<A>> managed) {
        this.managed = managed;
    }

    /**
     * Builds a resource from separate acquisition and release tasks.
     */
    public static <A> Resource<A> make(Task<A> acquire, Function<A, Task<Void>> release) {
        Objects.requireNonNull(acquire, "acquire");
        Objects.requireNonNull(release, "release");
        return new Resource<>(Task.defer(() ->
            acquire.map(value -> new Managed<>(value, release.apply(value)))
        ));
    }

    public static <A> Resource<A> pure(A value) {
        return make(Task.succeed(value), ignored -> Task.succeed(null));
    }

    public <B> Resource<B> map(Function<A, B> fn) {
        Objects.requireNonNull(fn, "fn");
        return flatMap(value -> Resource.pure(fn.apply(value)));
    }

    public <B> Resource<B> flatMap(Function<A, Resource<B>> fn) {
        Objects.requireNonNull(fn, "fn");
        return new Resource<>(Task.defer(() ->
            managed.flatMap(outer ->
                fn.apply(outer.value).managed.map(inner ->
                    new Managed<>(inner.value, inner.release.flatMap(ignored -> outer.release))
                )
            )
        ));
    }

    /**
     * Uses the resource and guarantees release on success, failure, or cancellation.
     */
    public <B> Task<B> use(Function<A, Task<B>> fn) {
        Objects.requireNonNull(fn, "fn");
        return managed.flatMap(current ->
            Task.bracket(
                Task.succeed(current.value),
                fn,
                ignored -> current.release
            )
        );
    }

    private record Managed<A>(A value, Task<Void> release) {
    }
}
