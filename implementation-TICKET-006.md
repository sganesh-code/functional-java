# Implementation Plan: TICKET-006 - Documentation and Migration Guide

- [x] **🎟️ [TICKET-006]: Documentation and Migration Guide**
  - **Description:** Document the architectural shift towards "Collection-first" interop. Provide a migration guide for users who were relying on covariant return types.
  - **Scope:**
    - **In scope:** `@README.md`, `@GEMINI.md`.
    - **Out of scope:** Modifying source code.
  - **Implementation Tasks:**
    - [x] **Update README Examples:** Update usage examples in `@README.md` to show usage with generic `Collection` types.
      - *Updated interop, transformation, and optic examples to use generic Collection return types.*
    - [x] **Document Narrowing:** Add a section on "Type Narrowing" in `@README.md` explaining how to use `from(Collection<T>)` when implementation-specific features are needed.
      - *Added "Type Narrowing" section with examples for List and Maybe.*
    - [x] **Update Design Mandates:** Update `@GEMINI.md` with the new design principle regarding covariant overrides (mandating return of base `Collection` for construction and transformation methods).
      - *Formalized "Standardized Return Types" as a top-level architectural mandate.*
    - [x] **Final Review:** Ensure all documentation aligns with the implemented changes.
      - *Verified all documentation updates align with the technical implementation.*

