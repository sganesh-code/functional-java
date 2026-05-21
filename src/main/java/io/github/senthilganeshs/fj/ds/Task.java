package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.typeclass.Monad;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A purely functional abstraction for an asynchronous computation.
 *
 * @param <A> The type of the value produced by the task.
 */
public final class Task<A> implements Higher<Task.µ, A> {
    public final static class µ {}

    @FunctionalInterface
    public interface Canceler {
        void cancel();
    }

    public interface AsyncCallback<A> {
        void success(A value);
        void failure(Throwable error);
        void cancel();
    }

    @SuppressWarnings("unchecked")
    public static <A> Task<A> narrowK(Higher<µ, A> hka) {
        return (Task<A>) hka;
    }

    private final Function<Maybe<CancellationToken>, CompletableFuture<A>> futureFactory;

    private Task(Function<Maybe<CancellationToken>, CompletableFuture<A>> futureFactory) {
        this.futureFactory = futureFactory;
    }

    public CompletableFuture<A> toFuture(Maybe<CancellationToken> token) {
        return futureFactory.apply(token);
    }

    /**
     * Defers construction of the underlying task until interpretation time.
     */
    public static <A> Task<A> defer(Supplier<Task<A>> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return new Task<>(token -> supplier.get().toFuture(token));
    }

    public static <A> Task<A> of(Supplier<A> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return defer(() -> new Task<>(token -> CompletableFuture.supplyAsync(() -> {
            token.forEach(CancellationToken::throwIfCancelled);
            return supplier.get();
        })));
    }

    public static <A> Task<A> of(Supplier<A> supplier, Executor executor) {
        Objects.requireNonNull(supplier, "supplier");
        Objects.requireNonNull(executor, "executor");
        return defer(() -> new Task<>(token -> CompletableFuture.supplyAsync(() -> {
            token.forEach(CancellationToken::throwIfCancelled);
            return supplier.get();
        }, executor)));
    }

    /**
     * Creates a Task from a callback-based asynchronous API.
     */
    public static <A> Task<A> async(java.util.function.Consumer<java.util.function.Consumer<A>> callback) {
        Objects.requireNonNull(callback, "callback");
        return asyncCancelable(bridge -> {
            callback.accept(bridge::success);
            return () -> { };
        });
    }

    /**
     * Creates a task from a callback-based API with explicit success, failure, and cancellation handling.
     */
    public static <A> Task<A> asyncCancelable(Function<AsyncCallback<A>, Canceler> register) {
        Objects.requireNonNull(register, "register");
        return new Task<>(token -> {
            CompletableFuture<A> future = new CompletableFuture<>();
            AtomicBoolean terminal = new AtomicBoolean(false);
            AtomicBoolean cancelRequested = new AtomicBoolean(false);
            AtomicReference<Canceler> cancelerRef = new AtomicReference<>(() -> { });

            AsyncCallback<A> callback = new AsyncCallback<>() {
                @Override
                public void success(A value) {
                    completeTerminal(() -> future.complete(value));
                }

                @Override
                public void failure(Throwable error) {
                    completeTerminal(() -> future.completeExceptionally(error));
                }

                @Override
                public void cancel() {
                    completeTerminal(() -> future.completeExceptionally(new CancellationException("Task cancelled")));
                }

                private void completeTerminal(Supplier<Boolean> action) {
                    if (terminal.compareAndSet(false, true)) {
                        action.get();
                    }
                }
            };

            Runnable cancelAction = () -> {
                cancelRequested.set(true);
                if (terminal.compareAndSet(false, true)) {
                    try {
                        cancelerRef.get().cancel();
                    } finally {
                        future.completeExceptionally(new CancellationException("Task cancelled"));
                    }
                } else {
                    cancelerRef.get().cancel();
                }
            };

            token.forEach(t -> t.onCancel(cancelAction));
            if (token.isSome() && token.orElse(null).isCancelled()) {
                cancelAction.run();
            }

            try {
                Canceler canceler = register.apply(callback);
                cancelerRef.set(canceler == null ? () -> { } : canceler);
            } catch (Throwable t) {
                callback.failure(t);
                return future;
            }

            if (cancelRequested.get()) {
                cancelerRef.get().cancel();
            }
            return future;
        });
    }

