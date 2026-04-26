# Agent Mandates: functional-java

This file defines the foundational rules for AI agents working on this repository. These instructions take absolute precedence over general defaults.

## 🛠 Tooling & Environment
- **Gradle Commands**: ALWAYS use `mise` to execute Gradle tasks.
  - Correct: `mise exec -- gradle test`
  - Incorrect: `./gradlew test`

## 🏗 Architectural Design Principles
- **The Core Triad**: All collection logic MUST derive from three methods: `empty()`, `build(T)`, and `foldl(seed, fn)`.
- **Persistence**: All data structures MUST be immutable/persistent. Every "update" operation must return a new instance.
- **Paradigm Balance**: Enforce functional principles (referential transparency) without violating Java's OO standards (e.g., use covariant overrides to keep the API fluent).
- **FIFO/LIFO Consistency**: 
  - `List` and `Stack` follow **LIFO** (Last-In, First-Out).
  - `Queue` and `Deque` follow **FIFO** (First-In, First-Out).

## 🚀 Performance & Practicality
- **Iteration over Recursion**: To prevent `StackOverflowError` and maximize speed, core engines (folds, maps, filters) MUST be implemented using iterative loops, not recursion.
- **Practicality over Purity**: While functional purity is preferred, practicality and readability take precedence. If a functional implementation is significantly harder to read or maintain than an imperative one, use the imperative style.

## 🧪 Quality & Testing
- **Regression Testing**: Any modification to existing APIs MUST be verified by running the full test suite.
- **Exhaustive Coverage**: New features MUST include tests for:
  - Positive scenarios (happy path).
  - Negative scenarios (error handling, invalid inputs).
  - Edge cases (empty collections, single-element collections, extreme values).

## 📦 Release & Security
- **Publication Protocol**: NEVER push a version tag (e.g., `v1.0.4`) or trigger a Maven Central publication without explicit user confirmation.
- **GPG & Secrets**: Never commit GPG keys, ring files, or `.asc` files. Ensure they are listed in `.gitignore`.

## 🔍 Optics & Typeclasses
- **Standard Optics**: Prefer using standard optics factories (e.g., `Either.rightP()`, `Maybe.someP()`, `Collection.eachP()`) over manual construction.
- **Algebraic Strategies**: Use `Eq<T>` and `Ord<T>` typeclasses to decouple comparison logic from the data structures.
