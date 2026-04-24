package io.github.senthilganeshs.fj.ds;

import java.util.Arrays;
import java.util.function.BiFunction;

public interface List<T> extends Collection<T> {
    
    @Override List<T> build(final T input);

    Tuple<Maybe<T>, List<T>> unzip();


    <R> List<Tuple<T, R>> zip(List<R> other);
    
    static <R> List<R> of (final java.util.List<R> list) {
        return list.stream().reduce(nil(), (rs, r) ->rs.build(r), (r1, r2) -> r2);
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
            return fn.apply(head.foldl(seed, (r, t) -> fn.apply(r, t)), tail);
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
             
             Maybe<T> h = thisUnzipped.getA().fromMaybe(Maybe.nothing());
             Maybe<R> oh = otherUnzipped.getA().fromMaybe(Maybe.nothing());
             
             if (h.isNothing() || oh.isNothing()) return List.nil();
             
             List<Tuple<T, R>> rest = thisUnzipped.getB().fromMaybe(List.nil()).zip(otherUnzipped.getB().fromMaybe(List.nil()));
             return (List<Tuple<T, R>>) List.of(Tuple.of(h.fromMaybe(null), oh.fromMaybe(null))).concat(rest);
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