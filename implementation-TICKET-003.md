# Implementation Plan: TICKET-003 - Task-Specific Automaton Support

- [x] **🎟️ [TICKET-003]: Task-Specific Automaton Support**
  - **Description:** Provide ergonomic support for using `Automaton` with the library's `Task` type, which is the primary effect type in FJ.
  - **Scope:**
    - **In scope:**
        - `io.github.senthilganeshs.fj.automaton.Automaton` (factory methods)
    - **Out of scope:**
        - Unit tests (handled in TICKET-004).
  - **Implementation Tasks:**
    - [x] **Investigate:**
        - Verify the location and access of `Task.monad` in `@src/main/java/io/github/senthilganeshs/fj/ds/Task.java`.
      - *Confirmed `Task.monad` exists as a public static final field.*
    - [x] **Add Task Factory Method:**
        - Update `@src/main/java/io/github/senthilganeshs/fj/automaton/Automaton.java`.
        - Add `public static <K, S, I, O> Automaton<Task.µ, K, S, I, O> ofTask(Machine<S, I, O> machine, Interpreter<Task.µ, O, I> interpreter, Repository<Task.µ, K, S> repository)`.
      - *Added `ofTask` static factory method to `Automaton.java` which uses `Task.monad`.*
    - [x] **Verification:**
        - Run `./gradlew classes` to ensure compilation.
      - *Verified compilation using `./gradlew classes`. Build successful.*
