![build](https://github.com/sganesh-code/functional-java/actions/workflows/ci.yml/badge.svg?branch=master)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.sganesh-code/functional-java.svg)](https://central.sonatype.com/artifact/io.github.sganesh-code/functional-java)

# functional-java 2.0 🚀

**Purely functional, polymorphic data structures for the modern Java developer.**

`functional-java` 2.0 introduces a unified architectural model that bridges the gap between OOP polymorphism and FP purity. It transforms Java into a powerful functional environment where different data structures can interoperate seamlessly through a single `Collection` interface.

---

## 📋 Table of Contents
1. [Universal Interoperability](#-universal-interoperability)
2. [Typeclass-Driven Design](#-typeclass-driven-design)
3. [The Core Triad](#-the-core-triad)
4. [Safe Narrowing & HKTs](#-safe-narrowing--hkts)
5. [Optics & Transformation](#-optics--transformation)
6. [Persistent Data Structures](#-persistent-data-structures)
7. [API Showcase](#-api-showcase-traditional-vs-functional)
8. [Property-Based Testing](#-property-based-testing)
9. [Installation](#-installation)
10. [License](#-license)

---

## 🧩 Universal Interoperability

In version 2.0, data structures are no longer isolated monads. They all implement the `Collection<T>` interface, allowing you to mix and match them in a single pipeline.

```java
// Seamless interop: List flatMapping into Maybe!
Collection<Integer> result = List.of(1, 2, 3)
    .flatMap(i -> i % 2 == 0 ? Maybe.some(i * 10) : Maybe.nothing());

// result: [20] (represented as a polymorphic Collection)
```

---

## ⚡ Typeclass-Driven Design

All 50+ combinators (`map`, `flatMap`, `liftA2`, `sequence`, etc.) are implemented once in the base `Collection` interface using integrated typeclass instances (`Functor`, `Applicative`, `Monad`, `Traversable`).

### High-Arity Applicatives
Functional-java 2.0 provides first-class support for high-arity lifting, essential for complex record merging.
```java
// Combining 4 different collections into a single result
Collection<User> users = Collection.liftA4(User::new, names, ages, emails, ids);
```

### Automatic Optimization
By providing an optimized typeclass instance, a data structure automatically optimizes all derived APIs. `Vector` uses high-speed bitmapped-trie paths while still returning the polymorphic `Collection` type.

---

## 💎 The Core Triad

Building a new functional data structure is now trivial. Implement three fundamental methods to gain the entire power of the library:

1.  **`empty()`**: Returns the identity element.
2.  **`build(T)`**: Persistent addition of an element (Snoc).
3.  **`foldl(seed, fn)`**: The fundamental iterative reduction engine.

Any class implementing these three methods instantly gains `map`, `filter`, `traverse`, `chunk`, `scanl`, `zipWith`, `unzip`, `distinct`, `reverse`, and dozens more.

---

## 🛡️ Safe Narrowing & HKTs

Functional-java uses a witness-based encoding (`µ`) to simulate Higher-Kinded Types in Java. When you need implemented implementation-specific logic, use the `TypeReference` API (Super Type Tokens).

```java
// Safely narrow a generic Collection to a specific Maybe
Maybe<User> user = collection.narrow(new TypeReference<Maybe<User>>(){})
    .orElse(Maybe.nothing());
```

---

## 👓 Optics & Transformation

Integration with **Optics** (Lenses, Prisms, Isos) allows for deep, immutable updates with zero boilerplate.

```java
// Update a nested record field deep within a collection
DataRecord updated = DataRecord.fieldP("address")
    .compose(Address.cityL)
    .set("San Francisco", record);
```

---

## 🛠️ Persistent Data Structures

All structures are **Persistent**. Updating a structure returns a new version while sharing memory with the original.

| Structure | Witness (`µ`) | Description |
| :--- | :--- | :--- |
| **`Maybe<T>`** | `Maybe.µ` | Safe handling of optional values. |
| **`Either<L, R>`** | `Either.µ` | Expressive error handling and branching. |
| **`List<T>`** | `List.µ` | Purely functional linked lists (Snoc-list). |
| **`Vector<T>`** | `Vector.µ` | High-performance persistent vectors (Bitmapped Trie). |
| **`HashMap<K, V>`** | `HashMap.µ` | Persistent hash maps (HAMT). |
| **`RoseTree<T>`** | `RoseTree.µ` | Multi-way trees for hierarchical data. |
| **`Set<T>`** | `Set.µ` | Persistent ordered sets (AVL Tree). |

---

## 🎭 API Showcase: Traditional vs. Functional

`functional-java` eliminates the defensive boilerplate and mutable state tracking common in traditional Java, resulting in code that is more expressive, thread-safe, and easier to reason about.

### 🛡️ 1. Safe Optionality
**Traditional:** Deeply nested `if-null` checks or `Optional.isPresent()`.
```java
// Traditional Java
User user = getUser();
String city = "Unknown";
if (user != null && user.getAddress() != null) {
    city = user.getAddress().getCity();
}
```
**functional-java:** Declarative chaining that handles missing data implicitly.
```java
// Expressive and safe
String city = Maybe.of(getUser())
    .flatMap(u -> Maybe.of(u.getAddress()))
    .map(Address::getCity)
    .orElse("Unknown");
```

### 🔄 2. Data Transformation & Purity
**Traditional:** Java Streams are useful but one-time use (lazy/terminal) and often require boilerplate collectors.
```java
// Traditional Streams (Mutable terminal)
List<Integer> list = List.of(1, 2, 3);
List<String> strings = list.stream()
    .filter(i -> i > 1)
    .map(i -> "ID-" + i)
    .collect(Collectors.toList());
```
**functional-java:** Purely functional, persistent transformations that are always valid and reusable.
```java
// Persistent and direct
List<String> strings = List.of(1, 2, 3)
    .filter(i -> i > 1)
    .map(i -> "ID-" + i); 
// strings is a persistent List, no collector needed.
```

### 👓 3. Deep Immutable Updates
**Traditional:** Manual "cloning" or builder patterns for deep updates in immutable records.
```java
// Manual deep update (Boilerplate heavy)
User updated = new User(
    user.getId(),
    user.getName(),
    new Address(
        user.getAddress().getStreet(),
        "New York", // updating city
        user.getAddress().getZip()
    )
);
```
**functional-java:** **Optics** provide a surgical, boilerplate-free way to reach deep into structures.
```java
// Precise and maintainable
User updated = User.addressL
    .compose(Address.cityL)
    .set("New York", user);
```

---

## 🧪 Property-Based Testing

Includes a full PBT suite (`Gen`, `Shrink`, `Property`) for verified reliability.
```java
Property.forAll(Gen.integer(), i -> i + 0 == i).assertTrue(100);
```

---

## 🚀 Installation (Version 2.0.9)

### Maven
```xml
<dependency>
    <groupId>io.github.sganesh-code</groupId>
    <artifactId>functional-java</artifactId>
    <version>2.0.9</version>
</dependency>
```

#### Gradle
```gradle
implementation 'io.github.sganesh-code:functional-java:2.0.9'
```

---

## ⚖️ License
Licensed under the MIT License.
