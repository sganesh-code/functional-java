package io.github.senthilganeshs.fj.optic;

import io.github.senthilganeshs.fj.ds.Maybe;

/**
 * Automated Prism generation for Java 17+ Sealed Interfaces.
 */
public final class SealedOptics {

    private SealedOptics() {}

    /**
     * Automatically creates a Prism for a specific case (subclass) of a sealed interface.
     * 
     * @param <S> The base sealed type
     * @param <T> The target subtype
     * @param baseClass The class of the sealed interface/class
     * @param targetClass The class of the target subtype
     * @return A Prism focusing on the target type
     */
    public static <S, T extends S> Prism<S, T> prism(Class<S> baseClass, Class<T> targetClass) {
        return Prism.of(
            s -> targetClass.isInstance(s) ? Maybe.some(targetClass.cast(s)) : Maybe.nothing(),
            t -> t
        );
    }
}
