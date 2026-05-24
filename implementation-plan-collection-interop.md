# Implementation Plan: Collection Covariant Return Type Standardization
**Target Repository:** functional-java
**Reference Design:** Standardizing `Collection<T>` return types across all data structures to improve interoperability and reduce API surface complexity.

The following tickets break down the implementation of the epic.

---

- [x] **🎟️ [TICKET-001]: Audit and Impact Analysis of Covariant Overrides**
  - **Description:** Conduct a comprehensive audit of all interfaces extending `Collection<T>` to identify covariant overrides of standard collection methods (map, filter, flatMap, etc.). Document the current state and identify necessary narrowing APIs (`from`, `of`).
  - **Scope:**
    - **In scope:** `List.java`, `Stack.java`, `Maybe.java`, `Either.java`, `Validation.java`, `Array.java`, `Vector.java`, `Queue.java`, `Deque.java`, `Set.java`, `HashMap.java`.
    - **Out of scope:** Non-collection data structures.
  - **Implementation Tasks:**
    - [ ] Audit `@src/main/java/io/github/senthilganeshs/fj/ds/Collection.java` for all default methods that are commonly overridden.
    - [ ] List all covariant overrides in `@src/main/java/io/github/senthilganeshs/fj/ds/List.java` and `@src/main/java/io/github/senthilganeshs/fj/ds/Stack.java`.
    - [ ] List all covariant overrides in `@src/main/java/io/github/senthilganeshs/fj/ds/Maybe.java`, `@src/main/java/io/github/senthilganeshs/fj/ds/Either.java`, and `@src/main/java/io/github/senthilganeshs/fj/ds/Validation.java`.
    - [ ] Verify existence of `from(Collection<T>)` or equivalent narrowing APIs for each affected data structure.

- [x] **🎟️ [TICKET-002]: Standardize List and Stack Return Types**
  - **Description:** Remove covariant return type overrides in `List` and `Stack` interfaces. These structures should rely on the default `Collection` implementations for map-like operations, returning `Collection<T>` at compile-time.
  - **Scope:**
    - **In scope:** `List.java`, `Stack.java`.
  - **Implementation Tasks:**
    - [ ] Remove overrides for `map`, `flatMap`, `filter`, `concat`, `take`, `drop`, `slice`, `reverse`, `mapMaybe` in `@src/main/java/io/github/senthilganeshs/fj/ds/List.java`.
    - [ ] Remove overrides for `map`, `flatMap`, `filter`, `take`, `drop`, `concat`, `mapMaybe` in `@src/main/java/io/github/senthilganeshs/fj/ds/Stack.java`.
    - [ ] Ensure `List.from(Collection<T>)` and `Stack.from(Collection<T>)` are correctly implemented to facilitate narrowing when needed.
    - [ ] Update any internal usages within these files that depend on the narrower types.

- [x] **🎟️ [TICKET-003]: Standardize Applicative and Monadic Data Structures**
  - **Description:** Remove covariant return type overrides in `Maybe`, `Either`, and `Validation`. While these are often used as monads, standardizing them as `Collection` implementations simplifies generic data processing pipelines.
  - **Scope:**
    - **In scope:** `Maybe.java`, `Either.java`, `Validation.java`.
  - **Implementation Tasks:**
    - [ ] Remove `map` and `flatMap` overrides in `@src/main/java/io/github/senthilganeshs/fj/ds/Maybe.java`.
    - [ ] Remove `map` and `flatMap` overrides in `@src/main/java/io/github/senthilganeshs/fj/ds/Either.java`.
    - [ ] Remove `map` override in `@src/main/java/io/github/senthilganeshs/fj/ds/Validation.java`.
    - [ ] Add `Maybe.from(Collection<T>)`, `Either.from(Collection<T>)`, and `Validation.from(Collection<T>)` if they do not already exist.

- [x] **🎟️ [TICKET-004]: Refactor Core Triad Signatures (empty, build)**
  - **Description:** Ensure that the core triad methods `empty()` and `build(T)` in the `Collection` interface are consistently implemented without unnecessary covariant overrides that might lead to tight coupling.
  - **Scope:**
    - **In scope:** `Collection.java` and all implementations.
  - **Implementation Tasks:**
    - [ ] Review `empty()` and `build(T)` in all classes implementing `Collection`.
    - [ ] Ensure they return `Collection<T>` in their `@Override` declarations to encourage working with the base interface.
    - [ ] Verify that `functor()` and `monad()` implementations in these classes still utilize optimized internal methods where applicable.

- [x] **🎟️ [TICKET-005]: Global Test Suite Refactoring and Validation**
  - **Description:** The changes to return types will cause significant compile-time errors in the test suite and potentially in user code. This ticket covers the systematic update of tests to use `Collection` types or explicit narrowing.
  - **Scope:**
    - **In scope:** All files in `@src/test/java/io/github/senthilganeshs/fj/ds/`.
  - **Implementation Tasks:**
    - [ ] Run `mise exec -- gradle test` and identify all failure points.
    - [ ] Update `ListTest.java`, `StackTest.java`, `MaybeTest.java`, `EitherTest.java`, etc., to accommodate the new return types.
    - [ ] Introduce "Interoperability Tests" that demonstrate mixing different collection types in a single pipeline through the `Collection` interface.

- [ ] **🎟️ [TICKET-006]: Documentation and Migration Guide**
  - **Description:** Document the architectural shift towards "Collection-first" interop. Provide a migration guide for users who were relying on covariant return types.
  - **Scope:**
    - **In scope:** `README.md`, `GEMINI.md`.
  - **Implementation Tasks:**
    - [ ] Update `@README.md` examples to show usage with `Collection` types.
    - [ ] Add a section on "Narrowing" in `@README.md` explaining how to use `from(Collection<T>)`.
    - [ ] Update `@GEMINI.md` with the new design principle regarding covariant overrides.
