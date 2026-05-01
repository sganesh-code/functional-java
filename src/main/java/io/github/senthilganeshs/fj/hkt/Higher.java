package io.github.senthilganeshs.fj.hkt;

/**
 * A witness interface for Higher-Kinded Types in Java.
 * 
 * <p>Used to represent a type constructor F applied to type argument A as F&lt;A&gt;.</p>
 * 
 * @param <W> The witness type representing the type constructor (e.g., Maybe.µ).
 * @param <A> The type argument.
 */
public interface Higher<W, A> {
}
