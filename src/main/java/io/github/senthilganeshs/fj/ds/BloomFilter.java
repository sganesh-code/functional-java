package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.typeclass.Hashable;
import io.github.senthilganeshs.fj.typeclass.Monoid;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.function.BiFunction;

/**
 * An immutable, purely functional Bloom Filter.
 * Uses BigInteger for a persistent bitset and Hashable for stable hashing.
 */
public final class BloomFilter<T> implements Collection<T> {
    private static final long serialVersionUID = 1L;

    private final int m; // Size of bitset
    private final int k; // Number of hash functions
    private final BigInteger bits;
    private final Hashable<T> hashable;

    private BloomFilter(int m, int k, BigInteger bits, Hashable<T> hashable) {
        this.m = m;
        this.k = k;
        this.bits = bits;
        this.hashable = hashable;
    }

    public static <T> BloomFilter<T> empty(int m, int k, Hashable<T> hashable) {
        return new BloomFilter<>(m, k, BigInteger.ZERO, hashable);
    }

    public static <T> BloomFilter<T> create(int m, int k, BigInteger bits, Hashable<T> hashable) {
        return new BloomFilter<>(m, k, bits, hashable);
    }

    /**
     * Standard Monoid for BloomFilters of the same configuration.
     */
    public static <T> Monoid<BloomFilter<T>> monoid(int m, int k, Hashable<T> hashable) {
        return Monoid.of(empty(m, k, hashable), (a, b) -> 
            new BloomFilter<>(m, k, a.bits.or(b.bits), hashable));
    }

    @Override
    public Collection<T> build(T value) {
        BigInteger nextBits = bits;
        String baseHash = hashable.hash(value);
        int h1 = baseHash.hashCode();
        int h2 = (baseHash + "alt").hashCode();

        for (int i = 0; i < k; i++) {
            int combinedHash = h1 + (i * h2);
            int bitIdx = Math.abs(combinedHash % m);
            nextBits = nextBits.setBit(bitIdx);
        }
        return new BloomFilter<>(m, k, nextBits, hashable);
    }

    @Override
    public boolean contains(T value) {
        String baseHash = hashable.hash(value);
        int h1 = baseHash.hashCode();
        int h2 = (baseHash + "alt").hashCode();

        for (int i = 0; i < k; i++) {
            int combinedHash = h1 + (i * h2);
            int bitIdx = Math.abs(combinedHash % m);
            if (!bits.testBit(bitIdx)) return false;
        }
        return true;
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

    @Override
    public int length() {
        return 0;
    }

    public int m() { return m; }
    public int k() { return k; }
    public BigInteger bits() { return bits; }
    public Hashable<T> hashable() { return hashable; }
}
