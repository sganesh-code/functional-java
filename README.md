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

Because the abstraction is shared, a custom collection can feed into existing helpers and get results back in their native types.

```java
EventWindow<String> raw = new EventWindow<>(3, List.of("  Ada ", "  Bob ", " "));

List<String> cleaned = raw
    .filter(name -> !name.isBlank())
    .map(String::trim)
    .map(String::toUpperCase);

Maybe<String> first = cleaned.atIndex(0);
```

The same idea works for sequencing effects:

```java
List<Task<String>> lookups = List.of(
    Task.of(() -> "alice"),
    Task.of(() -> "bob")
);

Task<Collection<String>> resolved = Task.sequence(lookups);
```

### 3. Solve A Real Problem With Multiple Structures

Suppose you receive a webhook payload, need to validate fields, normalize a few nested values, and prepare the request for downstream processing. The standard Java version usually becomes a mix of null checks, temporary objects, and exception handling.

With `functional-java`, you can split the problem into focused pieces:

```java
JsonValue payload = JsonParser.parser().parse(body).orElse(new JsonValue.JsonNull());

var emailT = JsonValue.path("customer", "contact").compose(JsonValue.stringAt("email"));
var zipT = JsonValue.path("customer", "address").compose(JsonValue.stringAt("zip"));
record Request(String email, String zip) {}

Maybe<String> emailM = emailT.getMaybe(payload);
Maybe<String> zipM = zipT.getMaybe(payload);

Validation<List<String>, String> emailV =
    emailM.isSome()
        ? Validation.valid(emailM.orElse(""))
        : Validation.invalid(List.of("customer.contact.email is missing"));

Validation<List<String>, String> zipV =
    zipM.isSome()
        ? Validation.valid(zipM.orElse(""))
        : Validation.invalid(List.of("customer.address.zip is missing"));

Validation<List<String>, Request> requestV =
    emailV.liftA2(
        (email, zip) -> new Request(email, zip),
        zipV,
        List.monoid()
    );
```

That gives you:
- explicit handling for missing payload data,
- accumulated validation errors,
- immutable nested updates through optics,
- a clean handoff into downstream async work or collection processing.

This is the pattern the library is built for: functional data handling at the edges, object-oriented boundaries where a domain object or message-oriented service makes the design clearer.

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

You can transform whole collections without introducing temporary mutable state or collectors.

```java
List<String> names = List.of("a", "b", "c")
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
List<String> cleaned = Collection.eachP().modify(rawNames, String::trim);
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

Version: `2.0.9`

### Maven

```xml
<dependency>
    <groupId>io.github.sganesh-code</groupId>
    <artifactId>functional-java</artifactId>
    <version>2.0.9</version>
</dependency>
```

### Gradle

```gradle
implementation 'io.github.sganesh-code:functional-java:2.0.9'
```

## Contributing

- Keep data structures persistent.
- Build new collection behavior from the core triad.
- Prefer readable functional code for data and effects.
- Use OOP boundaries where objects and message passing make the design clearer.
- Add happy-path, error-path, and edge-case tests for API changes.

## License

Licensed under the GNU General Public License v3.0. See [LICENSE](LICENSE).
