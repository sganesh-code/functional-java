![build](https://github.com/sganesh-code/functional-java/actions/workflows/ci.yml/badge.svg?branch=master)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.sganesh-code/functional-java.svg)](https://central.sonatype.com/artifact/io.github.sganesh-code/functional-java)

# functional-java 2.0

`functional-java` is a functional data and effects library for Java. It reduces boilerplate around data transformation, typed errors, async flows, and immutable updates while still fitting naturally into object-oriented codebases.

The design goal is simple: use functional concepts for the places where Java gets verbose or fragile, and use OOP for objects, boundaries, and message-oriented design. That combination makes code more readable, extensible, and maintainable.

## Why It Exists

Java's standard library is solid, but it often pushes boilerplate into the exact places where you want clarity:
- optional values,
- recoverable errors,
- async composition,
- deep immutable updates,
- reusable transformations over persistent data.

`functional-java` gives those patterns first-class APIs instead of ad hoc helpers.

## What Makes It Different

| Problem | Standard Java | `functional-java` |
| --- | --- | --- |
| Missing values | `null` checks or `Optional` chains | `Maybe` with rich combinators |
| Recoverable failures | exceptions or status objects | `Either` and `Validation` |
| Async composition | `CompletableFuture` chains | `Task`, `TaskEither`, `ReaderTaskEither` |
| Immutable collections | streams + collectors | persistent `List`, `Vector`, `Set`, `HashMap`, `Queue`, `Deque` |
| Deep updates | constructors/builders/manual copying | optics: `Lens`, `Prism`, `Iso`, `Traversal` |
| Shared behavior | repeated utility code | `Functor`, `Applicative`, `Monad`, `Traversable`, `Monoid`, `Eq`, `Ord`, `Hashable` |
| State machines | ad hoc orchestration | effectful `Automaton` engine |
| Parsing/encoding | hand-written glue | `Parser`, `JsonParser`, `Codec`, `JsonValue` |
| Verification | ad hoc tests | `Gen`, `Property`, `Shrink`, and law tests |

## Core Library Areas

### Persistent Data Structures

- `Maybe<T>` for optional values.
- `Either<L, R>` for typed branching and errors.
- `Validation<E, T>` for accumulated validation failures.
- `These<L, R>` for left/right/both semantics.
- `List<T>`, `Vector<T>`, `Set<T>`, `HashMap<K, V>`, `Map<K, V>`, `Queue<T>`, `Deque<T>`, `PriorityQueue<T>`.
- `Graph<T>`, `RoseTree<T>`, `LazyList<T>`, `Array<T>`, `Stack<T>`, `NonEmptyList<T>`, `Tuple<A, B>`, `Identity<T>`, `Const<A, B>`, `Writer<W, A>`.

### Effects And Context

- `IO<A>` for side effects.
- `Task<A>` for async work.
- `TaskEither<E, A>` for async work that can fail.
- `Reader<R, A>` for dependency injection.
- `State<S, A>` for explicit state threading.
- `ReaderTaskEither<R, E, A>` for environment + async + failure in one abstraction.

### Abstractions

- `Functor`, `Applicative`, `Monad`, `Traversable`.
- `Semigroup`, `Monoid`, `Eq`, `Ord`, `Hashable`, `Bifunctor`, `Profunctor`.
- HKT encoding via `Higher` so generic algorithms can be reused across data types.

### Parsing, Codecs, And Optics

- `Parser` and `JsonParser` for parser combinators.
- `JsonValue` and `JsonCodec` for structured JSON data.
- `Codec`, `Encoder`, and `Decoder` for typed serialization.
- `Lens`, `Prism`, `Iso`, `Traversal`, `AffineTraversal` for deep immutable updates.

### Testing And Benchmarks

- `Gen`, `Property`, `Shrink` for property-based testing.
- `FunctorLaws`, `MonadLaws`, `MonoidLaws` for algebraic law checks.
- JMH support for performance benchmarking.

## Core Triad

Most collection behavior derives from three methods:
- `empty()`
- `build(T)`
- `foldl(seed, fn)`

If you implement those correctly, the library gives you a large set of derived operations for free: `map`, `flatMap`, `filter`, `concat`, `take`, `drop`, `reverse`, `zipWith`, `sequence`, `foldMap`, `chunk`, and more.

That is the main leverage point of the library: define the storage semantics once, then reuse the shared abstraction layer everywhere else.

## Getting Started

### 1. Add A New Data Structure

If you want to introduce a new collection type, implement the triad first and let the shared APIs do the rest.

```java
final class EventWindow<T> implements Collection<T> {
    private final int capacity;
    private final List<T> items;

    EventWindow(int capacity, List<T> items) {
        this.capacity = capacity;
        this.items = items;
    }

    @Override
    public <R> Collection<R> empty() {
        return new EventWindow<>(capacity, List.nil());
    }

    @Override
    public Collection<T> build(T input) {
        List<T> next = (List<T>) items.build(input);
        return new EventWindow<>(capacity, next.length() > capacity ? next.drop(next.length() - capacity) : next);
    }

    @Override
    public <R> R foldl(R seed, BiFunction<R, T, R> fn) {
        return items.foldl(seed, fn);
    }
}
```

Once a type implements `Collection<T>`, it automatically participates in the rest of the ecosystem:
- `map`, `flatMap`, `filter`, `take`, `drop`, `zipWith`, `traverse`, `sequence`
- optics like `Collection.eachP()` and `Collection.at(...)`
- interop with `Maybe`, `Either`, `Validation`, `Task`, and `List`

That means a new data structure does not need a custom utility layer before it becomes useful. In this example the structure is not just a wrapper around `List`; it also enforces a bounded-history policy while still inheriting the shared API surface.

### 2. Interoperate With Existing Types

Because the abstraction is shared, a custom collection can feed into existing helpers and get results back as a generic `Collection`.

```java
EventWindow<String> raw = new EventWindow<>(3, List.of("  Ada ", "  Bob ", " "));

// Generic transformations return Collection<T>
Collection<String> cleaned = raw
    .filter(name -> !name.isBlank())
    .map(String::trim)
    .map(String::toUpperCase);

// You can still access generic features like optics
Maybe<String> first = cleaned.atIndex(0);
```

### 3. Type Narrowing

If you need implementation-specific features after a transformation, use the static `from()` narrowing methods provided by each data structure.

```java
Collection<Integer> result = List.of(1, 2, 3).map(i -> i * 2);

// Narrow back to List to use List-specific features like tail()
List<Integer> list = List.from(result);
Maybe<List<Integer>> rest = list.tail();

// Narrowing works for monadic types too
Maybe<Integer> maybe = Maybe.from(list.filter(i -> i > 10));
```

### 4. Solve A Real Problem: Real-time Metrics Aggregator

Suppose you are building a monitoring dashboard. You need to fetch configurations for multiple sensors in parallel, handle potential network failures, and then process a real-time stream of readings using those configurations—all while maintaining state (like rolling averages) and triggering alerts when thresholds are breached.

With `functional-java`, you can orchestrate this complex, stateful workflow using a unified set of primitives.

```java
// 1. Fetch configurations for multiple sensors in parallel
List<String> sensorIds = List.of("temp-1", "press-2", "hum-3");
TaskEither<String, List<Config>> configurations = 
    TaskEither.parTraverse(sensorIds, id -> configApi.fetch(id));

// 2. Define the "Brain" (Pure logic)
// Tracks rolling averages and emits alerts if thresholds are breached
Machine<Stats, Reading, Alert> brain = (stats, reading) -> {
    Stats next = stats.update(reading.value());
    return next.isCritical() 
        ? new Machine.Result<>(next, List.of(new Alert(reading.sensorId())))
        : new Machine.Result<>(next, List.nil());
};

// 3. Define the "Hands" (Side effects)
Interpreter<Task.µ, Alert, Reading> hands = alert -> 
    alertService.send(alert).map(__ -> List.nil());

// 4. Orchestrate the simultaneous streaming loop
Task<Void> pipeline = configurations.task().flatMap(either -> 
    either.either(
        error -> Task.fail(new RuntimeException("Fetch failed: " + error)),
        activeConfigs -> sensorStream.readings()
            // Filter readings to only those with an active configuration
            .filter(r -> activeConfigs.any(c -> c.sensorId().equals(r.sensorId())))
            // Run the state machine orchestrator for each reading concurrently
            .parEvalMap(4, reading -> {
                Automaton<Task.µ, String, Stats, Reading, Alert> engine = 
                    Automaton.ofTask(brain, hands, statsRepo);
                return Task.narrowK(engine.run(reading.sensorId(), reading));
            })
            .forEach(finalStats -> System.out.println("Updated Stats: " + finalStats))
    )
);

pipeline.runAsync(res -> System.out.println("Aggregator running..."));
```

This gives you:
- **Simultaneous Async Work**: `parTraverse` initiates all configuration fetches concurrently.
- **Complex Orchestration**: `Automaton` keeps logic (Machine), effects (Interpreter), and memory (Repository) decoupled and robust.
- **Concurrent Streaming**: `parEvalMap` processes stream elements in parallel while maintaining resource safety and order.
- **Declarative Pipeline**: The entire workflow—from API calls to stateful stream processing—is expressed as a single, readable chain of data and effects.

## Idiomatic Usage

### Webhook Payloads

When a payload is deeply nested, standard Java quickly turns into null checks and temporary variables. `JsonValue` optics keep the access path explicit and immutable.

```java
JsonValue root = JsonParser.parser().parse(body).orElse(new JsonValue.JsonNull());

var zipT = JsonValue.path("customer", "address")
    .compose(JsonValue.stringAt("zip"));

String zip = zipT.getMaybe(root).orElse("unknown");
JsonValue normalized = zipT.set("94105", root);
```

### Request Validation

If you validate a request in plain Java, you usually stop at the first failure or write a lot of branching. `Validation` lets you accumulate all field problems in one pass.

```java
record Registration(String email, int age) {}

Validation<List<String>, Registration> validated =
    validateEmail(email)
        .liftA2(
            (validEmail, validAge) -> new Registration(validEmail, validAge),
            validateAge(age),
            List.monoid()
        );
```

### Async Service Workflows

`CompletableFuture` is fine for a single async hop, but service code usually needs typed failures and a readable chain. `TaskEither` keeps success and failure explicit.

```java
TaskEither<String, Invoice> invoiceFlow =
    fetchOrder(orderId)
        .flatMap(order -> fetchTax(order.zipCode()))
        .flatMap(tax -> TaskEither.right(new Invoice(orderId, tax)));
```

Use `Task.zip` or `Task.parZip` when two independent effects can run concurrently:

```java
Task<Tuple<Customer, Account>> loaded =
    Task.zip(loadCustomer(customerId), loadAccount(accountId));

Task<Void> auditWrites =
    Task.whenAll(List.of(writeAuditEvent(event), publishMetric(metric)));
```

For Java async APIs, keep construction lazy with `fromCompletionStage`:

```java
Task<HttpResponse<String>> response =
    Task.fromCompletionStage(() -> httpClient.sendAsync(request, BodyHandlers.ofString()))
        .timeout(Duration.ofSeconds(2));
```

Typed failure workflows have matching collection helpers:

```java
TaskEither<DomainError, List<Customer>> customers =
    TaskEither.parTraverse(customerIds, id -> findCustomer(id));
```

### Environment-Aware Composition

When a workflow depends on configuration, repositories, and feature flags, `ReaderTaskEither` makes those dependencies explicit instead of hiding them in globals or service locators.

```java
ReaderTaskEither<AppEnv, String, String> program =
    ReaderTaskEither.ask().flatMap(env ->
        env.featureEnabled("risk-score")
            ? ReaderTaskEither.right("enabled")
            : ReaderTaskEither.left("disabled")
    );
```

### Reactive Interop

Reactor support lives in `io.github.senthilganeshs.fj.reactor`. The core primitives stay Reactor-free, and the bridge handles framework boundary conversions.

```java
Task<String> task = ReactorInterop.monoToTask(mono);
Mono<String> mono = ReactorInterop.taskToMono(task);
AsyncStream<String> stream = ReactorInterop.fluxToAsyncStream(flux);
Flux<String> flux = ReactorInterop.asyncStreamToFlux(stream);
```

`Mono.empty()` maps naturally through the `Task<Maybe<A>>` bridge:

```java
Task<Maybe<String>> maybeTask = ReactorInterop.monoToMaybeTask(Mono.empty());
```

At the WebFlux edge, return Reactor types from controllers and keep the domain service in functional-java types:

```java
@RestController
class InvoiceController {
    private final InvoiceService service;

    @GetMapping("/invoices/{id}")
    Mono<InvoiceDto> getInvoice(@PathVariable String id) {
        return ReactorInterop.taskEitherToMono(
            service.findInvoice(id),
            DomainError::toHttpException
        );
    }
}
```

Cancellation and release stay explicit. Cancelling a Reactor subscription cancels the underlying task or stream, and `Resource.use(...)` still releases on success, failure, and cancellation.

Release `2.1.0` introduces the `Automaton` engine for effectful state machines, providing a robust orchestrator for complex processes like AI agents and background workers with built-in checkpointing and sequential feedback loops.

Release `2.0.21` republishes the Reactor-familiar `Task` and `TaskEither` orchestration helpers from `2.0.20` and includes the ReactorInterop test stabilization needed to keep CI and the Maven Central publish flow deterministic. Release `2.0.19` added the Reactor bridge, `AsyncStream`, `Resource`, `Deferred`, `Fiber`, and `Outcome` support introduced by the epic work, along with the cancellation and resource-safety behavior that makes those pieces usable at the framework boundary.

#### Single-Result Example

Keep the service in functional-java and adapt it only at the controller boundary:

```java
record CustomerId(String value) {}
record CustomerDto(String id, String name) {}

interface CustomerService {
    TaskEither<DomainError, CustomerDto> findCustomer(CustomerId id);
}

@RestController
class CustomerController {
    private final CustomerService service;

    @GetMapping("/customers/{id}")
    Mono<CustomerDto> getCustomer(@PathVariable String id) {
        return ReactorInterop.taskEitherToMono(
            service.findCustomer(new CustomerId(id)),
            DomainError::toHttpException
        );
    }
}
```

This keeps domain failures typed in the service layer and maps them to HTTP exceptions only when Reactor is required.

#### Streaming Example

Use `AsyncStream` for domain streaming and convert to `Flux` at the edge:

```java
record Event(String id, String type) {}

interface EventService {
    AsyncStream<Event> eventsForTenant(String tenantId);
}

@RestController
class EventController {
    private final EventService service;

    @GetMapping(value = "/tenants/{tenantId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<Event> streamEvents(@PathVariable String tenantId) {
        return ReactorInterop.asyncStreamToFlux(service.eventsForTenant(tenantId));
    }
}
```

If you need to start from Reactor instead, use the inverse bridge:

```java
Task<String> value = ReactorInterop.monoToTask(Mono.just("ok"));
Task<Maybe<String>> maybeValue = ReactorInterop.monoToMaybeTask(Mono.empty());
AsyncStream<Integer> stream = ReactorInterop.fluxToAsyncStream(Flux.range(1, 3));
```

### Context And Error Mapping

For single-result workflows, `ReaderTaskEither<R, E, A>` is the preferred way to carry request context such as correlation IDs, auth state, or tenant information.

```java
record RequestContext(String correlationId, String tenantId, String userId) {}

ReaderTaskEither<RequestContext, DomainError, Invoice> flow =
    ReaderTaskEither.ask().flatMap(ctx ->
        loadInvoice(ctx.tenantId(), ctx.userId())
            .mapError(err -> new DomainError("invoice", err))
    );
```

For streaming workflows, use `Reader<R, AsyncStream<A>>` so the stream remains context-aware without introducing a separate context-local primitive.

```java
record StreamContext(String tenantId) {}

Reader<StreamContext, AsyncStream<Event>> events =
    Reader.ask().map(ctx ->
        fetchEvents(ctx.tenantId()).map(Event::normalize)
    );
```

At the framework edge, map framework exceptions into domain errors and domain errors back to framework exceptions explicitly:

```java
TaskEither<DomainError, Customer> service = loadCustomer(customerId);

Mono<Customer> endpoint =
    ReactorInterop.taskEitherToMono(service, DomainError::toHttpException);

TaskEither<DomainError, Customer> back =
    ReactorInterop.monoToTaskEither(endpoint, DomainError::fromThrowable);
```

This keeps Reactor Context, HTTP exceptions, and subscription mechanics at the boundary. Domain services expose `ReaderTaskEither`, `TaskEither`, `Reader`, `Task`, and `AsyncStream` instead of Reactor types.

### Resource Safety With `bracket`

If you use `try/catch/finally` for resource management, the cleanup logic usually gets buried under control flow. `Task.bracket` keeps acquisition, use, and release in one composable shape.

```java
Task<Path> stagedReport =
    Task.bracket(
        Task.of(() -> Files.createTempFile("report-", ".csv")),
        path -> Task.of(() -> {
            Files.writeString(path, "id,name\n1,Ada\n2,Bob\n");
            return path;
        }),
        path -> Task.of(() -> {
            Files.deleteIfExists(path);
            return null;
        })
    );
```

This is especially useful when the `use` step can fail. The release step still runs, which keeps cleanup logic explicit and local to the resource.

### Effectful State Machines: `Automaton`

Complex processes—like background workers, UI orchestrators, or AI agents—often follow a loop: receive input, consult logic, update state, run side-effects, and repeat. The `Automaton` engine abstracts this orchestration, ensuring that state is persisted (checkpointed) before any side-effects are triggered.

It consists of three parts:
- **Machine**: The "Brain" (Pure logic mapping `State + Input` to `NextState + Commands`).
- **Interpreter**: The "Hands" (Effectful execution of `Commands` yielding new `Inputs`).
- **Repository**: The "Memory" (Persistent storage for `State`).

```java
// 1. Define your logic (Pure)
Machine<Integer, String, String> counter = (state, input) -> 
    new Machine.Result<>(state + 1, List.nil());

// 2. Define your effects (Side-effectful)
Interpreter<Task.µ, String, String> worker = cmd -> Task.succeed(List.nil());

// 3. Define your persistence
Repository<Task.µ, String, Integer> repo = new MyDatabaseRepo();

// 4. Stitch it together
Automaton<Task.µ, String, Integer, String, String> engine = 
    Automaton.ofTask(counter, worker, repo);

// 5. Run the engine
Task<Integer> finalState = Task.narrowK(engine.run("user-123", "increment"));
```

The engine handles the recursive feedback loop automatically: if the interpreter yields new inputs, they are fed back into the machine sequentially until the process settles.

### Patterns Worth Knowing

The README focuses on the common entry points, but these are also important:
- `State` for threaded state transitions without mutation.
- `Writer` for audit logs, traces, and accumulated output.
- `These` for partial success when you need left, right, or both.
- `NonEmptyList` when a collection must not be empty by construction.
- `LazyList` for deferred or potentially unbounded sequences.
- `Parser`, `Codec`, and `JsonCodec` for parsing and typed data conversion.
- `Eq`, `Ord`, and `Hashable` for reusable comparison and hashing logic.
- `Property`, `Shrink`, and the law helpers for stronger correctness checks than example-based tests alone.

### JSON Codec

When you need a typed bridge between a domain value and JSON, `JsonCodec` keeps encoding and decoding close together.

```java
record Customer(String id, String email) {}

JsonCodec<Customer> customerCodec = new JsonCodec<>() {
    @Override
    public JsonValue encode(Customer value) {
        return new JsonValue.JsonObject(HashMap.<String, JsonValue>nil()
            .put("id", JsonValue.of(value.id()))
            .put("email", JsonValue.of(value.email())));
    }

    @Override
    public Either<String, Customer> decode(JsonValue json) {
        JsonValue.JsonObject obj = JsonValue.objectP().getMaybe(json).orElse(null);
        if (obj == null) return Either.left("Expected JSON object");
        String id = JsonValue.stringAt("id").getMaybe(obj).orElse("");
        String email = JsonValue.stringAt("email").getMaybe(obj).orElse("");
        return id.isBlank() || email.isBlank()
            ? Either.left("Missing id or email")
            : Either.right(new Customer(id, email));
    }
};
```

That gives you a typed boundary for API payloads without scattering serialization logic across the codebase.

### Writer And State

`Writer` is useful when you want the result and the trace together, and `State` is useful when you want to thread evolving state without mutation.

```java
Monoid<String> logs = Monoid.STRING_CONCAT;

Writer<String, Integer> writer = new Writer<>(10, "start;");
Writer<String, Integer> next = writer.flatMap(n -> new Writer<>(n + 5, "added5;"), logs);
```

```java
State<Integer, String> counter =
    State.<Integer>get()
        .flatMap(n -> State.modify(i -> i + 1)
            .flatMap(__ -> State.pure("previous=" + n)));
```

These are small abstractions, but they remove a lot of boilerplate in workflows that need traceability or explicit state threading.

### Deep Immutable Updates

Copying nested objects by hand is where immutable Java code becomes noisy. Optics keep the intent focused on the field you want to change.

```java
record Address(String city, String zip) {}
record User(String id, Address address) {}

Lens<User, Address> addressL = RecordOptics.of(User.class, User::address);
Lens<Address, String> cityL = RecordOptics.of(Address.class, Address::city);

User updated = addressL.compose(cityL).set("San Francisco", user);
```

### Persistent Collection Transformations

You can transform whole collections generically without introducing temporary mutable state or collectors.

```java
Collection<String> names = List.of("a", "b", "c")
    .filter(name -> !name.isBlank())
    .map(String::toUpperCase);
```

## Standard Library Vs Library Style

```java
// Traditional Java
User user = repository.find(id);
String zip = "unknown";
if (user != null && user.getAddress() != null && user.getAddress().getZip() != null) {
    zip = user.getAddress().getZip();
}
```

```java
// functional-java
String zip = Maybe.of(repository.find(id))
    .flatMap(u -> Maybe.of(u.getAddress()))
    .map(Address::getZip)
    .orElse("unknown");
```

```java
// Traditional copying
User updated = new User(
    user.getId(),
    user.getName(),
    new Address(user.getAddress().getStreet(), "New York", user.getAddress().getZip())
);
```

```java
// Optics
Lens<User, Address> addressL = RecordOptics.of(User.class, User::address);
Lens<Address, String> cityL = RecordOptics.of(Address.class, Address::city);

User updated = addressL.compose(cityL)
    .set("New York", user);
```

```java
// Transform every string in a persistent list without extra collectors or mutation
List<String> rawNames = List.of(" Ada ", "Bob ", "  Carla");
Collection<String> cleaned = Collection.eachP().modify(rawNames, String::trim);
```

## Performance Notes

- Core engines favor iterative implementations where practical.
- Persistent updates preserve previous versions and reduce accidental mutation.
- The API favors derived combinators so you write less plumbing and more intent.
- JMH support is included so behavior can be measured, not guessed.

## Property-Based Testing

Example-based unit tests are good for known scenarios. `Gen`, `Property`, and `Shrink` help when you want broader coverage across many random shapes of input.

Generators produce realistic values:
- `Gen.choose(min, max)` for bounded integers.
- `Gen.string(length)` for synthetic text.
- `Gen.list(gen, maxLength)` for collections of random values.
- `Gen.oneOf(...)` and `Gen.frequency(...)` when test inputs need realistic mixtures.

Shrinkers make failures actionable by reducing a failing input to a smaller counter-example:
- `Shrink.integer()` moves toward simpler numbers.
- `Shrink.string()` reduces noisy strings to the empty or minimal case.
- `Shrink.list(...)` reduces list-heavy failures to a simpler collection.

This matters because the failure you want to debug is rarely the first random value that breaks the property. You want the smallest readable case that still fails.

### Why It Is More Robust

A normal unit test checks one or two examples. A property can check dozens or hundreds of generated values, which makes it much better at catching:
- edge cases around empty and singleton collections,
- off-by-one errors,
- ordering bugs,
- broken invariants after transformations,
- regressions that only show up on unusual data shapes.

### Realistic Examples

#### Graph Invariants

If you build graph algorithms, it is not enough to test one hand-written graph. You want to check that a structural invariant holds across many generated graphs.

```java
Gen<Graph<Integer>> graphGen = Gen.choose(1, 20).flatMap(n -> {
    Graph<Integer> g = Graph.nil();
    for (int i = 0; i < n; i++) g = g.addVertex(i);
    for (int i = 0; i < n; i++) {
        for (int j = i + 1; j < n; j++) {
            if (Math.random() > 0.7) g = g.addEdge(i, j);
        }
    }
    return Gen.pure(g);
});

Property.forAll(graphGen, g -> {
    Maybe<List<Integer>> sorted = g.topologicalSort();
    if (sorted.isNothing()) return true;

    List<Integer> order = sorted.orElse(List.nil());
    return g.vertices().all(u ->
        g.neighbors(u).all(v ->
            order.indexOf(u).orElse(-1) < order.indexOf(v).orElse(-1)
        )
    );
}).assertTrue(50);
```

That test covers many graph shapes instead of one or two manual examples.

#### Collection Laws

You can also verify that a collection preserves expected behavior after transformations.

```java
Property.forAll(
    Gen.list(Gen.choose(-100, 100), 20),
    Shrink.list(Shrink.integer()),
    values -> {
        List<Integer> xs = List.from(values);
        return xs.reverse().reverse().equals(xs);
    }
).assertTrue(100);
```

If that property fails, the shrinker helps you get the smallest failing list instead of a huge random one.

#### Input Validation

Generators are also useful for validation rules that are easy to get wrong at the edges.

```java
Property.forAll(
    Gen.string(12),
    Shrink.string(),
    name -> !name.contains(" ")
).assertTrue(100);
```

That style is useful when you want to test a normalization rule, a parser, or a validator across many inputs instead of hand-picking a few cases.

## Installation

Version: `2.1.2`

### Maven

```xml
<dependency>
    <groupId>io.github.sganesh-code</groupId>
    <artifactId>functional-java</artifactId>
    <version>2.1.2</version>
</dependency>
```

### Gradle

```gradle
implementation 'io.github.sganesh-code:functional-java:2.1.2'
```

## Contributing

- Keep data structures persistent.
- Build new collection behavior from the core triad.
- Prefer readable functional code for data and effects.
- Use OOP boundaries where objects and message passing make the design clearer.
- Add happy-path, error-path, and edge-case tests for API changes.

## License

Licensed under the GNU General Public License v3.0. See [LICENSE](LICENSE).
