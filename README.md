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

## 🛠 Custom Data Structures & Zero-Cost Interop

One of the most powerful traits of `functional-java` is that it makes interoperability a first-class citizen. When you create a custom data structure by implementing the **Core Triad**, it doesn't just gain functional methods—it instantly gains the ability to interoperate with every other data structure in the library without writing a single line of integration code.

### 1. Implement your Triad (e.g., a Sliding Window)
A `SlidingWindow` is a collection that only keeps the last `N` elements. By implementing the Triad, it becomes a first-class functional citizen.

```java
public class SlidingWindow<T> implements Collection<T> {
    private final List<T> items; // Using FJ List internally
    private final int maxSize;

    @Override public <R> Collection<R> empty() { return new SlidingWindow<>(maxSize, List.nil()); }

    // Domain Logic: If at capacity, drop the oldest element before adding new
    @Override public Collection<T> build(T val) {
        List<T> next = (items.length() >= maxSize) ? items.drop(1).build(val) : items.build(val);
        return new SlidingWindow<>(maxSize, next);
    }

    @Override public <R> R foldl(R seed, BiFunction<R, T, R> fn) {
        return items.foldl(seed, fn);
    }
}
```

### 2. Get Interop "For Free"
Because everything is built on the same abstraction, you can convert, combine, and query across types seamlessly:

```java
SlidingWindow<Double> window = new SlidingWindow<>(3); // Window of size 3
window = window.build(10.0).build(20.0).build(30.0).build(40.0); // Content: [20.0, 30.0, 40.0]

// Interop: Convert window to a persistent Vector for O(1) random access
Vector<Double> vector = Vector.from(window);

// Interop: Perform atomic batch operations (Window -> Maybe -> functional List)
Maybe<List<Price>> prices = window.traverse(id -> priceDB.find(id));

// Interop: Categorize window data into a HashMap
HashMap<Boolean, Collection<Double>> segments = window.groupBy(val -> val > 25.0);
```

This "Zero-Cost Interop" ensures that your custom domain-specific structures are never isolated; they are full citizens of the functional ecosystem.

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

### 1. Robust Safety & Validation
Eliminate nested `if-present` or `null` checks with monadic pipelines and applicatives.

*   **Atomic Batch Retrieval (`traverse`)**: Turn a list of IDs into a list of profiles, but only if **every** ID exists.
    ```java
    // turns List<String> -> Maybe<List<Profile>>
    Maybe<List<Profile>> result = userIds.traverse(db::findMaybe);
    ```
*   **Validation Pipelines (`Either`)**: Chain operations that can fail without exceptions.
    ```java
    Either<String, Order> order = Either.right(cart)
        .flatMap(this::checkStock)
        .flatMap(this::applyDiscount)
        .flatMap(this::calculateTax);
    ```
*   **Safe Combinations (`liftA2`)**: Combine two `Maybe` or `Either` values using a function.
    ```java
    Maybe<Integer> sum = m1.liftA2(Integer::sum, m2);
    ```

### 2. Powerful Transformations & Data Cleaning
Cleanse and reshape data fluently.

*   **Sparse Data Processing (`mapMaybe`)**: Filter and transform in a single pass.
    ```java
    // Get all valid email addresses from a list of user profiles
    Collection<Email> emails = profiles.mapMaybe(p -> p.getEmailMaybe());
    ```
*   **Type-Safe Extraction (`filterType`)**: Safely extract specific types from mixed collections.
    ```java
    // Get only the Admin objects from a List<User>
    Collection<Admin> admins = users.filterType(Admin.class);
    ```
*   **Deduplication (`distinct`)**: Keep only unique elements in any collection.
    ```java
    Collection<Integer> unique = list.distinct();
    ```

### 3. Data Analytics & Aggregation
Transform collections into structured insights.

*   **Categorization (`groupBy`)**: Group elements into a `HashMap` by a key.
    ```java
    // Group users by their primary interest
    HashMap<String, Collection<User>> segments = users.groupBy(User::getInterest);
    ```
*   **Algebraic Summary (`foldMap`)**: Map elements to a `Monoid` and aggregate in one pass.
    ```java
    // Sum all order totals using the Double Sum monoid
    Double total = orders.foldMap(Order::getAmount, Monoid.DOUBLE_SUM);
    ```
