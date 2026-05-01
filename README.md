![build](https://github.com/sganesh-code/functional-java/actions/workflows/ci.yml/badge.svg?branch=master)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.sganesh-code/functional-java.svg)](https://central.sonatype.com/artifact/io.github.sganesh-code/functional-java)

# functional-java

**Purely functional, immutable data structures for the modern Java developer.**

`functional-java` is an initiative to bring expressive, Haskell-inspired functional APIs to Java without compromising on object-oriented principles. Built for robustness and clarity, it provides a unified set of tools to transform complex data manipulations into concise, declarative pipelines.

---

## 🚀 Key Features

*   **100% Immutable**: All structures are persistent; updates return new versions while sharing structure.
*   **Unified API**: 40+ functional methods (map, filter, traverse, partition, etc.) available on every data structure.
*   **Higher-Kinded Types (HKT)**: A lightweight polymorphism bridge allowing formal `Functor`, `Applicative`, and `Monad` typeclasses in Java.
*   **Haskell-Style Effects**: Pure representations for side effects, including `IO`, `Reader`, `State`, `Writer`, and `TaskEither`.
*   **Resource-Safe Streaming**: Lazy, effectful data streams with automatic resource management (`Stream`, `Pipe`, `Sink`, `bracket`).
*   **Property-Based Testing**: QuickCheck-style validation framework with monadic data generators (`Gen`), property invariants, and automatic shrinking.
*   **Optics Engine**: Advanced Lenses, Prisms, and Traversals for deep immutable updates.
*   **Production-Grade Parser**: Monadic parser combinators with source-position tracking (line/column) and expression handling.
*   **Performance Optimized**: Achieving up to **400x speedup** over recursive implementations for large datasets.

---

## 📋 Table of Contents
1. [The HKT Bridge](#the-hkt-bridge)
2. [Core Triad Design](#core-triad-design)
3. [Zero-Cost Interop](#zero-cost-interop)
4. [Supported Data Structures](#supported-data-structures)
5. [API Showcase](#api-showcase)
6. [Installation](#installation)
7. [Performance](#performance)
8. [License](#license)

---

## The HKT Bridge

Java lacks native Higher-Kinded Types (abstracting over `F<A>`). `functional-java` bridges this gap using the **Lightweight Higher-Kinded Polymorphism** pattern, enabling truly polymorphic functional programming.

```java
// Formal Monad definition in Java
public interface Monad<W> extends Applicative<W> {
    <A, B> Higher<W, B> flatMap(Function<A, Higher<W, B>> fn, Higher<W, A> fa);
}

// Composition across ANY monad (Maybe, List, Task, etc.)
public <W, A, B> Higher<W, B> sequence(Monad<W> m, List<Higher<W, A>> list) {
    return list.foldl(m.pure(List.nil()), (acc, fa) -> 
        m.liftA2((l, a) -> l.build(a), acc, fa));
}
```

---

## Core Triad Design

The library is built on the `Collection<T>` interface. To implement any complex functional behavior, a data structure only needs three core methods:

1.  **`empty()`**: Returns an empty instance.
2.  **`build(T)`**: Persistent addition of an element.
3.  **`foldl(seed, fn)`**: The fundamental reduction engine.

By implementing these, types instantly gain `map`, `flatMap`, `traverse`, `chunk`, `groupBy`, and more.

---

## Zero-Cost Interop

Custom data structures gain interop for free:

```java
// Convert custom Window to persistent Vector for O(1) access
Vector<Double> vector = Vector.from(mySlidingWindow);

// Perform atomic batch operations
Maybe<List<Price>> prices = window.traverse(id -> priceDB.find(id));
```

---

## Supported Data Structures

### 📦 Sequential & Effectful
*   **`List`**, **`Vector`**, **`Array`**, **`LazyList`**, **`NonEmptyList`**.
*   **`Task`**: Asynchronous computations with cancellation and timeout support.
*   **`IO`**: Synchronous, deferred side-effect evaluation.

### 🔍 Associative & Sets
*   **`Set`**, **`HashMap`** (HAMT), **`Map`**, **`PriorityQueue`**.

### 🎭 Functional Primitives
*   **`Maybe`**, **`Either`**, **`Validation`**, **`These`** (Inclusive OR).
*   **`Identity`**, **`Const`**, **`Endo`**, **`Tuple`**.

### 🌳 Structural & Streams
*   **`Stream`**: Effectful, lazy data streams (F-algebraic).
*   **`RoseTree`**, **`Graph`** (with TopoSort, BFS, DFS).

### 🏗 Parsing & Testing
*   **`Parser`**: With line/column tracking and expression combinators.
*   **`Gen`** / **`Property`**: Property-based testing and data generation.

---

## API Showcase

### A. Algebraic Effects & Composition

*   **Dependency Injection (`Reader`)**:
    ```java
    Reader<Config, String> app = Reader.<Config>ask().map(Config::getEndpoint);
    String url = app.run().apply(new Config("https://api.com"));
    ```
*   **Safe Mutation (`State`)**:
    ```java
    State<Integer, String> increment = State.<Integer>get()
        .flatMap(s -> State.modify(i -> i + 1).map(__ -> "Old: " + s));
    ```
*   **Combined Effects (`ReaderTaskEither`)**: DI, Async, and Error handling in one.
    ```java
    ReaderTaskEither<Env, Error, User> auth = ReaderTaskEither.<Env, Error>ask()
        .flatMap(env -> db.findUser(env.token));
    ```

### B. Resource-Safe Streaming

*   **Parallel Evaluation (`parEvalMap`)**:
    ```java
    Stream<Task.µ, User> users = Stream.fromList(ids, Task.monad)
        .parEvalMap(4, id -> db.fetchUserTask(id));
    ```
*   **Automatic Resource Management (`bracket`)**:
    ```java
    Stream<IO.µ, Byte> data = Stream.bracket(
        IO.of(() -> openFile()), 
        file -> Stream.fromFile(file), 
        file -> IO.of(() -> file.close()), 
        IO.monad);
    ```

### C. Property-Based Testing (Lawful Validation)

*   **Declarative Invariants**:
    ```java
    Property<Integer> p = Property.forAll(Gen.choose(1, 100), i -> i > 0);
    p.assertTrue(100); // Run 100 random trials
    ```
*   **Automatic Counter-Example Shrinking**:
    ```java
    // Fails for i > 5. Shrinker will find exactly 6 as the minimal failure.
    Property<Integer> p = Property.forAll(Gen.integer(), Shrink.integer(), i -> i <= 5);
    ```

### D. Advanced Optics & Updates

*   **Zero-Boilerplate Lenses**:
    ```java
    Lens<User, String> nameL = RecordOptics.of(User.class, User::name);
    User updated = nameL.set("Bob", user);
    ```

### E. Parsing & JSON

*   **Expression Parsing**:
    ```java
    Parser<Integer> sum = Parser.integer().chainl1(Parser.character('+').map(__ -> Integer::sum));
    ```
*   **JSON Optics**:
    ```java
    JsonValue city = JsonValue.path("user", "address").compose(JsonValue.stringAt("city")).getMaybe(json);
    ```

---

## Installation (Version 1.3.1)

### Maven
```xml
<dependency>
    <groupId>io.github.sganesh-code</groupId>
    <artifactId>functional-java</artifactId>
    <version>1.3.1</version>
</dependency>
```

#### Gradle
```gradle
implementation 'io.github.sganesh-code:functional-java:1.3.1'
```

---

## Performance

The library achieves up to **400x speedup** over traditional recursive implementations.

**Benchmark Highlights (1,000 Elements):**
*   **List Folding**: ~4.5μs
*   **Vector Access**: ~0.003μs
*   **Map Lookup**: ~0.004μs

---

## License

Distributed under the **GPL-v3.0** License. See `LICENSE` for more information.
