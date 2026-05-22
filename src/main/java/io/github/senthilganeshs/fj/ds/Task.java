package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.typeclass.Monad;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

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

    private final Function<Maybe<CancellationToken>, Mono<Value<A>>> monoFactory;

    private Task(Function<Maybe<CancellationToken>, Mono<Value<A>>> monoFactory) {
        this.monoFactory = monoFactory;
    }

    /**
     * Converts this task to a Reactor {@link Mono}. Java {@code null} results are represented
     * as empty Mono completion because Reactor publishers cannot emit null values.
     */
    public Mono<A> toMono(Maybe<CancellationToken> token) {
        Objects.requireNonNull(token, "token");
        return toInternalMono(token).flatMap(value ->
            value.value() == null ? Mono.empty() : Mono.just(value.value())
        );
    }

    public CompletableFuture<A> toFuture(Maybe<CancellationToken> token) {
        Objects.requireNonNull(token, "token");
        CompletableFuture<A> future = new CompletableFuture<>();
        AtomicReference<Disposable> disposable = new AtomicReference<>();

        Disposable current = toInternalMono(token).subscribe(
            value -> future.complete(value.value()),
            error -> future.completeExceptionally(unwrap(error)),
            () -> future.complete(null)
        );
        disposable.set(current);
        future.whenComplete((__, ___) -> {
            if (future.isCancelled()) {
                Disposable d = disposable.get();
                if (d != null) {
                    d.dispose();
                }
            }
        });
        return future;
    }

    /**
     * Defers construction of the underlying task until interpretation time.
     */
    public static <A> Task<A> defer(Supplier<Task<A>> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return new Task<>(token -> Mono.defer(() -> supplier.get().toInternalMono(token)));
    }

    public static <A> Task<A> of(Supplier<A> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return defer(() -> new Task<>(token -> Mono.fromSupplier(() -> {
            token.forEach(CancellationToken::throwIfCancelled);
            return Value.of(supplier.get());
        }).subscribeOn(Schedulers.boundedElastic())));
    }

    public static <A> Task<A> of(Supplier<A> supplier, Executor executor) {
        Objects.requireNonNull(supplier, "supplier");
        Objects.requireNonNull(executor, "executor");
        Scheduler scheduler = Schedulers.fromExecutor(executor);
        return defer(() -> new Task<>(token -> Mono.fromSupplier(() -> {
            token.forEach(CancellationToken::throwIfCancelled);
            return Value.of(supplier.get());
        }).subscribeOn(scheduler)));
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
        return new Task<>(token -> Mono.create(sink -> {
            AtomicBoolean terminal = new AtomicBoolean(false);
            AtomicBoolean cancelRequested = new AtomicBoolean(false);
            AtomicBoolean cancelerCalled = new AtomicBoolean(false);
            AtomicReference<Canceler> cancelerRef = new AtomicReference<>();

            Runnable invokeCanceler = () -> {
                Canceler canceler = cancelerRef.get();
                if (canceler != null && cancelerCalled.compareAndSet(false, true)) {
                    canceler.cancel();
                }
            };

            AsyncCallback<A> callback = new AsyncCallback<>() {
                @Override
                public void success(A value) {
                    if (terminal.compareAndSet(false, true)) {
                        sink.success(Value.of(value));
                    }
                }

                @Override
                public void failure(Throwable error) {
                    if (terminal.compareAndSet(false, true)) {
                        sink.error(error);
                    }
                }

                @Override
                public void cancel() {
                    if (terminal.compareAndSet(false, true)) {
                        sink.error(new CancellationException("Task cancelled"));
                    }
                }
            };

            Runnable cancelAction = () -> {
                cancelRequested.set(true);
                if (terminal.compareAndSet(false, true)) {
                    try {
                        invokeCanceler.run();
                    } finally {
                        sink.error(new CancellationException("Task cancelled"));
                    }
                }
            };

            sink.onCancel(new Disposable() {
                @Override
                public void dispose() {
                    cancelAction.run();
                }

                @Override
                public boolean isDisposed() {
                    return terminal.get();
                }
            });
            token.forEach(t -> t.onCancel(cancelAction));

            try {
                Canceler canceler = register.apply(callback);
                cancelerRef.set(canceler == null ? () -> { } : canceler);
            } catch (Throwable t) {
                callback.failure(t);
                return;
            }

            if (cancelRequested.get()) {
                invokeCanceler.run();
            }
        }));
    }

    public static <A> Task<A> succeed(A value) {
        return new Task<>(token -> Mono.just(Value.of(value)));
    }

    public static <A> Task<A> fail(Throwable t) {
        Objects.requireNonNull(t, "t");
        return new Task<>(token -> Mono.error(t));
    }

    public static <A> Task<A> fromMono(Mono<A> mono) {
        Objects.requireNonNull(mono, "mono");
        return new Task<>(token -> fromMonoValue(mono.map(Value::of), token));
    }

    static <A> Task<A> fromFuture(CompletableFuture<A> future) {
        Objects.requireNonNull(future, "future");
        return new Task<>(token -> Mono.create(sink ->
            future.whenComplete((value, error) -> {
                if (error != null) {
                    sink.error(unwrap(error));
                } else {
                    sink.success(Value.of(value));
                }
            })
        ));
    }

    static <A> Task<A> fromNullableMono(Mono<Value<A>> mono) {
        Objects.requireNonNull(mono, "mono");
        return new Task<>(token -> fromMonoValue(mono, token));
    }

    public <B> Task<B> map(Function<A, B> fn) {
        Objects.requireNonNull(fn, "fn");
        return new Task<>(token -> toInternalMono(token).map(value -> Value.of(fn.apply(value.value()))));
    }

    public <B> Task<B> flatMap(Function<A, Task<B>> fn) {
        Objects.requireNonNull(fn, "fn");
        return new Task<>(token -> toInternalMono(token).flatMap(value -> fn.apply(value.value()).toInternalMono(token)));
    }

    public <B, C> Task<C> liftA2(BiFunction<A, B, C> fn, Task<B> second) {
        Objects.requireNonNull(fn, "fn");
        Objects.requireNonNull(second, "second");
        return new Task<>(token -> Mono.zip(
            toInternalMono(token),
            second.toInternalMono(token),
            (a, b) -> Value.of(fn.apply(a.value(), b.value()))
        ));
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
        return new Task<>(token -> toInternalMono(token).onErrorResume(error -> {
            Throwable cause = unwrap(error);
            if (cause instanceof CancellationException) {
                return Mono.error(cause);
            }
            return Mono.just(Value.of(fn.apply(cause)));
        }));
    }

    /**
     * Recovers from a failure by switching to another task.
     */
    public Task<A> recoverWith(Function<Throwable, Task<A>> fn) {
        Objects.requireNonNull(fn, "fn");
        return new Task<>(token -> toInternalMono(token).onErrorResume(error -> {
            Throwable cause = unwrap(error);
            if (cause instanceof CancellationException) {
                return Mono.error(cause);
            }
            return fn.apply(cause).toInternalMono(token);
        }));
    }

    /**
     * Maps a failure to another failure without changing successful values.
     */
    public Task<A> mapError(Function<Throwable, Throwable> fn) {
        Objects.requireNonNull(fn, "fn");
        return new Task<>(token -> toInternalMono(token).onErrorMap(error -> {
            Throwable cause = unwrap(error);
            if (cause instanceof CancellationException) {
                return cause;
            }
            return Objects.requireNonNull(fn.apply(cause), "mapped error");
        }));
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
        Objects.requireNonNull(unit, "unit");
        return new Task<>(token -> toInternalMono(token).timeout(Duration.ofNanos(unit.toNanos(timeout))));
    }

    /**
     * Retries the task if it fails, using a basic retry strategy.
     */
    public Task<A> retry(int maxRetries) {
        return retry(maxRetries, 0, TimeUnit.MILLISECONDS);
    }

    public Task<A> retry(int maxRetries, long delay, TimeUnit unit) {
        Objects.requireNonNull(unit, "unit");
        if (delay <= 0) {
            return new Task<>(token -> toInternalMono(token).retry(maxRetries));
        }
        Retry retry = Retry.fixedDelay(maxRetries, Duration.ofNanos(unit.toNanos(delay)))
            .filter(error -> !(unwrap(error) instanceof CancellationException))
            .onRetryExhaustedThrow((__, signal) -> signal.failure());
        return new Task<>(token -> toInternalMono(token).retryWhen(retry));
    }

    /**
     * Handles both result and exception, producing a new value.
     * Useful for recovery (e.g. timeout fallbacks).
     */
    public <B> Task<B> handle(BiFunction<A, Throwable, B> fn) {
        Objects.requireNonNull(fn, "fn");
        return new Task<>(token -> toInternalMono(token)
            .map(value -> Value.of(fn.apply(value.value(), null)))
            .onErrorResume(error -> Mono.just(Value.of(fn.apply(null, unwrap(error))))));
    }

    /**
     * Races multiple tasks and returns the result of the first to complete.
     */
    public static <A> Task<A> race(Collection<Task<A>> tasks) {
        if (tasks.isEmpty()) {
            return Task.fail(new IllegalArgumentException("race requires at least one task"));
        }
        return new Task<>(token -> {
            java.util.List<Mono<Value<A>>> monos = new java.util.ArrayList<>();
            tasks.forEach(task -> monos.add(task.toInternalMono(token)));
            return Mono.firstWithSignal(monos);
        });
    }

    /**
     * Resource-safe bracket operation.
     */
    public static <R, A> Task<A> bracket(Task<R> acquire, Function<R, Task<A>> use, Function<R, Task<Void>> release) {
        Objects.requireNonNull(acquire, "acquire");
        Objects.requireNonNull(use, "use");
        Objects.requireNonNull(release, "release");
        return new Task<>(token -> Mono.usingWhen(
            acquire.toInternalMono(token),
            resource -> use.apply(resource.value()).toInternalMono(token),
            resource -> release.apply(resource.value()).toInternalMono(Maybe.nothing()).then(),
            (resource, __) -> release.apply(resource.value()).toInternalMono(Maybe.nothing()).then(),
            resource -> release.apply(resource.value()).toInternalMono(Maybe.nothing()).then()
        ));
    }

    /**
     * Executes a list of tasks in parallel and collects the results into a single list.
     * Guaranteed to initiate all tasks concurrently before waiting for results.
     */
    public static <A, B> Task<List<B>> parTraverse(List<A> items, Function<A, Task<B>> fn) {
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(fn, "fn");
        int concurrency = Math.max(1, items.length());
        return traverseWithConcurrency(concurrency, items, fn, Function.identity(), () -> { });
    }

    /**
     * Bounded parallel traverse to limit concurrency.
     */
    public static <A, B> Task<List<B>> boundedParTraverse(int limit, List<A> items, Function<A, Task<B>> fn) {
        if (limit <= 0) {
            return Task.fail(new IllegalArgumentException("limit must be positive"));
        }
        return traverseWithConcurrency(limit, items, fn, Function.identity(), () -> { });
    }

    /**
     * Executes a list of tasks in parallel using the provided executor.
     * Truly non-blocking: does not use .run() or .get() internally.
     */
    public static <A, B> Task<List<B>> boundedParTraverse(
        ExecutorService executor,
        List<A> items,
        Function<A, Task<B>> fn,
        boolean shutdownExecutor
    ) {
        Objects.requireNonNull(executor, "executor");
        Scheduler scheduler = Schedulers.fromExecutorService(executor);
        Runnable cleanup = shutdownExecutor ? executor::shutdown : () -> { };
        return traverseWithConcurrency(Integer.MAX_VALUE, items, fn, mono -> mono.subscribeOn(scheduler), cleanup);
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

    private Mono<Value<A>> toInternalMono(Maybe<CancellationToken> token) {
        return monoFactory.apply(token);
    }

    private static <A, B> Task<List<B>> traverseWithConcurrency(
        int concurrency,
        List<A> items,
        Function<A, Task<B>> fn,
        Function<Mono<Value<B>>, Mono<Value<B>>> scheduler,
        Runnable cleanup
    ) {
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(fn, "fn");
        Objects.requireNonNull(scheduler, "scheduler");
        Objects.requireNonNull(cleanup, "cleanup");
        return new Task<>(token -> Flux.fromIterable(items)
            .flatMapSequential(item -> scheduler.apply(Mono.defer(() -> fn.apply(item).toInternalMono(token))), concurrency)
            .collectList()
            .map(values -> {
                List<B> result = List.nil();
                for (Value<B> value : values) {
                    result = result.build(value.value());
                }
                return Value.of(result);
            })
            .doFinally(__ -> cleanup.run()));
    }

    private static <A> Mono<Value<A>> fromMonoValue(Mono<Value<A>> mono, Maybe<CancellationToken> token) {
        return Mono.create(sink -> {
            if (token.isSome() && token.orElse(null).isCancelled()) {
                sink.error(new CancellationException("Task cancelled"));
                return;
            }

            AtomicBoolean terminal = new AtomicBoolean(false);
            AtomicReference<Disposable> disposableRef = new AtomicReference<>();
            Runnable cancel = () -> {
                if (terminal.compareAndSet(false, true)) {
                    try {
                        sink.error(new CancellationException("Task cancelled"));
                    } finally {
                        Disposable disposable = disposableRef.get();
                        if (disposable != null) {
                            disposable.dispose();
                        }
                    }
                }
            };

            Disposable disposable = mono.subscribe(
                value -> {
                    if (terminal.compareAndSet(false, true)) {
                        sink.success(value);
                    }
                },
                error -> {
                    if (terminal.compareAndSet(false, true)) {
                        sink.error(unwrap(error));
                    }
                },
                () -> {
                    if (terminal.compareAndSet(false, true)) {
                        sink.success();
                    }
                }
            );
            disposableRef.set(disposable);
            sink.onCancel(new Disposable() {
                @Override
                public void dispose() {
                    cancel.run();
                }

                @Override
                public boolean isDisposed() {
                    return terminal.get();
                }
            });
            token.forEach(t -> t.onCancel(cancel));
        });
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof CompletionException || current instanceof ExecutionException) && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    static final class Value<A> {
        private final A value;

        private Value(A value) {
            this.value = value;
        }

        static <A> Value<A> of(A value) {
            return new Value<>(value);
        }

        A value() {
            return value;
        }
    }
}
