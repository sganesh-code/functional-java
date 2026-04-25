package io.github.senthilganeshs.fj.ds;

/**
 * An algebraic structure consisting of a set together with a single 
 * associative binary operation.
 */
public interface Semigroup<T> {
    T combine(T a, T b);
}
