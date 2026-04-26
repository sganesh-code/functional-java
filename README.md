![build](https://github.com/sganesh-code/functional-java/actions/workflows/ci.yml/badge.svg?branch=master)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.sganesh-code/functional-java.svg)](https://central.sonatype.com/artifact/io.github.sganesh-code/functional-java)

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
*   **Algebraic Typeclasses**: Extensible `Eq`, `Ord`, `Semigroup`, and `Monoid` implementations for type-safe comparisons and reductions.
*   **Optics Engine**: Advanced Lenses, Prisms, and Traversals for deep immutable updates.
*   **Parser Combinators**: Build robust text parsers using monadic composition.
*   **Functional Serialization**: Purely functional binary encoding/decoding framework.
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
*   **`Validation`**: Success or error accumulation (Parallel Validation).

### 🌳 Structural
*   **`RoseTree`**: Multi-way (N-ary) tree structure.
*   **`Graph`**: Purely functional Directed Graph with BFS, DFS, and Topological Sort.

### 🏗 Parsing & Serialization
*   **`Parser`**: Monadic state-based parser combinator.
*   **`JsonValue`**: Purely functional JSON AST with built-in optics.
*   **`Encoder` / `Decoder`**: Purely functional binary serialization framework.

---

## API Showcase

### 1. Monadic Parser Combinators
Build complex grammars by composing simple parsers.

```java
// A simple CSV parser: letters separated by commas
Parser<List<Character>> csv = Parser.letter().sepBy(Parser.character(','));
List<Character> result = csv.parse("a,b,c").orElse(List.nil()); // ['a', 'b', 'c']
```

### 2. Streamlined JSON Navigation & Optics
Navigate and update deeply nested JSON structures with zero boilerplate.

```java
// Deep update: { "user": { "profile": { "name": "Alice" } } }
// Focus on user.profile.name and update to "Bob"
JsonValue updated = JsonValue.path("user", "profile")
    .compose(JsonValue.stringAt("name"))
    .set("Bob", rootJson);
```

### 3. Functional Binary Serialization
Encode and decode data structures to binary format fluently.

```java
// Encode a List of Integers to a DataOutput
Encoder<List<Integer>> enc = Codec.listEncoder(Codec.intEncoder());
enc.encode(dataOutput, List.of(1, 2, 3));

// Decode it back from DataInput
Decoder<List<Integer>> dec = Codec.listDecoder(Codec.intDecoder());
List<Integer> list = dec.decode(dataInput).orElse(List.nil());
```

### 4. Robust Safety & Validation
Eliminate nested `if-present` or `null` checks with monadic pipelines and applicatives.

*   **Atomic Batch Retrieval (`traverse`)**: Turn a list of IDs into a list of profiles, but only if **every** ID exists.
    ```java
    // turns List<String> -> Maybe<List<Profile>>
    Maybe<List<Profile>> result = userIds.traverse(db::findMaybe);
    ```
*   **Validation Pipelines (`Either`)**: Chain operations that can fail without exceptions.
    ```java
    Either<String, Order> order = Either.right(cart)
        .flatMapEither(this::checkStock)
        .flatMapEither(this::applyDiscount);
    ```

### 5. Optics: Lenses, Prisms, and Traversals
Effortlessly update deeply nested immutable structures.

*   **Lens**: Focus on a single mandatory field.
*   **Prism**: Focus on an optional case (Sum types).
*   **Traversal**: Focus on all elements in a collection at once.
    ```java
    // Update every value in a HashMap at once using optics
    Traversal<HashMap<String, Integer>, Integer> eachVal = Traversal.fromCollection();
    HashMap<String, Integer> doubled = eachVal.modify(myMap, i -> i * 2);
    ```

### 6. Path Validation
Explicitly verify if a hierarchical path exists before operating.

```java
// Returns Validation.valid(node) or Validation.invalid("Key 'name' not found at step 2")
Validation<String, JsonValue> result = json.validatePath("users", 0, "name");
```

---

## Installation (Version 1.2.4)

### Maven
```xml
<dependency>
    <groupId>io.github.sganesh-code</groupId>
    <artifactId>functional-java</artifactId>
    <version>1.2.4</version>
</dependency>
```

#### Gradle
```gradle
implementation 'io.github.sganesh-code:functional-java:1.2.4'
```

---

## Performance

The library is designed for the JVM. Core recursive bottlenecks have been replaced with optimized iterative loops.

**Benchmark Highlights (1,000 Elements):**
*   **List Folding**: ~4.5μs
*   **Vector Access**: ~0.003μs
*   **Map Lookup**: ~0.004μs

*Full details available in [BENCHMARK.md](./BENCHMARK.md).*

---

## License

Distributed under the **GPL-v3.0** License. See `LICENSE` for more information.
