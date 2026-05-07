package io.github.senthilganeshs.fj.ds;

/**
 * Store-neutral failure description for graph materialization and persistence.
 */
public record GraphStoreError(String message, Throwable cause) {
    public static GraphStoreError of(String message) {
        return new GraphStoreError(message, null);
    }

    public static GraphStoreError of(String message, Throwable cause) {
        return new GraphStoreError(message, cause);
    }
}