    public static <A> Task<A> succeed(A value) {
        return new Task<>(token -> CompletableFuture.completedFuture(value));
    }

    public static <A> Task<A> fail(Throwable t) {
        return new Task<>(token -> CompletableFuture.failedFuture(t));
    }

    static <A> Task<A> fromFuture(CompletableFuture<A> future) {
        Objects.requireNonNull(future, "future");
        return new Task<>(token -> future);
    }

    public <B> Task<B> map(Function<A, B> fn) {
        Objects.requireNonNull(fn, "fn");
        return new Task<>(token -> toFuture(token).thenApply(fn));
    }

    public <B> Task<B> flatMap(Function<A, Task<B>> fn) {
        Objects.requireNonNull(fn, "fn");
        return new Task<>(token -> toFuture(token).thenCompose(a -> fn.apply(a).toFuture(token)));
    }

    public <B, C> Task<C> liftA2(BiFunction<A, B, C> fn, Task<B> second) {
        Objects.requireNonNull(fn, "fn");
        Objects.requireNonNull(second, "second");
        return new Task<>(token -> toFuture(token).thenCombine(second.toFuture(token), fn));
    }

    /**
     * Starts the task and returns a fiber that can be joined or cancelled.
     */
    public Fiber<Throwable, A> start() {
        CancellationToken cancellationToken = new CancellationToken();
        CompletableFuture<Outcome<Throwable, A>> started = toFuture(Maybe.some(cancellationToken))
            .handle((value, error) -> {
                if (error == null) {
                    return Outcome.succeeded(value);
                }
                Throwable cause = unwrap(error);
                if (cause instanceof CancellationException) {
                    return Outcome.cancelled();
                }
                return Outcome.failed(cause);
            });
        return new Fiber<>(cancellationToken, Task.fromFuture(started));
    }

    /**
     * Recovers from a failure by mapping the error to a fallback value.
     */
    public Task<A> recover(Function<Throwable, A> fn) {
        Objects.requireNonNull(fn, "fn");
        return new Task<A>(token -> {
            CompletableFuture<CompletableFuture<A>> handled = toFuture(token).handle((val, ex) -> {
                if (ex == null) {
                    return CompletableFuture.completedFuture(val);
                }
                Throwable cause = unwrap(ex);
                if (cause instanceof CancellationException) {
                    return failedFuture(cause);
                }
                return CompletableFuture.completedFuture(fn.apply(cause));
            });
            return handled.thenCompose(inner -> inner);
        });
    }

    /**
     * Recovers from a failure by switching to another task.
     */
    public Task<A> recoverWith(Function<Throwable, Task<A>> fn) {
        Objects.requireNonNull(fn, "fn");
        return new Task<A>(token -> {
            CompletableFuture<CompletableFuture<A>> handled = toFuture(token).handle((val, ex) -> {
                if (ex == null) {
                    return CompletableFuture.completedFuture(val);
                }
                Throwable cause = unwrap(ex);
                if (cause instanceof CancellationException) {
                    return failedFuture(cause);
                }
                return fn.apply(cause).toFuture(token);
            });
            return handled.thenCompose(inner -> inner);
        });
    }

    /**
     * Maps a failure to another failure without changing successful values.
     */
    public Task<A> mapError(Function<Throwable, Throwable> fn) {
        Objects.requireNonNull(fn, "fn");
        return new Task<A>(token -> {
            CompletableFuture<CompletableFuture<A>> handled = toFuture(token).handle((val, ex) -> {
                if (ex == null) {
                    return CompletableFuture.completedFuture(val);
                }
                Throwable cause = unwrap(ex);
                if (cause instanceof CancellationException) {
                    return failedFuture(cause);
                }
                Throwable mapped = Objects.requireNonNull(fn.apply(cause), "mapped error");
                return failedFuture(mapped);
            });
            return handled.thenCompose(inner -> inner);
        });
    }

    public static final Monad<µ> monad = new Monad<>() {
        @Override
        public <A> Higher<µ, A> pure(A a) {
            return Task.succeed(a);
        }

        @Override
        public <A, B> Higher<µ, B> flatMap(Function<A, Higher<µ, B>> fn, Higher<µ, A> fa) {
            return narrowK(fa).flatMap(a -> narrowK(fn.apply(a)));
        }

        @Override
        public <A, B> Higher<µ, B> map(Function<A, B> fn, Higher<µ, A> fa) {
            return narrowK(fa).map(fn);
        }
    };

