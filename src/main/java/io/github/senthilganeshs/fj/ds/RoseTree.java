package io.github.senthilganeshs.fj.ds;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A purely functional Rose Tree (multi-way tree).
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

    @Override
    default <R> Collection<R> empty() {
        return (Collection<R>) of(null);
    }

    @Override
    @SuppressWarnings("unchecked")
    default <R> RoseTree<R> map(Function<T, R> fn) {
        return RoseTree.of(fn.apply(value()), (List<RoseTree<R>>) (List) children().map(c -> c.map(fn)));
    }

    @Override
    default Collection<T> build(T input) {
        return new RoseTreeImpl<>(value(), List.<RoseTree<T>>from(children().build(RoseTree.of(input))));
    }

    final class RoseTreeImpl<T> implements RoseTree<T> {
        private final T value;
        private final List<RoseTree<T>> children;

        RoseTreeImpl(T value, List<RoseTree<T>> children) {
            this.value = value;
            this.children = children;
        }

        @Override public T value() { return value; }
        @Override public List<RoseTree<T>> children() { return children; }

        @Override
        public <R> R foldl(R seed, BiFunction<R, T, R> fn) {
            R res = fn.apply(seed, value);
            return children.foldl(res, (acc, child) -> child.foldl(acc, fn));
        }

        @Override
        public String toString() {
            return "RoseTree(" + value + ", " + children.mkString("[", ", ", "]") + ")";
        }

        @Override
        public boolean equals(final Object other) {
            if (other instanceof RoseTree) {
                RoseTree<?> t = (RoseTree<?>) other;
                return java.util.Objects.equals(t.value(), value) && t.children().equals(children);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(value, children);
        }
    }
}
