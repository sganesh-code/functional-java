package io.github.senthilganeshs.fj.ds;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A purely functional Rose Tree (N-ary Tree).
 * Each node has a value and a list of children.
 * 
 * @param <T> The type of element.
 */
public interface RoseTree<T> extends Collection<T> {

    T value();
    List<RoseTree<T>> children();

    @SuppressWarnings("unchecked")
    @Override
    default <R> RoseTree<R> map(Function<T, R> fn) {
        return (RoseTree<R>) RoseTree.of(fn.apply(value()), children().map(child -> child.map(fn)));
    }

    static <R> RoseTree<R> of(R value) {
        return new RoseTreeImpl<>(value, List.nil());
    }

    static <R> RoseTree<R> of(R value, List<RoseTree<R>> children) {
        return new RoseTreeImpl<>(value, children);
    }

    final class RoseTreeImpl<T> implements RoseTree<T> {
        private final T value;
        private final List<RoseTree<T>> children;

        RoseTreeImpl(T value, List<RoseTree<T>> children) {
            this.value = value;
            this.children = children;
        }

        @Override
        public T value() {
            return value;
        }

        @Override
        public List<RoseTree<T>> children() {
            return children;
        }

        @Override
        public <R> Collection<R> empty() {
            return List.nil();
        }

        @Override
        public Collection<T> build(T input) {
            return new RoseTreeImpl<>(value, children.build(RoseTree.of(input)));
        }

        @Override
        public <R> R foldl(R seed, BiFunction<R, T, R> fn) {
            R res = fn.apply(seed, value);
            return children.foldl(res, (acc, child) -> child.foldl(acc, fn));
        }

        @Override
        public String toString() {
            if (children.length() == 0) {
                return "Node(" + value + ")";
            }
            return "Node(" + value + ", " + children + ")";
        }

        @Override
        public boolean equals(Object other) {
            if (other == null) return false;
            if (other == this) return true;
            if (!(other instanceof RoseTree)) return false;
            RoseTree<?> o = (RoseTree<?>) other;
            return value.equals(o.value()) && children.equals(o.children());
        }

        @Override
        public int hashCode() {
            return value.hashCode() ^ children.hashCode();
        }
    }
}
