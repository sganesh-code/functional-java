package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.typeclass.Monad;
import java.util.concurrent.*;
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

    public static <A> Task<A> of(Supplier<A> supplier) {
        return new Task<>(token -> CompletableFuture.supplyAsync(() -> {
            token.forEach(CancellationToken::throwIfCancelled);
            return supplier.get();
        }));
    }

    public static <A> Task<A> of(Supplier<A> supplier, Executor executor) {
        return new Task<>(token -> CompletableFuture.supplyAsync(() -> {
            token.forEach(CancellationToken::throwIfCancelled);
            return supplier.get();
        }, executor));
    }

    public static <A> Task<A> succeed(A value) {
        return new Task<>(token -> CompletableFuture.completedFuture(value));
    }

    public static <A> Task<A> fail(Throwable t) {
        return new Task<>(token -> CompletableFuture.failedFuture(t));
    }

    public <B> Task<B> map(Function<A, B> fn) {
        return new Task<>(token -> toFuture(token).thenApply(fn));
    }

    public <B> Task<B> flatMap(Function<A, Task<B>> fn) {
        return new Task<>(token -> toFuture(token).thenCompose(a -> fn.apply(a).toFuture(token)));
    }

    public <B, C> Task<C> liftA2(BiFunction<A, B, C> fn, Task<B> second) {
        return new Task<>(token -> toFuture(token).thenCombine(second.toFuture(token), fn));
    }

    public static final Monad<µ> monad = new Monad<>() {
        @Override
        public <A> Higher<µ, A> pure(A a) { return Task.succeed(a); }

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
     */
    public A run() {
        return run(Maybe.nothing());
    }

    public A run(Maybe<CancellationToken> token) {
        try {
            return toFuture(token).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
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
                result.completeExceptionally(ex);
            }
            return null;
        });
    }

    /**
     * Races multiple tasks and returns the result of the first to complete.
     */
    public static <A> Task<A> race(Collection<Task<A>> tasks) {
        return new Task<>(token -> {
            CompletableFuture<A> result = new CompletableFuture<>();
            tasks.forEach(task -> task.toFuture(token).thenAccept(result::complete));
            return result;
        });
    }

    /**
     * Resource-safe bracket operation.
     */
    public static <R, A> Task<A> bracket(Task<R> acquire, Function<R, Task<A>> use, Function<R, Task<Void>> release) {
        return acquire.flatMap(resource -> 
            use.apply(resource).flatMap(a -> 
                release.apply(resource).map(__ -> a)
            )
        );
    }

    /**
     * Executes a list of tasks in parallel and collects the results into a single list.
     */
    public static <A, B> Task<List<B>> parTraverse(List<A> items, Function<A, Task<B>> fn) {
        return new Task<>(token -> {
            List<CompletableFuture<B>> futures = List.from(items.map(a -> fn.apply(a).toFuture(token)));
            
            CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.foldl(new java.util.ArrayList<CompletableFuture<?>>(), (acc, f) -> {
                    acc.add(f);
                    return acc;
                }).toArray(new CompletableFuture[0])
            );

            return allOf.thenApply(__ -> 
                List.from(futures.map(f -> f.join()))
            );
        });
    }

    /**
     * Bounded parallel traverse to limit concurrency.
     */
    public static <A, B> Task<List<B>> boundedParTraverse(int limit, List<A> items, Function<A, Task<B>> fn) {
        ExecutorService executor = Executors.newFixedThreadPool(limit);
        return parTraverse(items, a -> Task.of(() -> fn.apply(a).run(), executor))
            .flatMap(res -> {
                executor.shutdown();
                return Task.succeed(res);
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
}
