package io.github.senthilganeshs.fj.ds;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A purely functional abstraction for an asynchronous computation.
 * 
 * @param <A> The type of the value produced by the task.
 */
public final class Task<A> {
    private final Supplier<CompletableFuture<A>> futureSupplier;

    private Task(Supplier<CompletableFuture<A>> futureSupplier) {
        this.futureSupplier = futureSupplier;
    }

    public static <A> Task<A> of(Supplier<A> supplier) {
        return new Task<>(() -> CompletableFuture.supplyAsync(supplier));
    }

    public static <A> Task<A> of(Supplier<A> supplier, Executor executor) {
        return new Task<>(() -> CompletableFuture.supplyAsync(supplier, executor));
    }

    public static <A> Task<A> succeed(A value) {
        return new Task<>(() -> CompletableFuture.completedFuture(value));
    }

    public <B> Task<B> map(Function<A, B> fn) {
        return new Task<>(() -> futureSupplier.get().thenApply(fn));
    }

    public <B> Task<B> flatMap(Function<A, Task<B>> fn) {
        return new Task<>(() -> futureSupplier.get().thenCompose(a -> fn.apply(a).futureSupplier.get()));
    }

    public <B, C> Task<C> liftA2(BiFunction<A, B, C> fn, Task<B> second) {
        return new Task<>(() -> futureSupplier.get().thenCombine(second.futureSupplier.get(), fn));
    }

    /**
     * Executes the task and blocks until the result is available.
     */
    public A run() {
        return futureSupplier.get().join();
    }

    /**
     * Executes a list of tasks in parallel and collects the results into a single list.
     */
    public static <A, B> Task<List<B>> parTraverse(List<A> items, Function<A, Task<B>> fn) {
        return new Task<>(() -> {
            List<CompletableFuture<B>> futures = List.from(items.map(a -> fn.apply(a).futureSupplier.get()));
            
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
     * Converts a collection of tasks into a task of a collection, executing in parallel.
     */
    public static <A> Task<Collection<A>> sequence(Collection<Task<A>> tasks) {
        List<Task<A>> list = (tasks instanceof List) ? (List<Task<A>>) tasks : List.from(tasks);
        return parTraverse(list, Function.identity()).map(l -> (Collection<A>) l);
    }
}
