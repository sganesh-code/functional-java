package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.typeclass.Hashable;
import io.github.senthilganeshs.fj.typeclass.Monoid;
import java.util.Iterator;
import java.util.function.BiFunction;

/**
 * An immutable, purely functional HyperLogLog implementation for cardinality estimation.
 * Uses a fixed number of registers (m = 2^p) for a fixed memory footprint.
 */
public final class HyperLogLog<T> implements Collection<T> {
    private static final long serialVersionUID = 1L;

    private final int p; // Precision (number of bits for register index)
    private final int m; // Number of registers (2^p)
    private final List<Integer> registers;
    private final Hashable<T> hashable;

    private HyperLogLog(int p, List<Integer> registers, Hashable<T> hashable) {
        this.p = p;
        this.m = 1 << p;
        this.registers = registers;
        this.hashable = hashable;
    }

    public static <T> HyperLogLog<T> empty(int p, Hashable<T> hashable) {
        int m = 1 << p;
        List<Integer> initialRegisters = List.nil();
        for (int i = 0; i < m; i++) {
            initialRegisters = List.cons(initialRegisters, 0);
        }
        return new HyperLogLog<>(p, initialRegisters, hashable);
    }

    public static <T> HyperLogLog<T> create(int p, List<Integer> registers, Hashable<T> hashable) {
        return new HyperLogLog<>(p, registers, hashable);
    }

    /**
     * Monoid for HyperLogLog merging.
     */
    public static <T> Monoid<HyperLogLog<T>> monoid(int p, Hashable<T> hashable) {
        return Monoid.of(empty(p, hashable), (a, b) -> {
            List<Integer> merged = a.registers.zip(b.registers).map(t -> 
                Math.max(t.getA().orElse(0), t.getB().orElse(0)));
            return new HyperLogLog<>(p, merged, hashable);
        });
    }

    @Override
    public Collection<T> build(T value) {
        String hashStr = hashable.hash(value);
        long hash = hash64(hashStr);
        
        // Use lower p bits for index
        int j = (int) (hash & (m - 1));
        // Use remaining bits for rho
        long w = hash >>> p;
        int rho = (w == 0) ? (64 - p + 1) : Long.numberOfTrailingZeros(w) + 1;

        int currentValue = registers.atIndex(j).orElse(0);
        if (rho > currentValue) {
            return new HyperLogLog<>(p, List.from(registers.updateAtIndex(j, rho)), hashable);
        }
        return this;
    }

    private long hash64(String s) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++) {
            h ^= (long) s.charAt(i);
            h *= 1099511628211L; // FNV prime
        }
        return h;
    }

    @Override
    public int length() {
        return (int) estimateCardinality();
    }

    public long estimateCardinality() {
        double sum = registers.foldl(0.0, (acc, r) -> acc + Math.pow(2.0, -r));
        double alpha;
        if (m == 16) alpha = 0.673;
        else if (m == 32) alpha = 0.697;
        else if (m == 64) alpha = 0.709;
        else alpha = 0.7213 / (1.0 + 1.079 / m);

        double estimate = alpha * m * m / sum;

        // Small range correction
        if (estimate <= 2.5 * m) {
            int v = registers.filter(r -> r == 0).length();
            if (v > 0) {
                estimate = m * Math.log((double) m / v);
            }
        }
        return Math.round(estimate);
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
    public Iterator<T> iterator() {
        return java.util.Collections.emptyIterator();
    }

    public List<Integer> registers() { return registers; }
    public int p() { return p; }
    public int m() { return m; }
    public Hashable<T> hashable() { return hashable; }
}
