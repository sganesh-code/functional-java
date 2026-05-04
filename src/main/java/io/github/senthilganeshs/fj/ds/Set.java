package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.typeclass.Ord;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A purely functional Set implementation using a self-balancing AVL Tree.
 * 
 * @param <T> The type of elements in the set.
 */
public interface Set<T> extends Collection<T> {

    @Override Set<T> build(final T value);

    default Set<T> add(final T value) {
        return build(value);
    }

    boolean contains(final T value);

    /**
     * Returns the ordering strategy used by this set.
     * @return The Ord instance.
     */
    Ord<T> order();

    default Set<T> union(Set<T> other) {
        return other.foldl(this, Set::build);
    }

    default Set<T> intersect(Set<T> other) {
        return filter(other::contains).foldl(empty(order()), Set::build);
    }

    default Set<T> difference(Set<T> other) {
        return filter(t -> !other.contains(t)).foldl(empty(order()), Set::build);
    }

    static <R extends Comparable<R>> Collection<R> sort(Collection<R> collection) {
        return of(collection).foldl(collection.empty(), (rs, t) -> rs.build(t));
    }
    
    static <R extends Comparable<R>> Set<R> nil() {
        return empty(Ord.<R>natural());
    }

    static <R> Set<R> empty(Ord<R> ord) {
        return new Empty<>(ord);
    }

    static <R extends Comparable<R>> Set<R> emptyNatural() {
        return empty(Ord.<R>natural());
    }
    
    static <R extends Comparable<R>> Set<R> of(final Collection<R> values) {
        return of(Ord.<R>natural(), values);
    }

    static <R> Set<R> of(Ord<R> ord, final Collection<R> values) {
        return values.foldl(empty(ord), (r, t) -> r.build(t));
    }
    
    static <R extends Comparable<R>> Set<R> of(final java.util.Collection<R> values) {
        return of(Ord.<R>natural(), values);
    }

    static <R> Set<R> of(Ord<R> ord, final java.util.Collection<R> values) {
        Set<R> tree = empty(ord);
        for (final R value : values) {
            tree = tree.build(value);
        }
        return tree;
    }   
    
    @SafeVarargs
    static <R extends Comparable<R>> Set<R> of(R... values) {
        return of(Ord.<R>natural(), values);
    }

    @SafeVarargs
    static <R> Set<R> of(Ord<R> ord, R... values) {
        Set<R> tree = empty(ord);
        if (values == null) return tree;
        for (final R value  : values) {
            tree = tree.build(value);            
        }
        return tree;
    }

    interface AVLTree<T> extends Set<T> {
        
        @Override AVLTree<T> build(final T value);

        AVLTree<T> replaceLeft(final Function<AVLTree<T>, AVLTree<T>> left);

        AVLTree<T> replaceRight(final Function<AVLTree<T>, AVLTree<T>> right);

        AVLTree<T> rotateLeft();

        AVLTree<T> rotateRight();

        int height();
    }
   
    final class NonEmpty<T> implements AVLTree<T> {

        private final Ord<T> ord;
        private final AVLTree<T> right;
        private final AVLTree<T> left;
        private final T value;

        NonEmpty (final Ord<T> ord, final T value, final AVLTree<T> left, final AVLTree<T> right) {
            this.ord = ord;
            this.value = value;
            this.left = left;
            this.right = right;
        }

        @Override
        public Ord<T> order() {
            return ord;
        }
        
        @Override
        public <R> Collection<R> empty() {
            return List.nil();
        }

        @Override
        public <R> R foldl(R seed, BiFunction<R, T, R> fn) {
            R acc = seed;
            Deque<AVLTree<T>> stack = new ArrayDeque<>();
            AVLTree<T> curr = this;
            while (curr instanceof NonEmpty || !stack.isEmpty()) {
                while (curr instanceof NonEmpty) {
                    stack.push(curr);
                    curr = ((NonEmpty<T>) curr).left;
                }
                curr = stack.pop();
                NonEmpty<T> ne = (NonEmpty<T>) curr;
                acc = fn.apply(acc, ne.value);
                curr = ne.right;
            }
            return acc;
        }
                
