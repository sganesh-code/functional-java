# Implementation Plan: TICKET-002 - Standardize List and Stack Return Types

- [x] **🎟️ [TICKET-002]: Standardize List and Stack Return Types**
  - **Description:** Remove covariant return type overrides in `List` and `Stack` interfaces. These structures should rely on the default `Collection` implementations for map-like operations, returning `Collection<T>` at compile-time.
  - **Scope:**
    - **In scope:** `@src/main/java/io/github/senthilganeshs/fj/ds/List.java`, `@src/main/java/io/github/senthilganeshs/fj/ds/Stack.java`.
    - **Out of scope:** Other data structures (Maybe, Either, etc. - covered in TICKET-003).
  - **Implementation Tasks:**
    - [x] **Standardize List Overrides:** 
      - [x] Remove covariant overrides for `map`, `flatMap`, `filter`, `concat`, `take`, `drop`, `slice`, `reverse`, `mapMaybe`, and `zipWith` in `@src/main/java/io/github/senthilganeshs/fj/ds/List.java`.
        - *Standardized List by removing covariant overrides; they now default to returning Collection<T>.*
      - [x] Ensure `List.from(Collection<T>)` and `List.of(T...)` are robust and used where narrowing is needed.
        - *Verified robustness of existing factory methods.*
    - [x] **Standardize Stack Overrides:** 
      - [x] Remove covariant overrides for `map`, `flatMap`, `filter`, `take`, `drop`, `concat`, `reverse`, and `mapMaybe` in `@src/main/java/io/github/senthilganeshs/fj/ds/Stack.java`.
        - *Standardized Stack by removing covariant overrides.*
      - [x] Ensure `Stack.from(Collection<T>)` and `Stack.of(T...)` are robust and used where narrowing is needed.
        - *Verified Stack factory methods.*
    - [x] **Update Internal Usages:**
      - [x] Identify and update any internal recursive calls or helper methods within `List.java` and `Stack.java` that might break due to type changes.
        - *Updated List.tail() and StackImpl.build() to maintain internal consistency.*
    - [x] **Verify Core Implementation:** Run a targeted build of the `main` classes to ensure they compile correctly (ignoring test failures for now, as they are covered in TICKET-005).
      - *Successfully compiled main classes after fixing downstream breakages in Task, TaskEither, HyperLogLog, AsyncStream, and Automaton.*

