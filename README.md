![build](https://github.com/sganesh-code/functional-java/actions/workflows/ci.yml/badge.svg?branch=master)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.sganesh-code/functional-java.svg)](https://central.sonatype.com/artifact/io.github.sganesh-code/functional-java)

# functional-java

**Purely functional, immutable data structures for the modern Java developer.**

`functional-java` is an initiative to bring expressive, Haskell-inspired functional APIs to Java without compromising on object-oriented principles. Built for robustness and clarity, it provides a unified set of tools to transform complex data manipulations into concise, declarative pipelines.

---

## 📋 Table of Contents
1. [Core Concepts](#-core-concepts)
2. [Persistent Data Structures](#-persistent-data-structures)
3. [Railway Oriented Programming](#-railway-oriented-programming)
4. [Async & Side Effects](#-async--side-effects)
5. [Resource-Safe Streaming](#-resource-safe-streaming)
6. [Optics: Deep Immutable Updates](#-optics-deep-immutable-updates)
7. [Serialization & Codecs](#-serialization--codecs)
8. [Property-Based Testing](#-property-based-testing)
9. [Installation](#-installation)
10. [License](#-license)

---

## 🧩 Core Concepts

### The HKT Bridge
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

### The Core Triad Design
All 40+ combinators (`map`, `filter`, `traverse`, `chunk`, etc.) are powered by just three fundamental methods that any new data structure can implement:
1.  **`empty()`**: Returns an empty instance.
2.  **`build(T)`**: Persistent addition of an element.
3.  **`foldl(seed, fn)`**: The fundamental iterative reduction engine.

---

## 🛡️ Persistent Data Structures

Unlike standard Java collections, `functional-java` structures are **Persistent**. Updating a structure returns a new version while sharing as much memory as possible with the original, making them ideal for concurrent systems.

### `Vector` vs. `ArrayList`
| Feature | `java.util.ArrayList` | `fj.ds.Vector` |
| :--- | :--- | :--- |
| **Mutability** | Mutable (requires defensive copying) | **Immutable (Persistent)** |
| **Safety** | Risk of `ConcurrentModificationException` | **Thread-safe by design** |
| **Updates** | O(N) for deep copy + update | **O(log32 N) via Bitmapped Trie** |

**Standard Java (Boilerplate & Risk):**
```java
// Defensive copying is required for safety
List<String> list = new ArrayList<>(existingList);
list.add("New Element"); 
return Collections.unmodifiableList(list);
```

**Functional Java (Concise & Safe):**
```java
return vector.build("New Element"); // Original is untouched
```

---

## 🛤️ Railway Oriented Programming

Handle errors and optionality without exceptions or null-checks.

### `Validation` (Accumulating Errors)
Standard `try-catch` or `Optional` short-circuits on the first failure. `Validation` allows you to collect *all* errors (e.g., in a web form).

```java
Validation<List<String>, User> result = validateName(input.name)
    .liftA2((name, email) -> new User(name, email), validateEmail(input.email), List.semigroup());

// result will contain ALL validation failures, not just the first one.
```

### `Maybe` vs. `Optional`
`Maybe` is not just a null-wrapper; it's a full `Traversable` and `Applicative`.

```java
// Sequence a list of maybes: List<Maybe<A>> -> Maybe<List<A>>
Maybe<List<Integer>> result = Collection.sequence(listOfMaybes);
```

---

## ⚡ Async & Side Effects

### `Task` vs. `CompletableFuture`
`CompletableFuture` is **eager**—it starts executing immediately upon creation. `Task` is **lazy**—it is a *description* of an operation that only runs when explicitly requested.

```java
// This just describes a task; nothing has happened yet.
Task<String> fetchUser = Task.of(() -> httpClient.get("/user/1"));

// Tasks can be retried, timed out, or cancelled before they run.
Task<String> safeTask = fetchUser.timeout(5, TimeUnit.SECONDS).retry(3);

String user = safeTask.run(); // Execution starts here.
```

### Dependency Injection with `Reader`
Avoid passing configuration through every layer of your application.

```java
Reader<Config, String> app = Reader.<Config>ask().map(Config::getEndpoint);
String url = app.run().apply(new Config("https://api.com"));
```

---

## 🌊 Resource-Safe Streaming

### `Stream` vs. `java.util.stream.Stream`
Java's native streams are one-shot and don't handle side effects or resource management algebraically. `fj.stream.Stream` uses the `bracket` pattern to guarantee resource cleanup.

```java
// Guaranteed to close the file even if processing fails
Stream<IO.µ, Byte> data = Stream.bracket(
    IO.of(() -> new FileInputStream("data.bin")), 
    file -> Stream.fromInputStream(file), 
    file -> IO.of(() -> file.close()), 
    IO.monad);
```

---

## 🔬 Optics: Deep Immutable Updates

Updating a nested field in an immutable record hierarchy is traditionally verbose in Java. `Lens` makes it trivial.

**Standard Java:**
```java
User updated = new User(
    user.id(),
    new Address(
        user.address().street(),
        "New City", // updated
        user.address().zip()
    ),
    user.roles()
);
```

**Functional Java (with `Lens`):**
```java
Lens<User, String> cityL = RecordOptics.of(User.class, User::address)
    .compose(RecordOptics.of(Address.class, Address::city));

User updated = cityL.set("New City", user);
```

### Composed Traversals
Use the `Optics` entry point to compose traversals over nested structures (e.g., `Maybe<List<A>>`).

```java
// Focus on every element in a nested List inside a Maybe
Optics.<List<User>>maybe()
    .compose(Optics.list())
    .forEach(maybeUsers, user -> System.out.println(user.name()));
```

---

## 📦 Serialization & Codecs

`fj.codec` provides purely functional, type-safe binary serialization without the fragility of standard Java serialization.

```java
// Create a combined Encoder
Encoder<User> userEncoder = (out, user) -> {
    Either<String, Void> res1 = Codec.stringEncoder().encode(out, user.name());
    if (res1.isLeft()) return res1;
    return Codec.intEncoder().encode(out, user.age());
};

// Create a combined Decoder
Decoder<User> userDecoder = in -> 
    Codec.stringDecoder().decode(in).flatMapEither(name -> 
        Codec.intDecoder().decode(in).map(age -> new User(name, age))
    );

// Serialize safely to any DataOutput
userEncoder.encode(output, user);
```

### Declarative JSON Mapping
`JsonCodec<A>` provides high-level mapping between domain objects and the `JsonValue` AST.

```java
// Convert primitives, FJ collections, and Records to JSON AST in one call
JsonValue json = JsonValue.of(user);

// Use a JsonCodec for bidirectional mapping
JsonCodec<User> userCodec = JsonCodec.of(); 
JsonValue root = userCodec.encode(user);
```

---

## 🧪 Property-Based Testing

Instead of writing individual test cases, define **invariants** that must hold true for all possible inputs.

```java
// The library will generate 100 random integers to find a counter-example
Property<Integer> p = Property.forAll(Gen.choose(1, 100), i -> i > 0);
p.assertTrue(100); 

// Automatic Shrinking: If it fails for i=50, it will automatically 
// try to find the smallest failing input (e.g., 1).
```

### Advanced Generators
Compose complex data generators using weighted frequencies or random selections.

```java
Gen<Integer> weighted = Gen.frequency(List.of(
    Tuple.of(90, Gen.pure(1)), // 90% chance of 1
    Tuple.of(10, Gen.pure(2))  // 10% chance of 2
));
```

---

## 🌉 Zero-Cost Interop

Seamlessly integrate with existing Java code using `.from()` adapters:

```java
// Convert any Iterable to a persistent Vector
Vector<String> vector = Vector.from(javaList);

// Convert back to standard Java for legacy APIs
List<String> javaList = vector.toJavaList();
```

---

## ⚡ Boilerplate Shortcuts

Reduce Java verbosity with built-in iteration and interop helpers.

### Indexed Iteration
Avoid manual counters or `zipWithIndex`.

```java
// Side-effects with index (val, index)
collection.forEach((val, i) -> System.out.println(i + ": " + val));
```

### Standard Java Interop
Easily bridge with libraries expecting standard `java.util` structures.

```java
// Convert FJ HashMap to standard mutable Map
Map<String, User> map = fjMap.toJavaMap();
```

---

## 🚀 Installation (Version 1.3.7)

### Maven
```xml
<dependency>
    <groupId>io.github.sganesh-code</groupId>
    <artifactId>functional-java</artifactId>
    <version>1.3.7</version>
</dependency>
```

#### Gradle
```gradle
implementation 'io.github.sganesh-code:functional-java:1.3.7'
```

---

## ⚖️ License

Distributed under the **GPL-v3.0** License. See `LICENSE` for more information.
