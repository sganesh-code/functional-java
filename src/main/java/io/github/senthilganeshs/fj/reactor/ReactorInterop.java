package io.github.senthilganeshs.fj.reactor;

import io.github.senthilganeshs.fj.ds.CancellationToken;
import io.github.senthilganeshs.fj.ds.Either;
import io.github.senthilganeshs.fj.ds.Fiber;
import io.github.senthilganeshs.fj.ds.Maybe;
import io.github.senthilganeshs.fj.ds.Task;
import io.github.senthilganeshs.fj.ds.TaskEither;
import io.github.senthilganeshs.fj.ds.Tuple;
import io.github.senthilganeshs.fj.stream.AsyncStream;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.reactivestreams.Subscription;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

/**
 * Reactor interop utilities that keep Reactor types confined to the bridge layer.
 *
 * Use these helpers at framework edges only. Domain and service code should
 * continue to expose {@code Task}, {@code TaskEither}, {@code ReaderTaskEither},
 * and {@code AsyncStream}.
 */
public final class ReactorInterop {
    private ReactorInterop() {
    }

    /**
     * Converts a {@link Mono} into a {@link Task} that fails when the mono completes empty.
     */
    public static <A> Task<A> monoToTask(Mono<A> mono) {
        Objects.requireNonNull(mono, "mono");
        return monoToMaybeTask(mono).flatMap(maybe ->
            maybe.either(
                () -> Task.fail(new NoSuchElementException("Mono completed empty")),
                Task::succeed
            )
        );
    }

    /**
     * Converts a {@link Mono} into a task that preserves empty completion as {@link Maybe#nothing()}.
     */
    public static <A> Task<Maybe<A>> monoToMaybeTask(Mono<A> mono) {
        Objects.requireNonNull(mono, "mono");
        return Task.asyncCancelable(callback -> {
            Disposable disposable = mono.subscribe(
                value -> callback.success(Maybe.some(value)),
                callback::failure,
                () -> callback.success(Maybe.nothing())
            );
            return disposable::dispose;
        });
    }

    /**
     * Converts a {@link Mono} into a typed-error task, mapping both throwable failures and empty completion.
     */
    public static <E, A> TaskEither<E, A> monoToTaskEither(Mono<A> mono, Function<Throwable, E> errorMapper) {
        Objects.requireNonNull(errorMapper, "errorMapper");
        return TaskEither.of(monoToMaybeTask(mono).map(maybe ->
            maybe.either(
                () -> Either.left(errorMapper.apply(new NoSuchElementException("Mono completed empty"))),
                Either::right
            )
        ));
    }

    /**
     * Converts a {@link Task} into a {@link Mono} without blocking the caller thread.
     */
    public static <A> Mono<A> taskToMono(Task<A> task) {
        Objects.requireNonNull(task, "task");
        return Mono.create(sink -> {
            Fiber<Throwable, A> fiber = task.start();
            sink.onCancel(fiber::cancel);
            fiber.join().runAsync(result -> result.either(
                error -> {
                    sink.error(error);
                    return null;
                },
                outcome -> outcome.fold(
                    () -> {
                        sink.error(new CancellationException("Task cancelled"));
                        return null;
                    },
                    error -> {
                        sink.error(error);
                        return null;
                    },
                    value -> {
                        sink.success(value);
                        return null;
                    }
                )
            ));
        });
    }

    /**
     * Converts a task of {@link Maybe} into a {@link Mono}, completing empty when the task yields {@link Maybe#nothing()}.
     */
    public static <A> Mono<A> taskMaybeToMono(Task<Maybe<A>> task) {
        Objects.requireNonNull(task, "task");
        return Mono.create(sink -> {
            Fiber<Throwable, Maybe<A>> fiber = task.start();
            sink.onCancel(fiber::cancel);
            fiber.join().runAsync(result -> result.either(
                error -> {
                    sink.error(error);
                    return null;
                },
                outcome -> outcome.fold(
                    () -> {
                        sink.error(new CancellationException("Task cancelled"));
                        return null;
                    },
                    error -> {
                        sink.error(error);
                        return null;
                    },
                    maybe -> maybe.either(
                        () -> {
                            sink.success();
                            return null;
                        },
                        value -> {
                            sink.success(value);
                            return null;
                        }
                    )
                )
            ));
        });
    }

