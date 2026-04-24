package io.github.senthilganeshs.fj.ds;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A purely functional Rose Tree (N-ary Tree).
 * Each node has a value and a list of children.
 */
public interface RoseTree<T> extends Collection<T> {

    T value();
    List<RoseTree<T>> children();

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
            // A Rose Tree must have at least one node (the root), 
            // so 'empty' is tricky. We'll return nil() List or similar 
            // if we need to participate in Collection operations that build.
            return List.nil();
        }

        @Override
        public Collection<T> build(T input) {
            // Adding a child to the root
            return new RoseTreeImpl<>(value, children.build(RoseTree.of(input)));
        }

        @Override
        public <R> R foldl(R seed, BiFunction<R, T, R> fn) {
            // Pre-order traversal
            R res = fn.apply(seed, value);
            return children.foldl(res, (r, child) -> child.foldl(r, fn));
        }

        @Override
        public <R> Collection<R> map(Function<T, R> fn) {
            return new RoseTreeImpl<>(
                fn.apply(value), 
                (List<RoseTree<R>>) children.map(child -> (RoseTree<R>) child.map(fn))
            );
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
