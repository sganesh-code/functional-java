# Implementation Plan: TICKET-004 - Refactor Core Triad Signatures (empty, build)

- [x] **🎟️ [TICKET-004]: Refactor Core Triad Signatures (empty, build)**
  - **Description:** Ensure that the core triad methods `empty()` and `build(T)` in the `Collection` interface are consistently implemented without unnecessary covariant overrides. This enforces the architectural mandate that basic construction returns the base interface.
  - **Scope:**
    - **In scope:** All classes implementing `Collection<T>`, specifically looking for `build(T)` and `empty()` overrides.
    - **Out of scope:** Methods other than `empty()` and `build(T)`.
  - **Implementation Tasks:**
    - [x] **Audit Structured Collections:**
      - [x] Update `Queue.java` to return `Collection<T>` from `build(T)`.
        - *Standardized Queue.build() return type.*
      - [x] Update `Set.java` to return `Collection<T>` from `build(T)`.
        - *Standardized Set.build() and AVLTree.build() return types; introduced add() as the Set-returning variant.*
      - [x] Update `Array.java` to return `Collection<T>` from `build(T)`.
        - *Verified Array.build() and fixed a potential capacity bug.*
      - [x] Check `Vector.java`, `RoseTree.java`, `HyperLogLog.java`, `BloomFilter.java`.
        - *Confirmed they already followed the generic pattern or didn't have covariant overrides.*
    - [x] **Internal Cleanup:** Resolve any internal casting issues or helper method breakages within these classes.
      - *Fixed Set.of, Set.union/intersect/difference and Map.put to use add() or explicit narrowing.*
    - [x] **Verify Core Implementation:** Run `mise exec -- ./gradlew classes` to ensure main source set remains clean.
      - *Successfully compiled all main source classes.*

