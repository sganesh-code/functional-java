# Reactive Programming Support Epic Plan

This file is the execution source of truth for Reactor support in `functional-java`.

Implementation must not start until this file exists and has been reviewed as the plan for the work.

`functional_java_reactor_core_integration.md` remains research input only. It informs the plan, but it is not an implementation checklist.

## Scope

Add Reactor Core support to the same artifact while keeping the core functional primitives free of Reactor-specific public methods.

Public application APIs should continue to use:

- `Task`
- `TaskEither`
- `ReaderTaskEither`
- `Resource`
- `Deferred`
- `Fiber`
- `Outcome`
- `AsyncStream`

Reactor types may appear only in a dedicated bridge package for framework edges such as Spring WebFlux.

## API Boundary Rules

- Do not add `toMono`, `fromMono`, `toFlux`, or `fromFlux` methods directly on core functional primitives.
- Keep Reactor imports out of core packages such as `io.github.senthilganeshs.fj.ds` and `io.github.senthilganeshs.fj.stream`.
- Put Reactor-specific signatures in a dedicated bridge package, for example `io.github.senthilganeshs.fj.reactor`.
- Keep the same Gradle artifact for core and bridge code.
- Treat Reactor as an interop dependency, not as the library’s public abstraction layer.

## Public API Decisions

- `Task<A>` remains the primary single-result async primitive.
- `TaskEither<E, A>` remains the primary typed-error async primitive.
- `ReaderTaskEither<R, E, A>` remains the preferred single-result environment-aware API.
- `AsyncStream<A>` is the preferred public replacement for Reactor `Flux<A>`.
- `Resource<A>` is the preferred acquisition/use/release abstraction.
- `Deferred<A>` is the preferred one-shot completion primitive.
- `Fiber<E, A>` and `Outcome<E, A>` model started tasks, completion, failure, and cancellation.
- Reactor bridging should be expressed through utility classes, not instance methods on the core primitives.

## Epics

### Epic 0: Planning Artifact

Goal: establish and preserve the implementation plan before code changes begin.

Tasks:

1. Create `epic-planning.md` at the repository root.
2. Capture the finalized epic plan, API boundary rules, assumptions, and acceptance criteria.
3. State explicitly that implementation must not start until `epic-planning.md` exists.
4. Keep `functional_java_reactor_core_integration.md` as research input, not as an implementation checklist.

Acceptance criteria:

- `epic-planning.md` exists at the repository root.
- The file separates epics, tasks, public API decisions, test strategy, and assumptions.
- The plan is clear enough to drive implementation without revisiting the research note for task ordering.

### Epic 1: Effect Runtime Foundation

Goal: strengthen `Task` so Reactor and other async sources can be adapted without blocking.

Tasks:

1. Add `Task.defer(...)` for subscription-time lazy task construction.
2. Add `Task.asyncCancelable(...)` with success, failure, and cancel callbacks.
3. Introduce small callback/canceler types, either nested in `Task` or package-level in `fj.ds`.
4. Add recovery helpers such as `Task.recover(...)`, `Task.recoverWith(...)`, and `Task.mapError(...)` if needed by the bridge implementation.
5. Ensure `Task.async(...)` remains source-compatible, likely implemented through the new primitive.
6. Update `Task.runAsync(...)` to preserve failure semantics cleanly.

Acceptance criteria:

- Async registration occurs only when the task is interpreted.
- Success, failure, and cancellation race safely with one terminal result.
- No Reactor bridge implementation needs `block()`, `blockFirst()`, or `blockLast()`.
- Existing `TaskTest` behavior remains compatible.

### Epic 2: Cancellation, Fiber, And Outcome

Goal: make cancellation operational enough to dispose Reactor subscriptions and clean up resources.

Tasks:

