package io.github.senthilganeshs.fj.test;

import io.github.senthilganeshs.fj.ds.List;
import java.util.function.Function;

/**
 * A typeclass for types that can be shrunk to "simpler" values.
 * Used in property-based testing to find minimal counter-examples.
 * 
 * @param <A> The type to shrink.
 */
public interface Shrink<A> extends Function<A, List<A>> {

    static Shrink<Integer> integer() {
        return n -> {
            if (n == 0) return List.nil();
            List<Integer> res = List.of(0);
            
            // Binary search style shrinking
            int temp = n;
            while (Math.abs(temp) > 0) {
                temp = temp / 2;
                final int finalTemp = temp;
                if (finalTemp != 0 && !res.find(i -> i == finalTemp).isSome()) {
                    res = (List<Integer>) res.build(finalTemp);
                }
            }
            
            // Also try values close to n
            if (n > 0) {
                res = (List<Integer>) res.build(n - 1);
            } else {
                res = (List<Integer>) res.build(n + 1);
            }
            
            return (List<Integer>) res.reverse().filter(i -> i != n);
        };
    }

    static Shrink<String> string() {
        return s -> {
            if (s.isEmpty()) return List.nil();
            return List.of(""); // The simplest string
        };
    }

    static <A> Shrink<List<A>> list(Shrink<A> elementShrink) {
        return list -> {
            if (list.isEmpty()) return List.nil();
            return List.of(List.<A>nil());
        };
    }
}
