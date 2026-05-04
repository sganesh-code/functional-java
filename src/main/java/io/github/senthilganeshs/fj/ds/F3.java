package io.github.senthilganeshs.fj.ds;

@FunctionalInterface
public interface F3<A, B, C, R> {
    R apply(A a, B b, C c);
}