1. Extend `CancellationToken` with idempotent cancellation and `onCancel(Runnable)` listener registration.
2. Add `Outcome<E, A>` with `Succeeded`, `Failed`, and `Cancelled`.
3. Add `Fiber<E, A>` with `join()` and `cancel()`.
4. Add `Task.start()` or equivalent to create fibers.
5. Rework `Task.race(...)` so losing tasks are cancelled.
6. Add tests for listener execution, idempotent cancellation, cancellation-before-registration, and race cleanup.

Acceptance criteria:

- A cancellation token can actively dispose registered resources.
- `Task.race(...)` cancels losers.
- Cancellation is distinguishable from ordinary failure in `Outcome`.
- Repeated cancellation is safe.

### Epic 3: Resource And Deferred Primitives

Goal: add reusable primitives needed for safe interop and stream bridging.

Tasks:

1. Add `Resource<A>` with `make`, `use`, `map`, and `flatMap`.
2. Implement reverse-order release for nested resources.
3. Ensure release runs on success, failure, and cancellation.
4. Add `Deferred<A>` with `get`, `complete`, and `tryGet`.
5. Define and test completion semantics: exactly one winning completion; later completion attempts are deterministic.
6. Use these primitives internally where they simplify cancellation and bridge code.

Acceptance criteria:

- `Resource.use(...)` always releases acquired resources.
- Nested resources release in reverse acquisition order.
- `Deferred.get()` waits asynchronously without blocking the caller thread.
- Cancelled waiters do not corrupt the deferred value.

### Epic 4: AsyncStream Public Wrapper

Goal: provide a pleasant functional-java streaming type for Reactor Flux use cases without exposing HKT witnesses.

Tasks:

1. Add `AsyncStream<A>` as a thin wrapper over `Stream<Task.µ, A>`.
2. Provide core operations: `empty`, `emit`, `fromList`, `map`, `flatMap`, `concat`, `foldl`, `toList`, `onFinalize`.
3. Add `unfoldTask(...)` for pull-based async stream construction.
4. Review `Stream.parEvalMap(...)` and either fix bounded concurrency semantics or document/defer it.
5. Add tests mirroring existing `StreamTest` plus async finalization scenarios.

Acceptance criteria:

- User-facing streaming APIs can return `AsyncStream<A>`.
- Existing generic `Stream<F, A>` remains intact.
- `AsyncStream` does not import Reactor.
- Stream finalizers run on normal completion and failure; cancellation behavior is covered once bridge cancellation exists.

### Epic 5: Reactor Bridge Utility Package

Goal: expose sanctioned utility conversions for framework boundaries while keeping primitives Reactor-free.

Tasks:

1. Add Reactor dependencies to the same artifact:
   - `reactor-core` for main code.
   - `reactor-test` for tests.
2. Add an exported bridge package such as `io.github.senthilganeshs.fj.reactor`.
3. Implement `Mono<A> -> Task<A>`.
4. Implement empty `Mono<A> -> Task<Maybe<A>>`.
5. Implement `Mono<A> -> TaskEither<E, A>` with caller-provided `Throwable -> E` mapper.
6. Implement `Task<A> -> Mono<A>`.
7. Implement `Task<Maybe<A>> -> Mono<A>` where `Nothing` completes empty.
8. Implement `TaskEither<E, A> -> Mono<A>` with caller-provided `E -> Throwable` mapper.
9. Implement `Flux<A> -> AsyncStream<A>` using a demand-aware, cancellation-aware bridge.
10. Implement `AsyncStream<A> -> Flux<A>` without eager collection.
11. Ensure all bridge methods avoid blocking.

Acceptance criteria:

- Reactor imports appear only in the bridge package and tests.
- Spring WebFlux can call utility methods to return `Mono` or `Flux`.
- Functional primitives do not gain Reactor-specific instance methods.
- Cancelling a `Mono`/`Flux` subscription cancels the underlying `Task`/`AsyncStream`.
- Cancelling a functional-java task/stream disposes Reactor subscriptions.

### Epic 6: Reader Context And Error Mapping Patterns

Goal: provide clear guidance and helpers for replacing Reactor Context and error operators.

Tasks:

