# Implementation Plan: TICKET-005 - Global Test Suite Refactoring and Validation

- [x] **🎟️ [TICKET-005]: Global Test Suite Refactoring and Validation**
  - **Description:** The changes to return types in previous tickets will cause significant compile-time errors in the test suite. This ticket covers the systematic update of tests to use `Collection` types or explicit narrowing, ensuring behavioral correctness is maintained.
  - **Scope:**
    - **In scope:** All files in `@src/test/java/io/github/senthilganeshs/fj/ds/` and related subdirectories.
    - **Out of scope:** Modifying main source code (unless a regression is found).
  - **Implementation Tasks:**
    - [x] **Identify Failure Points:** Run `mise exec -- ./gradlew test` and document the files with the most errors.
      - *Found 50+ failures initially, mostly due to metadata loss in Either/Validation and incorrect default implementations for Snoc-lists/Stacks.*
    - [x] **Refactor List and Stack Tests:** Update `ListTest.java` and `StackTest.java` to use narrowing where implementation-specific assertions are made.
      - *Updated StackTest and confirmed behavioral correctness of LIFO semantics.*
    - [x] **Refactor Monadic Tests:** Update `MaybeTest.java`, `EitherTest.java`, and `ValidationTest.java`.
      - *Fixed EitherTest to reflect standardized filter behavior and ensured metadata preservation.*
    - [x] **Refactor Structured Collection Tests:** Update `ArrayTest.java`, `QueueTest.java`, `SetTest.java`, etc.
      - *Resolved Queue ordering issues and verified generic interoperability.*
    - [x] **Generic Interop Tests:** Add a new test file `CollectionInteropTest.java` demonstrating seamless mixing of different collection types in generic pipelines.
      - *Added comprehensive interop tests demonstrating generic mapping, concating, and narrowing.*
    - [x] **Final Validation:** Ensure `mise exec -- ./gradlew test` passes completely.
      - *Successfully passed all 283+ tests across the entire project.*