*   **State History (`scanl`)**: Track every intermediate state of a reduction (e.g., running balance).
    ```java
    // [100, -20, -10] -> [100, 80, 70]
    Collection<Integer> balanceHistory = transactions.scanl(initialBalance, Integer::sum);
    ```

### 4. Advanced Segmentation & Batching
Handle large datasets with structural precision.

*   **Prefix Splitting (`span`)**: Split a collection at the first element that fails a condition.
    ```java
    // Returns Tuple of (Prefix matches, Remainder)
    Tuple<Collection<Task>, Collection<Task>> t = tasks.span(task -> task.isHighPriority());
    ```
*   **Fixed-Size Batching (`chunk`)**: Break data into batches for processing.
    ```java
    // Process 10,000 items in batches of 100
    Collection<Collection<Item>> batches = items.chunk(100);
    ```

### 5. Dynamic Data Generation
Build complex structures from a simple seed.

*   **Sequence Generation (`unfold`)**: Build a collection iteratively until a condition is met.
    ```java
    // Generate a range [10, 9, ..., 1]
    Collection<Integer> countdown = Collection.unfold(10, i -> i > 0 ? Maybe.some(Tuple.of(i, i - 1)) : Maybe.nothing());
    ```

### 6. Algebraic Strategies (`Eq` & `Ord`)
Decouple ordering and equality logic from your data types.

*   **Custom Ordering**: Create a Set for types that don't implement `Comparable`.
    ```java
    // Case-insensitive string set
    Set<String> s = Set.empty(Ord.fromComparator(String.CASE_INSENSITIVE_ORDER));
    ```
*   **Domain-Specific Equality**: Define how elements are considered unique.
    ```java
    // Keep only unique users by their ID (uses distinct with custom Eq)
    Collection<User> uniqueUsers = users.distinct(Eq.fromEquals()); 
    ```

### 7. Optics: Lenses, Prisms, and Traversals
Effortlessly update deeply nested immutable structures without boilerplate.

*   **Lens**: Focus on a single mandatory field (Records).
    ```java
    Lens<Shipment, Hub> hubL = Lens.of(Shipment::origin, (h, s) -> new Shipment(s.id(), h));
    Lens<Hub, String> cityL = Lens.of(Hub::city, (c, h) -> new Hub(h.id(), c));
    
    // Compose them to update city 2 levels deep
    Shipment updated = hubL.compose(cityL).set("Paris", oldShipment);
    ```
*   **Prism**: Focus on an optional case (Sum types like `Either` or `Maybe`).
    ```java
    // Target the Right value of an Either response
    Prism<Either<Error, User>, User> userP = Prism.of(
        e -> e.isRight() ? Maybe.some(e.orElse(null)) : Maybe.nothing(),
        Either::right
    );
    ```
*   **Traversal**: Focus on all elements in a collection at once.
    ```java
    Traversal<List<Hub>, Hub> eachHub = Traversal.fromCollection();
    // Update every city in a list of hubs
    List<Hub> allReset = eachHub.compose(cityL).set("UNKNOWN", hubList);
    ```

### 8. Parallel Validation (`Validation`)
Unlike `Either`, which stops at the first error, `Validation` accumulates **every** error using a `Semigroup`.

```java
// Combine multiple validations; if both fail, errors are concatenated
Validation<String, User> result = v1.liftA2(User::new, v2, Monoid.STRING_CONCAT);
```

### 9. Lazy Generators (`Generator`)
Generate infinite or sequential sequences with zero memory overhead until evaluated.

```java
// Infinite sequence of 1, 2, 3, 4...
LazyList<Integer> naturalNumbers = Generator.iterate(1, i -> i + 1);

// Infinite repetition: [A, A, A, ...]
LazyList<String> stream = Generator.repeat("A");
```

---

## Installation (Version 1.0.3)

### Maven
```xml
<dependency>
    <groupId>io.github.sganesh-code</groupId>
    <artifactId>functional-java</artifactId>
    <version>1.0.3</version>
</dependency>
```

#### Gradle
```gradle
implementation 'io.github.sganesh-code:functional-java:1.0.3'
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
