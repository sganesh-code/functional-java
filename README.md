![build](https://github.com/sganesh-code/functional-java/actions/workflows/ci.yml/badge.svg?branch=master)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.sganesh-code/functional-java.svg)](https://central.sonatype.com/artifact/io.github.sganesh-code/functional-java)

# functional-java

**Purely functional, immutable data structures for the modern Java developer.**

`functional-java` is an initiative to bring expressive, Haskell-inspired functional APIs to Java without compromising on object-oriented principles. Built for robustness and clarity, it provides a unified set of tools to transform complex data manipulations into concise, declarative pipelines.

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
2. [Zero-Cost Interop](#zero-cost-interop)
3. [Supported Data Structures](#supported-data-structures)
4. [API Showcase](#api-showcase)
5. [Installation](#installation)
6. [Performance](#performance)
7. [License](#license)

---

## Core Triad Design

The library is built on a powerful abstraction: the `Collection<T>` interface. To implement any complex functional behavior, a data structure only needs to implement three core methods:

1.  **`empty()`**: Returns an empty instance.
2.  **`build(T)`**: Persistent addition of an element.
3.  **`foldl(seed, fn)`**: The fundamental reduction engine.

By implementing these three, every data structure automatically inherits the full functional suite: `map`, `flatMap`, `traverse`, `zipWith`, `span`, `chunk`, `groupBy`, and many more.

---

## Zero-Cost Interop

Interoperability is a first-class citizen. When you create a custom data structure by implementing the **Core Triad**, it instantly gains the ability to interoperate with every other data structure in the library.

### 1. Implement your Triad (e.g., a Sliding Window)
A `SlidingWindow` is a collection that only keeps the last `N` elements.

```java
public class SlidingWindow<T> implements Collection<T> {
    private final List<T> items; 
    private final int maxSize;

    @Override public <R> Collection<R> empty() { return new SlidingWindow<>(maxSize, List.nil()); }

    @Override public Collection<T> build(T val) {
        List<T> next = (items.length() >= maxSize) ? items.drop(1).build(val) : items.build(val);
        return new SlidingWindow<>(maxSize, next);
    }

    @Override public <R> R foldl(R seed, BiFunction<R, T, R> fn) { return items.foldl(seed, fn); }
}
```

### 2. Interop "For Free"
Convert, combine, and query across types seamlessly:

```java
SlidingWindow<Double> window = new SlidingWindow<>(3).build(10.0).build(20.0).build(30.0).build(40.0);

// Interop: Convert to persistent Vector for O(1) access
Vector<Double> vector = Vector.from(window);

// Interop: Perform atomic batch operations
Maybe<List<Price>> prices = window.traverse(id -> priceDB.find(id));

// Interop: Categorize window data into a HashMap
HashMap<Boolean, Collection<Double>> segments = window.groupBy(val -> val > 25.0);
```

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

### A. Robust Safety & Validation

*   **Atomic Batch Retrieval (`traverse`)**: Turn a list of IDs into a list of profiles, but only if **every** ID exists.
    ```java
    Maybe<List<Profile>> result = userIds.traverse(db::findMaybe);
    ```
*   **Validation Pipelines (`Either`)**: Chain operations that can fail without exceptions.
    ```java
    Either<String, Order> order = Either.right(cart)
        .flatMapEither(this::checkStock)
        .flatMapEither(this::applyDiscount);
    ```
*   **Safe Combinations (`liftA2`)**: Combine two monadic values using a function.
    ```java
    Maybe<Integer> sum = m1.liftA2(Integer::sum, m2);
    ```
*   **Parallel Validation (`Validation`)**: Unlike `Either`, `Validation` accumulates **every** error using a `Semigroup`.
    ```java
    Validation<String, User> result = v1.liftA2(User::new, v2, Monoid.STRING_CONCAT);
    ```

### B. Data Analytics, Cleaning, & Batching

*   **Categorization (`groupBy`)**: Group elements into a `HashMap` by a key.
    ```java
    HashMap<String, Collection<User>> segments = users.groupBy(User::getInterest);
    ```
*   **Algebraic Summary (`foldMap`)**: Map elements to a `Monoid` and aggregate in one pass.
    ```java
    Double total = orders.foldMap(Order::getAmount, Monoid.DOUBLE_SUM);
    ```
*   **Prefix Splitting (`span`)**: Split a collection at the first element that fails a condition.
    ```java
    Tuple<Collection<Task>, Collection<Task>> t = tasks.span(task -> task.isHighPriority());
    ```
*   **Fixed-Size Batching (`chunk`)**: Break data into batches for processing.
    ```java
    Collection<Collection<Item>> batches = items.chunk(100);
    ```
*   **Dynamic Data Generation (`unfold`)**: Build a collection iteratively from a seed.
    ```java
    Collection<Integer> countdown = Collection.unfold(10, i -> i > 0 ? Maybe.some(Tuple.of(i, i - 1)) : Maybe.nothing());
    ```

### C. Advanced Optics & Immutable Updates

*   **Zero-Boilerplate Lenses (`RecordOptics`)**: Automatically generate Lenses for any Java Record.
    ```java
    Lens<User, String> nameL = RecordOptics.of(User.class, User::name);
    User updated = nameL.set("Bob", user);
    ```
*   **Sealed Interface Prisms (`SealedOptics`)**: Automatically generate Prisms for Java 17+ Sealed Hierarchies.
    ```java
    Prism<Result, Success> successP = SealedOptics.prism(Result.class, Success.class);
    ```
*   **Optics with Defaults (`AffineTraversal`)**: Handle nested optionals and provide defaults in the optic.
    ```java
    Lens<User, String> cityL = userAddressP.compose(cityLens).withDefault("UNKNOWN");
    ```
*   **Indexed Optics (`at`)**: Target specific elements in a collection by position.
    ```java
    Collection<User> updated = Collection.at(2).compose(nameLens).set("Bob", userList);
    ```
*   **Isomorphisms (`Iso`)**: Lossless, two-way transformations between types (e.g., Record <-> Tuple).
    ```java
    Iso<Point, Tuple<Integer, Integer>> pointIso = Iso.of(p -> Tuple.of(p.x(), p.y()), t -> ...);
    ```

### D. Parsing, JSON, & Persistence

*   **Monadic Parser Combinators**: Build complex grammars by composing simple parsers.
    ```java
    Parser<List<Character>> csv = Parser.letter().sepBy(Parser.character(','));
    List<Character> result = csv.parse("a,b,c").orElse(List.nil());
    ```
*   **Streamlined JSON Navigation**: Deeply nested navigation and updates with zero boilerplate.
    ```java
    JsonValue updated = JsonValue.path("user", "profile", "address")
        .compose(JsonValue.stringAt("city"))
        .set("Paris", rootJson);
    ```
*   **Functional Path Validation**: Explicitly verify a hierarchical path before operating.
    ```java
    Validation<String, JsonValue> result = json.validatePath("users", 0, "name");
    ```
*   **Functional Binary Serialization**: High-performance binary encoding fluently composed.
    ```java
    Encoder<List<Integer>> enc = Codec.listEncoder(Codec.intEncoder());
    enc.encode(dataOutput, List.of(1, 2, 3));
    ```

### E. Automated JSON Isomorphisms

Bridge the gap between static Java Records and dynamic JSON with zero boilerplate.

*   **Bidirectional Mapping**: Automatically convert arbitrary records to/from `JsonValue`.
    ```java
    // 1. Define your domain records
    record User(String name, List<String> roles) {}
    
    // 2. Convert to JSON
    JsonValue json = JsonValue.fromRecord(new User("Alice", List.of("ADMIN")));
    
    // 3. Convert back to Record
    User user = json.toRecord(User.class);
    ```

### F. Deferred Execution (Lazy)

*   **Lazy Generators (`Generator`)**: Infinite sequences with zero memory overhead.
    ```java
    LazyList<Integer> naturalNumbers = Generator.iterate(1, i -> i + 1);
    ```
*   **Memoization (`Lazy`)**: Ensure expensive computations run exactly once.
    ```java
    Lazy<String> data = Lazy.of(() -> fetchFromRemote());
    ```

---

## Installation (Version 1.2.5)

### Maven
```xml
<dependency>
    <groupId>io.github.sganesh-code</groupId>
    <artifactId>functional-java</artifactId>
    <version>1.2.5</version>
</dependency>
```

#### Gradle
```gradle
implementation 'io.github.sganesh-code:functional-java:1.2.5'
```

---

## Performance

The library core iterative engine achieves up to **400x speedup** over traditional recursive implementations for large datasets.

**Benchmark Highlights (1,000 Elements):**
*   **List Folding**: ~4.5μs
*   **Vector Access**: ~0.003μs
*   **Map Lookup**: ~0.004μs

*Full details available in [BENCHMARK.md](./BENCHMARK.md).*

---

## License

Distributed under the **GPL-v3.0** License. See `LICENSE` for more information.
