# Implementation Plan: TICKET-003 - Standardize Applicative and Monadic Data Structures

- [x] **🎟️ [TICKET-003]: Standardize Applicative and Monadic Data Structures**
  - **Description:** Remove covariant return type overrides in `Maybe`, `Either`, and `Validation`. While these are often used as monads, standardizing them as `Collection` implementations simplifies generic data processing pipelines.
  - **Scope:**
    - **In scope:** `@src/main/java/io/github/senthilganeshs/fj/ds/Maybe.java`, `@src/main/java/io/github/senthilganeshs/fj/ds/Either.java`, `@src/main/java/io/github/senthilganeshs/fj/ds/Validation.java`.
    - **Out of scope:** Core Triad changes (TICKET-004) and Test Suite updates (TICKET-005).
  - **Implementation Tasks:**
    - [x] **Standardize Maybe:**
      - [x] Add `Maybe.from(Collection<T>)` static narrowing method.
        - *Added Maybe.from() using headMaybe().*
      - [x] Remove covariant overrides for `map` and `flatMap` in `@src/main/java/io/github/senthilganeshs/fj/ds/Maybe.java`.
        - *Standardized Maybe; it now relies on base Collection implementations.*
      - [x] Ensure `flatMapMaybe` exists as an ergonomic handle for monadic chaining.
        - *Ensured flatMapMaybe exists and is correctly implemented.*
    - [x] **Standardize Either:**
      - [x] Add `Either.from(Collection<T>)` static narrowing method.
        - *Added Either.from() with Right-biased head selection.*
      - [x] Remove covariant overrides for `map` and `flatMap` in `@src/main/java/io/github/senthilganeshs/fj/ds/Either.java`.
        - *Standardized Either.*
      - [x] Ensure `flatMapEither` exists as an ergonomic handle for monadic chaining.
        - *Ensured flatMapEither exists.*
    - [x] **Standardize Validation:**
      - [x] Add `Validation.from(Collection<T>)` static narrowing method.
        - *Added Validation.from().*
      - [x] Remove covariant override for `map` in `@src/main/java/io/github/senthilganeshs/fj/ds/Validation.java`.
        - *Standardized Validation.*
    - [x] **Verify Core Implementation:** Run `mise exec -- ./gradlew classes` to ensure main source set compiles (expecting downstream breakages in tests and potentially some main classes).
      - *Successfully compiled all main classes after resolving extensive cascade breakages in Tuple, Stack, Deque, JsonValue, RecordOptics, Parser, Stream, AsyncStream, and HashMap using explicit narrowing.*

