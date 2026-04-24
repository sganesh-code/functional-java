![build](https://travis-ci.org/senthilganeshs/functional-java.svg?branch=master)
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

### Installation (Version 1.2.0)

#### Maven
```xml
<dependency>
    <groupId>io.github.senthilganeshs</groupId>
    <artifactId>functional-java</artifactId>
    <version>1.2.0</version>
</dependency>
```

#### Gradle
```gradle
implementation 'io.github.senthilganeshs:functional-java:1.2.0'
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

### Advanced Functional Patterns
```java
// Applicative Functor: Apply a list of functions to a list of values
List.of(1,2,3).apply(List.of(i -> i + 1, i -> i + 10));
// > [2, 3, 4, 11, 12, 13]

// Traverse: Map a function that returns a Collection, then turn it "inside out"
List.of(1,2,3).traverse(i -> Maybe.some(i + 1));
// > Some([2, 3, 4])

// Sequence: Turn a Collection of Collections inside out
Collection.sequence(List.of(Maybe.some(1), Maybe.some(2)));
// > Some([1, 2])
```

### Persistent Vectors & HashMaps
```java
Vector<Integer> v = Vector.of(1, 2, 3);
v.at(1); // Some(2)
v.update(1, 10); // [1, 10, 3] (Original v is unchanged)

HashMap<String, Integer> map = HashMap.<String, Integer>nil().put("A", 1).put("B", 2);
map.get("A"); // Some(1)
```

### Error Handling with Either
```java
Either.lefts(List.of(Either.right(1), Either.left("Error"), Either.right(3)));
// > ["Error"]
```