    /**
     * Converts a typed-error task into a {@link Mono} using the supplied domain-to-throwable mapper.
     */
    public static <E, A> Mono<A> taskEitherToMono(TaskEither<E, A> taskEither, Function<E, Throwable> errorMapper) {
        Objects.requireNonNull(taskEither, "taskEither");
        Objects.requireNonNull(errorMapper, "errorMapper");
        return Mono.create(sink -> {
            Fiber<Throwable, Either<E, A>> fiber = taskEither.task().start();
            sink.onCancel(fiber::cancel);
            fiber.join().runAsync(result -> result.either(
                error -> {
                    sink.error(error);
                    return null;
                },
                outcome -> outcome.fold(
                    () -> {
                        sink.error(new CancellationException("Task cancelled"));
                        return null;
                    },
                    error -> {
                        sink.error(error);
                        return null;
                    },
                    either -> either.either(
                        e -> {
                            sink.error(Objects.requireNonNull(errorMapper.apply(e), "mapped error"));
                            return null;
                        },
                        value -> {
                            sink.success(value);
                            return null;
                        }
                    )
                )
            ));
        });
    }

    /**
     * Converts a {@link Flux} into an {@link AsyncStream} without eager collection.
     */
    public static <A> AsyncStream<A> fluxToAsyncStream(Flux<A> flux) {
        Objects.requireNonNull(flux, "flux");
        FluxState<A> state = new FluxState<>(flux);
        return AsyncStream.unfoldTask(state, FluxState::next).onFinalize(state.disposeTask());
    }

    /**
     * Converts an {@link AsyncStream} into a {@link Flux} while preserving cancellation and finalization.
     */
    public static <A> Flux<A> asyncStreamToFlux(AsyncStream<A> stream) {
        Objects.requireNonNull(stream, "stream");
        return Flux.create(sink -> {
            CancellationToken token = new CancellationToken();
            AtomicBoolean finalized = new AtomicBoolean(false);
            Task<Void> finalizer = stream.finalizer();

            sink.onCancel(() -> {
                token.cancel();
                runFinalizer(finalizer, finalized, () -> { }, __ -> { });
            });

            pump(stream, sink, token, () -> {
                runFinalizer(finalizer, finalized, () -> {
                    if (!sink.isCancelled()) {
                        sink.complete();
                    }
                }, error -> {
                    if (!sink.isCancelled()) {
                        sink.error(error);
                    }
                });
            }, error -> {
                runFinalizer(finalizer, finalized, () -> {
                    if (!sink.isCancelled()) {
                        sink.error(error);
                    }
                }, finalizerError -> {
                    if (!sink.isCancelled()) {
                        sink.error(finalizerError);
                    }
                });
            });
        });
    }

    private static void runFinalizer(
        Task<Void> finalizer,
        AtomicBoolean finalized,
        Runnable onSuccess,
        java.util.function.Consumer<Throwable> onFailure
    ) {
        if (finalized.compareAndSet(false, true)) {
            finalizer.runAsync(result -> result.either(
                error -> {
                    onFailure.accept(error);
                    return null;
                },
                __ -> {
                    onSuccess.run();
                    return null;
                }
            ));
        }
    }

    private static <A> void pump(
        AsyncStream<A> stream,
        FluxSink<A> sink,
        CancellationToken token,
        Runnable onComplete,
        java.util.function.Consumer<Throwable> onError
    ) {
        if (token.isCancelled()) {
            return;
        }

        stream.step().runAsync(Maybe.some(token), outcome -> {
            if (token.isCancelled()) {
                return;
            }

            outcome.either(
                error -> {
                    onError.accept(error);
                    return null;
                },
                maybe -> maybe.either(
                    () -> {
                        onComplete.run();
                        return null;
                    },
                    tuple -> {
                        sink.next(tuple.getA().orElse(null));
                        pump(tuple.getB().orElse(null), sink, token, onComplete, onError);
                        return null;
                    }
                )
            );
        });
    }

