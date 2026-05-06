package io.github.senthilganeshs.fj.ds;

/**
 * A functional builder for conditional list accumulation.
 * Acts as a purely functional "Rules Engine" evaluator.
 * 
 * @param <T> The type of the results accumulated.
 */
public final class Cond<T> {
    private final List<T> results;

    private Cond(List<T> results) {
        this.results = results;
    }

    /**
     * Creates a new, empty Cond builder.
     */
    public static <T> Cond<T> empty() {
        return new Cond<>(List.nil());
    }

    /**
     * Accumulates the given value if the condition is true.
     */
    public Cond<T> when(boolean condition, T value) {
        if (condition) {
            return new Cond<>(List.from(results.build(value)));
        }
        return this;
    }

    /**
     * Evaluates the builder and returns the accumulated List.
     */
    public List<T> evaluate() {
        return results;
    }
}
