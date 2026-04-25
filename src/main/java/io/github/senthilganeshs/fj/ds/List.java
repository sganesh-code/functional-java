package io.github.senthilganeshs.fj.ds;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public interface List<T> extends Collection<T> {
    
    @Override List<T> build(final T input);

    Tuple<Maybe<T>, List<T>> unzip();

    <R> List<Tuple<T, R>> zip(List<R> other);

    @Override
    default <R> List<R> map(Function<T, R> fn) {
        return from(Collection.super.map(fn));
    }

    @Override
    default <R> Collection<R> flatMap(Function<T, Collection<R>> fn) {
        return Collection.super.flatMap(fn);
    }

    @Override
    default List<T> filter(Predicate<T> pred) {
        return from(Collection.super.filter(pred));
    }

    @Override
    default List<T> take(int n) {
        return from(Collection.super.take(n));
    }

    @Override
    default List<T> drop(int n) {
        return from(Collection.super.drop(n));
    }

    @Override
    default List<T> reverse() {
        return from(Collection.super.reverse());
    }

    @Override
    default <R> List<R> mapMaybe(Function<T, Maybe<R>> fn) {
        return from(Collection.super.mapMaybe(fn));
    }

    @Override
    default List<T> takeWhile(Predicate<T> pred) {
        return from(Collection.super.takeWhile(pred));
    }

    @Override
    default List<T> dropWhile(Predicate<T> pred) {
        return from(Collection.super.dropWhile(pred));
    }
    
    static <R> List<R> of (final java.util.List<R> list) {
        return list.stream().reduce(nil(), (rs, r) ->rs.build(r), (r1, r2) -> r2);
    }

    public static <R> List<R> from (final Collection<R> collection) {
        return collection.foldl(nil(), (list, r) -> (List<R>) list.build(r));
    }

    static <R> Collection<R> emptyQueue() {
        return new EmptyList<>();
    }

    static <R> Collection<R> newQueue(R[] values) {
        return Arrays.stream(values).reduce(emptyQueue(), (queue, r) -> queue.build(r), (a, b) -> b);
    }
    
    @SafeVarargs
    public static <R> List<R> of (final R...values) {
        if (values == null || values.length == 0)
            return nil();
        List<R> list = nil();
        for (R value : values) {
            list = list.build(value);
        }
        return list;
    }
    
    static final List<Void> EMPTY = new EmptyList<Void>();
    
    @SuppressWarnings("unchecked")
    public static<R> List<R> nil() {
        return (List<R>) EMPTY;
    }
    
    public static <R> List<R> cons(final List<R> head, final R tail) {
        return new LinkedList<>(head, tail);
    }
    
    final static class EmptyList<T> implements List<T> {

        @Override
        public <R> Collection<R> empty() {
            return nil();
        }

        @Override
        public <R> R foldl(R seed, BiFunction<R, T, R> fn) {
            return seed;
        }

        @Override
        public List<T> build(T input) {
            return new LinkedList<>(this, input);
        }

        @Override
        public Tuple<Maybe<T>, List<T>> unzip() {
            return Tuple.of();
        }


        @Override
        public <R> List<Tuple<T, R>> zip(List<R> other) {
            return List.of();
        }

        @Override
        public String toString() {
            return "";
        }
        
        @Override
        public int hashCode() {
            return 0;
        }
        
        @Override
        public boolean equals(final Object other) {
            if (other == null)
                return false;
            if (other == this)
                return true;
            if (other instanceof EmptyList)
                return true;
            return false;
        }
        
    }
    
    final static class LinkedList<T> implements List<T> {
        private final T tail;
        private final List<T> head;
        
        LinkedList (final List<T> head, final T tail) {
            this.head = head;
            this.tail = tail;
        }
        
        @Override
        public <R> Collection<R> empty() {
            return nil();
        }
        @Override
        public <R> R foldl(R seed, BiFunction<R, T, R> fn) {
            java.util.Deque<T> stack = new java.util.ArrayDeque<>();
            List<T> curr = this;
            while (curr instanceof LinkedList) {
                LinkedList<T> ll = (LinkedList<T>) curr;
                stack.push(ll.tail);
                curr = ll.head;
            }
            R acc = seed;
            while (!stack.isEmpty()) {
                acc = fn.apply(acc, stack.pop());
            }
            return acc;
        }
        @Override
        public List<T> build(T input) {
            return cons(this, input);
        }

        @Override
        public Tuple<Maybe<T>, List<T>> unzip() {
            Maybe<T> first = head.find(i -> true);
            if (first.isNothing()) {
                return Tuple.of(Maybe.some(tail), List.nil());
            }
            return Tuple.of(
                    first,
                    (List<T>) head.drop(1).build(tail));
        }

        @Override
        public <R> List<Tuple<T, R>> zip(List<R> other) {
             Tuple<Maybe<T>, List<T>> thisUnzipped = this.unzip();
             Tuple<Maybe<R>, List<R>> otherUnzipped = other.unzip();
             
             Maybe<T> h = thisUnzipped.getA().orElse(Maybe.nothing());
             Maybe<R> oh = otherUnzipped.getA().orElse(Maybe.nothing());
             
             if (h.isNothing() || oh.isNothing()) return List.nil();
             
             List<Tuple<T, R>> rest = thisUnzipped.getB().orElse(List.nil()).zip(otherUnzipped.getB().orElse(List.nil()));
             return (List<Tuple<T, R>>) List.of(Tuple.of(h.orElse(null), oh.orElse(null))).concat(rest);
        }

        @Override
        public String toString() {
            return head.foldl("[", (r, t) -> r + t + ",") + tail + "]";
        }
        
        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public boolean equals(final Object other) {
            if (other == null) return false;
            if (other == this) return true;
            
            if (other instanceof LinkedList) {
                LinkedList<T> llOther = (LinkedList<T>) other;
                if (llOther.tail.equals(((LinkedList) other).tail)) {
                    return llOther.head.equals(head);
                }
            }
            return false;
        }
    }
}