    private static final class FluxState<A> {
        private final Flux<A> flux;
        private final Queue<A> buffered = new ConcurrentLinkedQueue<>();
        private final AtomicReference<Subscription> subscription = new AtomicReference<>();
        private final AtomicReference<Pending<A>> pending = new AtomicReference<>();
        private final AtomicReference<Throwable> error = new AtomicReference<>();
        private final AtomicBoolean completed = new AtomicBoolean(false);
        private final AtomicBoolean subscribed = new AtomicBoolean(false);
        private final AtomicBoolean disposed = new AtomicBoolean(false);

        private FluxState(Flux<A> flux) {
            this.flux = flux;
        }

        private Task<Maybe<Tuple<A, FluxState<A>>>> next() {
            return Task.asyncCancelable(callback -> {
                if (disposed.get()) {
                    callback.cancel();
                    return this::dispose;
                }

                ensureSubscribed();

                A bufferedValue = buffered.poll();
                if (bufferedValue != null) {
                    callback.success(Maybe.some(Tuple.of(bufferedValue, this)));
                    return this::dispose;
                }

                Throwable failure = error.get();
                if (failure != null) {
                    callback.failure(failure);
                    return this::dispose;
                }

                if (completed.get()) {
                    callback.success(Maybe.nothing());
                    return this::dispose;
                }

                Pending<A> waiter = new Pending<>(callback);
                if (!pending.compareAndSet(null, waiter)) {
                    callback.failure(new IllegalStateException("Concurrent AsyncStream consumption is not supported"));
                    return this::dispose;
                }

                Subscription s = subscription.get();
                if (s != null) {
                    s.request(1);
                }

                return () -> {
                    if (pending.compareAndSet(waiter, null)) {
                        dispose();
                    }
                };
            });
        }

        private Task<Void> disposeTask() {
            return Task.of(() -> {
                dispose();
                return null;
            });
        }

        private void ensureSubscribed() {
            if (subscribed.compareAndSet(false, true)) {
                flux.subscribe(new BaseSubscriber<>() {
                    @Override
                    protected void hookOnSubscribe(Subscription s) {
                        subscription.set(s);
                        Pending<A> currentPending = pending.get();
                        if (currentPending != null && !disposed.get()) {
                            s.request(1);
                        }
                    }

                    @Override
                    protected void hookOnNext(A value) {
                        if (disposed.get()) {
                            return;
                        }

                        Pending<A> waiter = pending.getAndSet(null);
                        if (waiter != null) {
                            waiter.callback.success(Maybe.some(Tuple.of(value, FluxState.this)));
                        } else {
                            buffered.add(value);
                        }
                    }

                    @Override
                    protected void hookOnComplete() {
                        if (disposed.get()) {
                            return;
                        }

                        completed.set(true);
                        Pending<A> waiter = pending.getAndSet(null);
                        if (waiter != null) {
                            waiter.callback.success(Maybe.nothing());
                        }
                    }

                    @Override
                    protected void hookOnError(Throwable throwable) {
                        if (disposed.get()) {
                            return;
                        }

                        error.set(throwable);
                        Pending<A> waiter = pending.getAndSet(null);
                        if (waiter != null) {
                            waiter.callback.failure(throwable);
                        }
                    }
                });
            }
        }

        private void dispose() {
            if (disposed.compareAndSet(false, true)) {
                Subscription s = subscription.getAndSet(null);
                if (s != null) {
                    s.cancel();
                }
                pending.set(null);
                buffered.clear();
            }
        }
    }

    private static final class Pending<A> {
        private final Task.AsyncCallback<Maybe<Tuple<A, FluxState<A>>>> callback;

        private Pending(Task.AsyncCallback<Maybe<Tuple<A, FluxState<A>>>> callback) {
            this.callback = callback;
        }
    }
}