    /**
     * Executes the task and blocks until the result is available.
     * Note: Avoid calling this from a managed thread pool to prevent starvation deadlocks.
     */
    public A run() {
        return run(Maybe.nothing());
    }

    public A runSync() {
        return run();
    }

    public void runSync(java.util.function.Consumer<A> callback) {
        callback.accept(run());
    }

    public A run(Maybe<CancellationToken> token) {
        try {
            return toFuture(token).get();
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException(unwrap(e));
        }
    }

    /**
     * Executes the task asynchronously and provides the result to a callback.
     * This is the preferred non-blocking execution method.
     */
    public void runAsync(java.util.function.Consumer<Either<Throwable, A>> callback) {
        runAsync(Maybe.nothing(), callback);
    }

    public void runAsync(Maybe<CancellationToken> token, java.util.function.Consumer<Either<Throwable, A>> callback) {
        toFuture(token).whenComplete((val, ex) -> {
            if (ex != null) {
                callback.accept(Either.left(unwrap(ex)));
            } else {
                callback.accept(Either.right(val));
            }
        });
    }

    /**
     * Adds a timeout to the task.
     */
    public Task<A> timeout(long timeout, TimeUnit unit) {
        return new Task<>(token -> toFuture(token).orTimeout(timeout, unit));
    }

    /**
     * Retries the task if it fails, using a basic retry strategy.
     */
    public Task<A> retry(int maxRetries) {
        return retry(maxRetries, 0, TimeUnit.MILLISECONDS);
    }

    public Task<A> retry(int maxRetries, long delay, TimeUnit unit) {
        return new Task<>(token -> {
            CompletableFuture<A> result = new CompletableFuture<>();
            attemptRetry(maxRetries, delay, unit, result, token);
            return result;
        });
    }

    private void attemptRetry(int remaining, long delay, TimeUnit unit, CompletableFuture<A> result, Maybe<CancellationToken> token) {
        toFuture(token).handle((val, ex) -> {
            if (ex == null) {
                result.complete(val);
            } else if (remaining > 0) {
                if (delay > 0) {
                    CompletableFuture.delayedExecutor(delay, unit).execute(() ->
                        attemptRetry(remaining - 1, delay, unit, result, token)
                    );
                } else {
                    attemptRetry(remaining - 1, delay, unit, result, token);
                }
            } else {
                result.completeExceptionally(unwrap(ex));
            }
            return null;
        });
    }

    /**
     * Handles both result and exception, producing a new value.
     * Useful for recovery (e.g. timeout fallbacks).
     */
    public <B> Task<B> handle(BiFunction<A, Throwable, B> fn) {
        return new Task<>(token -> toFuture(token).handle((val, ex) -> fn.apply(val, ex)));
    }

    /**
     * Races multiple tasks and returns the result of the first to complete.
     */
    public static <A> Task<A> race(Collection<Task<A>> tasks) {
        if (tasks.isEmpty()) {
            return Task.fail(new IllegalArgumentException("race requires at least one task"));
        }
        return Task.asyncCancelable(callback -> {
            java.util.List<Fiber<Throwable, A>> fibers = new java.util.ArrayList<>();
            tasks.forEach(task -> fibers.add(task.start()));

            AtomicBoolean done = new AtomicBoolean(false);
            Runnable cancelLosers = () -> fibers.forEach(Fiber::cancel);

            fibers.forEach(fiber -> fiber.join().runAsync(outcome -> {
                if (done.compareAndSet(false, true)) {
                    outcome.either(
                        failure -> {
                            callback.failure(failure);
                            return null;
                        },
                        winner -> {
                            if (winner.isCancelled()) {
                                callback.cancel();
                            } else if (winner.isFailed()) {
                                Outcome.Failed<Throwable, A> failed = (Outcome.Failed<Throwable, A>) winner;
                                callback.failure(failed.error());
                            } else {
                                Outcome.Succeeded<Throwable, A> succeeded = (Outcome.Succeeded<Throwable, A>) winner;
                                callback.success(succeeded.value());
                            }
                            return null;
                        }
                    );
                    cancelLosers.run();
                }
            }));

            return cancelLosers::run;
        });
    }

