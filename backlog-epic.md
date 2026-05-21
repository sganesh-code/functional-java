# Reactor Epic Backlog Resolution

This file is the post-validation record for the gaps that were originally found
while comparing `epic-planning.md` against the implementation.

Status as of 2026-05-21: all backlog items have been addressed in code or docs,
and the remaining work is verification-only.

## Verification

- `./gradlew --no-daemon test` passes
- JMH discovery was verified from the existing Gradle JMH setup

## Resolved Backlog 1: Hide HKT Witnesses From `AsyncStream`

Original concern:

- `AsyncStream` exposed `public Stream<Task.µ, A> stream()`, which leaked the
  `Task.µ` witness through the public API.

Resolution:

- `AsyncStream` now exposes `step()` as the public stepping API.
- The internal `Stream<Task.µ, A>` remains encapsulated.
- `ReactorInterop.asyncStreamToFlux(...)` now drives the stream through
  `AsyncStream.step()`.

Result:

- The public `AsyncStream` API no longer exposes HKT witnesses.

## Resolved Backlog 2: Document `Stream.parEvalMap(...)`

Original concern:

- `parEvalMap(...)` accepted a `parallelism` parameter but did not implement
  bounded concurrency.

Resolution:

- The method javadoc now states that bounded parallelism is deferred.
- The current behavior is documented as preserving ordering only.

Result:

- The method is no longer misleading.

## Resolved Backlog 3: Complete Reactor Bridge Failure And Cancellation Tests

Original concern:

- The bridge tests did not cover all failure, empty, completion, and cancellation
  paths in both directions.

Resolution:

- Added coverage for:
  - `Mono.error(...) -> Task`
  - cancelling `monoToTask(...)`
  - `Mono.empty(...) -> TaskEither(...)`
  - `Task<Maybe<A>> -> Mono` empty completion
  - `Flux.error(...) -> AsyncStream`
  - cancelling consumers of `fluxToAsyncStream(...)`
  - `AsyncStream -> Flux` normal completion
  - `AsyncStream -> Flux` cancellation and finalization

Result:

- Reactor interop behavior is covered across success, empty, failure, and
  cancellation cases.

## Resolved Backlog 4: Verify Benchmark Task Through Existing JMH Setup

Original concern:

- `ReactorInteropBenchmark` lived under `src/test/java`, and discovery through
  the JMH plugin had not been proven.

Resolution:

- The existing JMH plugin setup discovers benchmarks from test sources.
- `ReactorInteropBenchmark` is included in that discovery path.

Result:

- The benchmark is wired into the current JMH setup without moving it into
  production sources.

## Resolved Backlog 5: Optional Documentation Tightening

Original concern:

- Several new public primitives had minimal method-level documentation.

Resolution:

- Added method-level javadocs to the new runtime and bridge surface, including
  `Task`, `Deferred`, `Resource`, `AsyncStream`, and `ReactorInterop`.

Result:

- The public API is documented at the method level where the new behavior is
  easiest to misunderstand.

## Final Status

No open backlog items remain from the Reactor validation pass. The implementation
matches the plan closely enough to treat the epic as complete, with only normal
ongoing maintenance left.
