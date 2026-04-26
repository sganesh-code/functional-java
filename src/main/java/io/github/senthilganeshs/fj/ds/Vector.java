package io.github.senthilganeshs.fj.ds;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BiFunction;

/**
 * Persistent Vector implementation using a Bitmapped Vector Trie.
 * 
 * @param <T> The type of elements.
 */
public interface Vector<T> extends Collection<T> {

    Maybe<T> at(int index);

    Vector<T> update(int index, T value);

    @SuppressWarnings("unchecked")
    static <R> Vector<R> from(Collection<R> c) {
        return (Vector<R>) c.foldl(Vector.<R>nil(), (v, r) -> (Vector<R>) v.build(r));
    }

    @SuppressWarnings("unchecked")
    @Override
    default <R> Vector<R> map(java.util.function.Function<T, R> fn) {
        return from(Collection.super.map(fn));
    }

    @SuppressWarnings("unchecked")
    @Override
    default <R> Vector<R> mapMaybe(java.util.function.Function<T, Maybe<R>> fn) {
        return from(Collection.super.mapMaybe(fn));
    }

    @SuppressWarnings("unchecked")
    @Override
    default Vector<T> filter(java.util.function.Predicate<T> pred) {
        return from(Collection.super.filter(pred));
    }

    @SuppressWarnings("unchecked")
    @Override
    default Vector<T> take(int n) {
        return from(Collection.super.take(n));
    }

    @SuppressWarnings("unchecked")
    @Override
    default Vector<T> drop(int n) {
        return from(Collection.super.drop(n));
    }

    static <R> Vector<R> nil() {
        return new VectorImpl<>(0, 5, null, null);
    }

    @SafeVarargs
    static <R> Vector<R> of(R... values) {
        Vector<R> v = nil();
        if (values == null) return v;
        for (R val : values) {
            v = (Vector<R>) v.build(val);
        }
        return v;
    }

    final class VectorImpl<T> implements Vector<T> {
        private static final int BITS = 5;
        private static final int WIDTH = 1 << BITS;
        private static final int MASK = WIDTH - 1;

        private final int size;
        private final int shift;
        private final Node root;
        private final Object[] tail;

        VectorImpl(int size, int shift, Node root, Object[] tail) {
            this.size = size;
            this.shift = shift;
            this.root = root;
            this.tail = tail;
        }

        private int tailOffset() {
            if (size < WIDTH) return 0;
            return ((size - 1) >> BITS) << BITS;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Maybe<T> at(int index) {
            if (index < 0 || index >= size) return Maybe.nothing();
            if (index >= tailOffset()) {
                return Maybe.some((T) tail[index & MASK]);
            }
            Node node = root;
            for (int level = shift; level > 0; level -= BITS) {
                node = (Node) node.array[(index >> level) & MASK];
            }
            return Maybe.some((T) node.array[index & MASK]);
        }

        @Override
        public Vector<T> update(int index, T value) {
            if (index < 0 || index >= size) return this;
            if (index >= tailOffset()) {
                Object[] newTail = new Object[tail.length];
                System.arraycopy(tail, 0, newTail, 0, tail.length);
                newTail[index & MASK] = value;
                return new VectorImpl<>(size, shift, root, newTail);
            }
            return new VectorImpl<>(size, shift, doUpdate(shift, root, index, value), tail);
        }

        private Node doUpdate(int level, Node node, int index, T value) {
            Node newNode = new Node(node.array.clone());
            if (level == 0) {
                newNode.array[index & MASK] = value;
            } else {
                int subIndex = (index >> level) & MASK;
                newNode.array[subIndex] = doUpdate(level - BITS, (Node) node.array[subIndex], index, value);
            }
            return newNode;
        }

        @Override
        public <R> Collection<R> empty() {
            return Vector.nil();
        }

        @Override
        public Collection<T> build(T input) {
            if (size - tailOffset() < WIDTH) {
                Object[] newTail;
                if (tail == null) {
                    newTail = new Object[1];
                    newTail[0] = input;
                } else {
                    newTail = new Object[tail.length + 1];
                    System.arraycopy(tail, 0, newTail, 0, tail.length);
                    newTail[tail.length] = input;
                }
                return new VectorImpl<>(size + 1, shift, root, newTail);
            }

            Node newRoot;
            int newShift = shift;
            Node tailNode = new Node(tail);
            if ((size >> BITS) > (1 << newShift)) {
                newRoot = new Node(new Object[WIDTH]);
                newRoot.array[0] = root;
                newRoot.array[1] = newPath(newShift, tailNode);
                newShift += BITS;
            } else {
                newRoot = pushTail(newShift, root, tailNode);
            }

            Object[] newTail = new Object[1];
            newTail[0] = input;
            return new VectorImpl<>(size + 1, newShift, newRoot, newTail);
        }

        private Node pushTail(int level, Node parent, Node tailNode) {
            int subIndex = ((size - 1) >> level) & MASK;
            Node newNode = new Node(parent == null ? new Object[WIDTH] : parent.array.clone());
            
            if (level == BITS) {
                newNode.array[subIndex] = tailNode;
            } else {
                Node child = (Node) newNode.array[subIndex];
                newNode.array[subIndex] = pushTail(level - BITS, child, tailNode);
            }
            return newNode;
        }

        private Node newPath(int level, Node node) {
            if (level == 0) return node;
            Node ret = new Node(new Object[WIDTH]);
            ret.array[0] = newPath(level - BITS, node);
            return ret;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R> R foldl(R seed, BiFunction<R, T, R> fn) {
            R res = seed;
            for (int i = 0; i < size; i++) {
                res = fn.apply(res, at(i).orElse(null));
            }
            return res;
        }

        @Override
        public String toString() {
            return foldl("[", (r, t) -> r + (r.equals("[") ? "" : ",") + t) + "]";
        }

        @Override
        public boolean equals(final Object other) {
            if (other == null) return false;
            if (other == this) return true;
            if (other instanceof Vector) {
                Vector<?> v = (Vector<?>) other;
                if (v.length() != size) return false;
                return this.toString().equals(v.toString());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        private static class Node {
            Object[] array;
            Node(Object[] array) { this.array = array; }
        }
    }
}
