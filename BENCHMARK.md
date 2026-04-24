# Functional Java Performance Benchmark Report

This report compares the purely functional, immutable data structures in `functional-java` against the standard mutable and immutable structures provided by the Java Standard Library.

## Methodology

*   **Environment**: JDK 21 (Zulu), JMH 1.20
*   **Hardware**: Darwin (macOS)
*   **Settings**: 1 Fork, 2 Warmup Iterations, 3 Measurement Iterations (1s each).
*   **Data Set**: 1,000 random integers (unless otherwise specified).

## Summary Table (1,000 Elements)

| Operation | FJ Implementation | Score (μs/op) | Java Implementation | Score (μs/op) | Performance Gap |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Construction (Sequential)** | `fj_buildList` | 1.33 | `java_buildArrayList` | 2.01 | **FJ is 1.5x Faster** |
| **Construction (Associative)**| `fj_buildHashMap` | 24.19 | `java_buildHashMap` | 5.59 | FJ is 4.3x Slower |
| **Construction (Vector)** | `fj_buildVector` | 8.01 | - | - | - |
| **Random Access** | `fj_vectorAccess` | 0.003 | `java_arrayListAccess` | 0.002 | Comparable |
| **Key Lookup** | `fj_hashMapLookup` | 0.004 | `java_hashMapLookup` | 0.003 | Comparable |
| **Set Search** | `fj_setContains` | 0.011 | `java_treeSetContains` | 0.006 | FJ is 1.8x Slower |
| **Folding/Reduction** | `fj_foldlList` | 1861.92 | `java_streamReduce` | 1.50 | FJ is ~1200x Slower |
| **Queue Operations** | `fj_queueMixedOps`| 3181.57 | `java_arrayDequeOps` | 3.32 | FJ is ~950x Slower |

## Deep Dive Analysis

### 1. The Win: Fast Persistence and Lookups
Our `Vector` (Bitmapped Vector Trie) and `HashMap` (HAMT) show exceptional lookup performance. At `0.003 - 0.004 μs`, they are practically as fast as Java's standard `ArrayList.get()` and `HashMap.get()`. This makes them ideal for read-heavy functional workloads where immutability is required without sacrificing access speed.

### 2. The Trade-off: Transformation Overhead
The largest performance gap is in sequential processing (`foldl` vs Java `Stream.reduce`). 
*   **Java**: Highly optimized for loops, effectively hitting CPU caches with primitive-like efficiency.
*   **FJ**: Relies on recursive method calls and high-level functional abstractions, leading to significantly higher instruction counts and stack overhead.

### 3. Queue Performance
The `BankersQueue` mixed operations benchmark is expensive because it forces internal stack reversals to maintain amortized O(1) performance. While O(1) amortized is mathematically sound, the constant factors in Java (object creation, stack reversal) are high compared to `ArrayDeque`'s circular buffer.

## Conclusion

`functional-java` structures are optimized for **structural sharing and persistence**. They excel in scenarios where you need to maintain "previous versions" of a collection efficiently. For high-performance, single-threaded batch processing, Java standard collections remain superior due to lower constant factors and better cache locality.

---
*Report generated on April 24, 2026.*