        @Override
        public AVLTree<T> build(T other) {
            int cmp = ord.compare(this.value, other);
            if (cmp == 0)
                return new NonEmpty<>(ord, other, left, right);

            AVLTree<T> lf;
            AVLTree<T> rt;
            
            if (cmp > 0) {
                lf = this.left.build(other);
                rt = this.right;
            } else {
                lf = this.left;
                rt = right.build(other);
            }

            int lfh = lf.height();
            int rth = rt.height();

            AVLTree<T> newNode = new NonEmpty<>(ord, value, lf, rt);

            if (lfh > rth && lfh - rth == 2) {
                if (ord.compare(lf.foldl(null, (a, v) -> v), other) > 0) {
                    return newNode.rotateLeft();
                } else {
                    return newNode.replaceLeft(AVLTree::rotateRight).rotateLeft();
                }
            } else if (lfh < rth && rth - lfh == 2) {
                if (ord.compare(rt.foldl(null, (a, v) -> v), other) < 0) {
                    return newNode.rotateRight();
                } else {
                    return newNode.replaceRight(AVLTree::rotateLeft).rotateRight();
                }
            } else {
                return newNode;
            }
        }

        @Override
        public AVLTree<T> replaceLeft(Function<AVLTree<T>, AVLTree<T>> left) {
            return new NonEmpty<>(ord, value, left.apply(this.left), right);
        }

        @Override
        public AVLTree<T> replaceRight(Function<AVLTree<T>, AVLTree<T>> right) {
            return new NonEmpty<>(ord, value, left, right.apply(this.right));
        }

        @Override
        public int height() {
            return 1 + Math.max(left.height(), right.height());
        }
        
        @Override
        public String toString() {
            return String.format(
                "{ Label : %s, left = %s, right = %s }",
                value, left, right);
        }

        @Override
        public AVLTree<T> rotateLeft() {
            return left.replaceRight(lfrt -> 
                new NonEmpty<>(ord, value, lfrt, right));
        }

        @Override
        public AVLTree<T> rotateRight() {
            return right.replaceLeft(rtlf -> 
                new NonEmpty<>(ord, value, left, rtlf));
        }

        @Override
        public boolean contains(T value) {
            int cmp = ord.compare(this.value, value);
            if (cmp == 0) return true;
            if (cmp > 0) return left.contains(value);
            return right.contains(value);
        }
        
        @Override
        public boolean equals(final Object other) {
            if (other == null) return false;
            if (other == this) return true;
            if (other instanceof Set) {
                Set<?> o = (Set<?>) other;
                if (this.length() != o.length()) return false;
                return this.toString().equals(o.toString());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }
    }
    
    final class Empty<T> implements AVLTree<T> {
        private final Ord<T> ord;

        Empty(Ord<T> ord) {
            this.ord = ord;
        }

        @Override
        public Ord<T> order() {
            return ord;
        }

        @Override
        public <R> Collection<R> empty() {
            return List.nil();
        }

        @Override
        public <R> R foldl(R seed, BiFunction<R, T, R> fn) {
            return seed;
        }

        @Override
        public AVLTree<T> build(T value) {
            return new NonEmpty<>(ord, value, this, this);
        }
        
        @Override
        public AVLTree<T> replaceLeft(Function<AVLTree<T>, AVLTree<T>> left) {
            return left.apply(this);
        }

        @Override
        public AVLTree<T> replaceRight(Function<AVLTree<T>, AVLTree<T>> right) {
            return right.apply(this);
        }

        @Override
        public int height() {
            return 0;
        } 
        
        @Override
        public String toString() {
            return "[]";
        }

        @Override
        public AVLTree<T> rotateLeft() {
            return this;
        }

        @Override
        public AVLTree<T> rotateRight() {
            return this;
        }

        @Override
        public boolean contains(T value) {
            return false;
        }
    }
}
