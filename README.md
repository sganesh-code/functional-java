![build](https://travis-ci.org/sganesh-code/functional-java.svg?branch=master)
# functional-java

Functional Java is an initiative to bring functional APIs to Java without compromising on object-oriented programming principles. It provides purely functional, immutable data structures with an API inspired by Haskell.

## Core Concepts & Design

The library is built around a single, powerful abstraction: the `Collection<T>` interface.

### The Core Triad
To add support for a new data structure in this library, one only needs to implement three fundamental methods. The rest of the functional API is derived automatically via default methods in the `Collection` interface.

1.  **`empty()`**: Returns an empty implementation of the data structure.
2.  **`build(T input)`**: Adds an element to the structure and returns a **new** instance, preserving immutability.
3.  **`foldl(R seed, BiFunction<R, T, R> fn)`**: The left-associative reduction operation that serves as the basis for almost all other transformations.

### Derived Functional Power
By implementing the triad above, a data structure instantly gains:
*   **Transformations**: `map`, `flatMap`, `filter`, `reverse`
*   **Combinators**: `concat`, `intersperse`, `intercalate`
*   **Applicative Functors**: `apply`, `liftA2`, `liftA3`, `liftA4`
*   **Traversable**: `traverse`, `sequence`
*   **Utilities**: `take`, `drop`, `slice`, `find`, `any`, `all`, `mkString`

---

## Supported Data Structures

The library provides a variety of persistent data structures optimized for functional use:

### Sequential
*   **`List`**: A classic functional linked list.
*   **`Vector`**: A persistent bitmapped vector trie providing near O(1) random access and updates.
*   **`Array`**: A functional wrapper around standard arrays.
*   **`LazyList`**: A list with deferred evaluation.

### Associative & Sets
*   **`Set`**: An immutable set implemented using a self-balancing AVL Tree.
*   **`HashMap`**: A high-performance Hash Array Mapped Trie (HAMT).
*   **`Map`**: A binary tree-based associative mapping.

### Functional Primitives
*   **`Maybe`**: Represents optional values (similar to `Optional` but with a richer functional API).
*   **`Either`**: Represents a value of one of two possible types (typically used for error handling).
*   **`Tuple`**: A simple product type for grouping related values.

### Structural & Abstract
*   **`Stack`**, **`Queue`**, **`Deque`**: Standard linear structures with functional APIs.
*   **`PriorityQueue`**: A heap-based priority queue.
*   **`RoseTree`**: A multi-way tree structure.
*   **`Graph`**: A functional representation of graphs.

---

## Usage

### Installation (Version 1.0.0)

#### Maven
```xml
<dependency>
    <groupId>io.github.sganesh-code</groupId>
    <artifactId>functional-java</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### Gradle
```gradle
implementation 'io.github.sganesh-code:functional-java:1.0.0'
```

---

## Examples

### Basic Transformations
```java
List.of(1,2,3).map(i -> i * 2); 
// > [2, 4, 6]

List.of(1,2,3,4,5,6).filter(i -> i % 2 == 0); 
// > [2, 4, 6]

List.of(1,2,3).flatMap(i -> List.of(i, i * 10));
// > [1, 10, 2, 20, 3, 30]
```

## Showcase: The Declarative Advantage

While standard Java has introduced `Stream` and `Optional`, they often remain verbose for complex logic. `functional-java` allows you to express "The What" rather than "The How."

### 1. Atomic Batch Retrieval (`traverse`)
Fetching metadata for a list of IDs and requiring **all** to exist (Atomic Success) or failing entirely.

**Standard Java:**
```java
List<Profile> results = new ArrayList<>();
for (String id : ids) {
    Profile p = service.findById(id);
    if (p == null) return Optional.empty(); // Manual atomic failure
    results.add(p);
}
return Optional.of(results);
```

**functional-java:**
```java
// List<String> -> Maybe<List<Profile>>
return ids.traverse(service::findById);
```

### 2. Validation Pipelines (`Either`)
Processing a checkout where each step can fail with a specific reason.

```java
Either<String, Order> result = Either.<String, Cart>right(cart)
    .flatMap(this::validateStock)      // returns Either<String, Stock>
    .flatMap(this::calculateTax)       // returns Either<String, TaxedOrder>
    .flatMap(this::applyDiscount);     // returns Either<String, Order>

result.either(
    error -> log.error("Checkout failed: " + error),
    order -> ship(order)
);
```

### 3. Complex Network Analytics (`Graph`)
Finding all "Influencers" reachable from a user who are also "Java" experts.

```java
long count = network.bfs("Alice")               // Graph Breadth-First Search
    .map(profiles::get)                        // Map to Maybe<Profile>
    .flatMap(m -> (Collection<Profile>) m)     // Flatten to existing profiles only
    .filter(p -> p.interests().any(i -> i.equals("Java")))
    .count();
```

### 4. Structural Aggregation (`RoseTree` & `foldl`)
Auditing a corporate hierarchy to calculate total budget across all regional hubs.

```java
// globalTopology is a RoseTree<Hub>
double totalBudget = globalTopology.foldl(0.0, (acc, hub) -> acc + hub.getBudget());
```

---

## Comparison Summary

| Feature | Standard Java | functional-java |
| :--- | :--- | :--- |
| **Error Handling** | `try-catch` or `null` checks | Monadic `Either` / `Maybe` |
| **Transformation** | `stream().map(...).collect(...)` | Direct `.map(...)` |
| **Atomic Ops** | Manual loop + early exit logic | `traverse(fn)` |
| **Persistence** | `Collections.unmodifiableList(...)` | Inherently Immutable |
| **Data Structures**| Mostly Linear/Associative | Graphs, RoseTrees, Deques, HAMTs |

---

### Error Handling with Either
```java
Either.lefts(List.of(Either.right(1), Either.left("Error"), Either.right(3)));
// > ["Error"]
```

---