1. Document `ReaderTaskEither<R, E, A>` as the preferred public replacement for subscriber context in single-result APIs.
2. Document `Reader<R, AsyncStream<A>>` as the default streaming context pattern.
3. Add small helper examples showing correlation ID/auth/tenant context passed through `ReaderTaskEither`.
4. Add bridge examples mapping `Throwable` to domain errors and domain errors back to framework exceptions.
5. Avoid adding a dedicated context-local primitive in this phase.

Acceptance criteria:

- Examples show Reactor Context confined to framework/bridge edges.
- Domain services expose functional-java types, not Reactor types.
- Typed error mapping is explicit and testable.

### Epic 7: Tests, Laws, And Benchmarks

Goal: verify behavior at the public primitive layer and the Reactor bridge layer.

Tasks:

1. Add unit tests for `Task.defer`, `asyncCancelable`, recovery helpers, cancellation listeners, `Fiber`, `Outcome`, `Resource`, `Deferred`, and `AsyncStream`.
2. Add bridge tests with `reactor-test` and `StepVerifier`.
3. Test Mono value, empty, failure, cancellation, and typed error conversion.
4. Test Flux value flow, completion, failure, cancellation, and no eager collection.
5. Add tests that run on Reactor non-blocking schedulers to catch accidental blocking.
6. Add property/law-style tests where practical for `AsyncStream` map/flatMap and `Resource` release invariants.
7. Add JMH benchmarks for common conversions:
   - `Mono -> Task`
   - `Task -> Mono`
   - `Flux -> AsyncStream`
   - `AsyncStream -> Flux`

Acceptance criteria:

- `./gradlew --no-daemon test` passes.
- No bridge test requires blocking Reactor calls.
- Cancellation and release paths are explicitly tested.
- Benchmarks compile and can be run through the existing JMH setup.

### Epic 8: Documentation And Examples

Goal: make the intended integration style clear to users.

Tasks:

1. Update `README.md` with a “Reactive Interop” section.
2. Show preferred public API signatures using `Task`, `TaskEither`, `ReaderTaskEither`, and `AsyncStream`.
3. Show Spring/WebFlux edge examples using the bridge utility package.
4. Document the rule: primitives stay Reactor-free; bridge utilities are for framework boundaries.
5. Document `Mono.empty()` mapping to `Maybe.nothing()`.
6. Document cancellation and resource safety expectations.
7. Add javadocs for all new public primitives and bridge utilities.

Acceptance criteria:

- Users can understand when to use functional primitives versus Reactor bridge utilities.
- Examples do not encourage domain APIs returning `Mono` or `Flux`.
- The bridge package is clearly marked as interop or boundary support.

## Test Strategy

- Keep the first verification step focused on the planning artifact itself.
- Add unit tests alongside each primitive introduced in later epics.
- Use `reactor-test` and `StepVerifier` only in bridge-layer tests.
- Add cancellation, release, and non-blocking scheduler coverage before broadening the surface area.
- Use property-style tests for laws and invariants where the behavior is associative or resource-sensitive.
- Add benchmarks after the core behavior is stable enough to measure.

## Assumptions

- Reactor support will be added to the same Gradle artifact.
- Reactor types may appear in a dedicated bridge utility package, but not as methods directly on core primitives.
- Core primitives remain implementation-neutral and reusable without Reactor-specific vocabulary.
- `AsyncStream<A>` is the preferred public replacement for `Flux<A>`.
- Hot stream abstractions such as `Topic<A>` or `Hub<A>` are out of scope for the first implementation pass.
- A dedicated context-local abstraction is out of scope unless `ReaderTaskEither` proves insufficient.
- Dependency versions should be verified at implementation time against current stable Reactor Core and Reactor Test releases.

## Acceptance Criteria For The Plan

- The epic order is explicit and stable.
- Public API boundaries are documented before implementation.
- The planning artifact is the reference point for future implementation work.
- The document is specific enough that later code changes can be checked against it.
