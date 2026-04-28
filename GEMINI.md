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
- **Feature Completion & Release Nudge**: Once a feature branch's changes are validated and the build is passing with all tests, you MUST nudge the user to release the changes and tag a new version before proceeding to any unrelated tasks or new features. This ensures the library evolves incrementally and downstream consumers (like `transformation-engine`) remain synced.
- **Publication Protocol**: NEVER push a version tag (e.g., `v1.0.4`) or trigger a Maven Central publication without explicit user confirmation.
- **Triggering Deployment**: To deploy a new version to Maven Central:
  1.  Update the `version` in `build.gradle` (e.g., `1.1.1`).
  2.  Update the version strings in the `Installation` and example snippets of `README.md`.
  3.  Commit and push the change to `master`.
  4.  Create and push a Git tag matching the version (prefixed with 'v'):
      ```bash
      git tag v1.1.1
      git push origin v1.1.1
      ```
  5.  The GitHub CI runner will automatically detect the `v*` tag and execute the `publish` job.
- **GPG & Secrets**: Never commit GPG keys, ring files, or `.asc` files. Ensure they are listed in `.gitignore`.

## 🔍 Optics & Typeclasses
- **Standard Optics**: Prefer using standard optics factories (e.g., `Either.rightP()`, `Maybe.someP()`, `Collection.eachP()`) over manual construction.
- **Algebraic Strategies**: Use `Eq<T>` and `Ord<T>` typeclasses to decouple comparison logic from the data structures.
