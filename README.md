![build](https://github.com/sganesh-code/functional-java/actions/workflows/ci.yml/badge.svg?branch=master)

# functional-java

**Purely functional, immutable data structures for the modern Java developer.**

`functional-java` is an initiative to bring expressive, Haskell-inspired functional APIs to Java without compromising on object-oriented principles. Built for robustness and clarity, it provides a unified set of tools to transform complex data manipulations into concise, declarative pipelines.

---

## 📖 Documentation
**[Explore the Full API Reference (GitHub Pages)](https://sganesh-code.github.io/functional-java/)**

---

## 🚀 Key Features

*   **100% Immutable**: All structures are persistent; updates return new versions while sharing structure.
*   **Unified API**: 40+ functional methods (map, filter, traverse, partition, etc.) available on every data structure via a single interface.
*   **Performance Optimized**: Core engines rewritten from recursion to iterative loops, achieving up to **400x speedup** for large datasets.
*   **Zero Dependencies**: Lean and lightweight library with no external requirements.

---

## 📋 Table of Contents
1. [Core Triad Design](#core-triad-design)
2. [Supported Data Structures](#supported-data-structures)
3. [API Showcase](#api-showcase)
4. [Installation](#installation)
5. [Performance](#performance)
6. [License](#license)

---

## Core Triad Design

The library is built on a powerful abstraction: the `Collection<T>` interface. To implement any complex functional behavior, a data structure only needs to implement three core methods:

1.  **`empty()`**: Returns an empty instance.
2.  **`build(T)`**: Persistent addition of an element.
3.  **`foldl(seed, fn)`**: The fundamental reduction engine.

By implementing these three, every data structure automatically inherits the full functional suite: `map`, `flatMap`, `traverse`, `zipWith`, `span`, `chunk`, `groupBy`, and many more.

---

## Supported Data Structures

### 📦 Sequential
*   **`List`**: Purely functional linked list (Snoc-list style).
*   **`Vector`**: Bitmapped Vector Trie for near O(1) random access and updates.
*   **`Array`**: Functional wrapper for Java arrays.
*   **`LazyList`**: Deferred evaluation for finite or infinite sequences.

### 🔍 Associative & Sets
*   **`Set`**: Self-balancing AVL Tree implementation.
*   **`HashMap`**: High-performance Hash Array Mapped Trie (HAMT) with collision handling.
*   **`Map`**: Binary tree-based associative mapping.

### 🎭 Functional Primitives
*   **`Maybe`**: Safe optional values with monadic API.
*   **`Either`**: Disjoint union type for expressive error handling.
*   **`Tuple`**: Simple product types for grouping values.

### 🌳 Structural
*   **`RoseTree`**: Multi-way (N-ary) tree structure.
*   **`Graph`**: Purely functional Directed Graph with BFS, DFS, and Topological Sort.
*   **`Stack` / `Queue` / `Deque`**: Functional linear structures.

---

## API Showcase

### 1. Atomic Retrieval (`traverse`)
Turn a list of IDs into a list of profiles, but only if **every** ID exists.
```java
// turns List<String> -> Maybe<List<Profile>>
Maybe<List<Profile>> result = userIds.traverse(db::findMaybe);
```

### 2. Validation Pipelines (`Either`)
Chain operations that can fail without manual null checks or exceptions.
```java
Either<String, Order> order = Either.right(cart)
    .flatMap(this::checkStock)
    .flatMap(this::applyDiscount)
    .flatMap(this::calculateTax);
```

### 3. Data Segmentation (`span` & `groupBy`)
```java
// Split a list at the first non-positive number
Tuple<Collection<Integer>, Collection<Integer>> s = list.span(i -> i > 0);

// Group users by their primary interest
HashMap<String, Collection<User>> segments = users.groupBy(User::getInterest);
```

---

## Installation (Version 1.0.1)

### Maven
```xml
<dependency>
    <groupId>io.github.sganesh-code</groupId>
    <artifactId>functional-java</artifactId>
    <version>1.0.1</version>
</dependency>
```

### Gradle
```gradle
implementation 'io.github.sganesh-code:functional-java:1.0.1'
```

---

## Performance

The library is designed for the JVM. All recursive bottlenecks in the core traversal engine have been replaced with optimized iterative loops.

**Benchmark Highlights (1,000 Elements):**
*   **List Folding**: ~4.5μs (Competitive with Java Streams).
*   **Random Access**: ~0.003μs (Near constant time).
*   **Map Lookup**: ~0.004μs (Optimized HAMT traversal).

*Full details available in [BENCHMARK.md](./BENCHMARK.md).*

---

## License

Distributed under the **GPL-v3.0** License. See `LICENSE` for more information.