    /**
     * Resource-safe bracket operation.
     */
    public static <R, A> Task<A> bracket(Task<R> acquire, Function<R, Task<A>> use, Function<R, Task<Void>> release) {
        return acquire.flatMap(resource -> new Task<>(token -> {
            CompletableFuture<A> result = new CompletableFuture<>();
            use.apply(resource).toFuture(token).handle((val, ex) -> {
                release.apply(resource).toFuture(Maybe.nothing()).handle((__, ex2) -> {
                    if (ex != null) {
                        result.completeExceptionally(unwrap(ex));
                    } else if (ex2 != null) {
                        result.completeExceptionally(unwrap(ex2));
                    } else {
                        result.complete(val);
                    }
                    return null;
                });
                return null;
            });
            return result;
        }));
    }

    /**
     * Executes a list of tasks in parallel and collects the results into a single list.
     * Guaranteed to initiate all tasks concurrently before waiting for results.
     */
    public static <A, B> Task<List<B>> parTraverse(List<A> items, Function<A, Task<B>> fn) {
        return new Task<>(token -> {
            List<CompletableFuture<B>> futures = (List<CompletableFuture<B>>) items.foldl(List.<CompletableFuture<B>>nil(), (acc, a) ->
                (List<CompletableFuture<B>>) acc.build(fn.apply(a).toFuture(token)));

            CompletableFuture<?>[] array = new CompletableFuture<?>[futures.length()];
            final int[] i = {0};
            futures.forEach(f -> array[i[0]++] = f);

            return CompletableFuture.allOf(array).thenApply(__ ->
                List.from(futures.map(CompletableFuture::join))
            );
        });
    }

    /**
     * Bounded parallel traverse to limit concurrency.
     */
    public static <A, B> Task<List<B>> boundedParTraverse(int limit, List<A> items, Function<A, Task<B>> fn) {
        return boundedParTraverse(Executors.newFixedThreadPool(limit), items, fn, true);
    }

    /**
     * Executes a list of tasks in parallel using the provided executor.
     * Truly non-blocking: does not use .run() or .get() internally.
     */
    public static <A, B> Task<List<B>> boundedParTraverse(ExecutorService executor, List<A> items, Function<A, Task<B>> fn, boolean shutdownExecutor) {
        return new Task<>(token -> {
            List<CompletableFuture<B>> futures = (List<CompletableFuture<B>>) items.foldl(List.<CompletableFuture<B>>nil(), (acc, a) ->
                (List<CompletableFuture<B>>) acc.build(CompletableFuture.supplyAsync(() -> fn.apply(a), executor)
                    .thenCompose(t -> t.toFuture(token))));

            CompletableFuture<?>[] array = new CompletableFuture<?>[futures.length()];
            final int[] i = {0};
            futures.forEach(f -> array[i[0]++] = f);

            CompletableFuture<List<B>> result = CompletableFuture.allOf(array).thenApply(__ ->
                List.from(futures.map(CompletableFuture::join))
            );

            if (shutdownExecutor) {
                result.whenComplete((__, ex) -> executor.shutdown());
            }
            return result;
        });
    }

    /**
     * Converts a collection of tasks into a task of a collection, executing in parallel.
     */
    @SuppressWarnings("unchecked")
    public static <A> Task<Collection<A>> sequence(Collection<Task<A>> tasks) {
        List<Task<A>> list = (tasks instanceof List) ? (List<Task<A>>) tasks : List.from(tasks);
        return parTraverse(list, Function.identity()).map(l -> (Collection<A>) l);
    }

    /**
     * Converts a collection of tasks into a task of a collection, executing them sequentially.
     * Useful when tasks have dependencies or should not run concurrently.
     */
    public static <A> Task<List<A>> sequenceSequential(List<Task<A>> tasks) {
        return tasks.foldl(Task.succeed(List.<A>nil()), (accTask, task) ->
            accTask.flatMap(list -> task.map(a -> List.from(list.build(a))))
        );
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof CompletionException || current instanceof ExecutionException) && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static <A> CompletableFuture<A> failedFuture(Throwable throwable) {
        CompletableFuture<A> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);
        return future;
    }
}
