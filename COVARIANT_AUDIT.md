# Covariant Return Type Audit Summary

This document summarizes the findings of TICKET-001 regarding covariant overrides in the `functional-java` collection library.

## Targeted Methods for Standardization

The following methods in the `Collection<T>` interface are the primary targets for standardization (removing covariant overrides in implementations):

- `map(Function<T, R>)`
- `flatMap(Function<T, Collection<R>>)`
- `filter(Predicate<T>)`
- `concat(Collection<T>)`
- `take(int)`
- `drop(int)`
- `slice(int, int)`
- `reverse()`
- `mapMaybe(Function<T, Maybe<R>>)`
- `build(T)`
- `zipWith(BiFunction<T, R, S>, Collection<R>)`

## Impact Matrix

| Data Structure | Covariant Overrides Found | Narrowing API Status |
| :--- | :--- | :--- |
| **List** | map, flatMap, concat, filter, zipWith, take, drop, slice, reverse, mapMaybe, build | `List.from(Collection)` (Exists) |
| **Stack** | map, flatMap, concat, filter, take, drop, reverse, mapMaybe, build | `Stack.from(Collection)` (Exists) |
| **Maybe** | map, flatMap | **REQUIRED: `Maybe.from(Collection)`** |
| **Either** | map, flatMap | **REQUIRED: `Either.from(Collection)`** |
| **Validation** | map | **REQUIRED: `Validation.from(Collection)`** |
| **Queue** | build | `Queue.from(Collection)` (Exists) |
| **Set** | build | `Set.of(Collection)` (Exists) |
| **Vector** | None (Uses Collection types) | `Vector.from(Collection)` (Exists) |
| **Array** | None (Uses Collection types) | `Array.from(Collection)` (Not checked, but not affected) |

## Narrowing Recommendation

For all data structures where covariant overrides are removed, the recommended pattern for consumers needing the specific type is:

```java
List<Integer> list = List.from(collection.map(x -> x + 1));
```

This ensures that the intermediate operations remain generic and interoperable while providing a clear path to narrow the type when implementation-specific features are needed.
