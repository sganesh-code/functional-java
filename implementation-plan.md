# Implementation Plan: Functional Typeclass Static Utility Methods
**Target Repository:** functional-java
**Reference Design:** N/A (Epic Description)

The following tickets break down the implementation of the epic to add static utility methods to functional typeclasses. This will allow any type implementing these typeclasses to easily use derived methods via static imports.

---

- [x] **🎟️ [TICKET-000]: New Foundation Typeclasses: Foldable and Contravariant**
  - **Description:** Introduce `Foldable` and `Contravariant` typeclasses which are missing but essential for many generic operations.
  - **Scope:**
    - **In scope:** `Foldable.java`, `Contravariant.java` in `@src/main/java/io/github/senthilganeshs/fj/typeclass/`.
    - **In scope:** Update `Traversable.java` to extend `Foldable`.
  - **Implementation Tasks:**
    - [x] Define `Foldable<W>` interface with `foldl`, `foldr`, and `foldMap`.
      - *Created Foldable.java with foldl, foldr, foldMap, and fold methods, including static utility versions.*
    - [x] Define `Contravariant<W>` interface with `contramap`.
      - *Created Contravariant.java with contramap and its static utility.*
    - [x] Update `Traversable<W>` to extend `Foldable<W>`.
      - *Updated Traversable.java to extend Foldable.*
    - [x] Add static utility `static <W, A, B> B foldMap(Foldable<W> f, Monoid<B> m, java.util.function.Function<A, B> fn, Higher<W, A> fa)` to `Foldable`.
      - *Added static foldMap to Foldable.java.*
    - [x] Add static utility `static <W, A> A fold(Foldable<W> f, Monoid<A> m, Higher<W, A> fa)` to `Foldable`.
      - *Added static fold to Foldable.java.*
    - [x] Create unit tests in `src/test/java/io/github/senthilganeshs/fj/typeclass/FoldableTest.java`.
      - *Created FoldableTest.java using TestNG to verify Foldable utilities and Contravariant mapping logic.*

- [ ] **🎟️ [TICKET-001]: Semigroup and Monoid Static Utilities**
  - **Description:** Add static utility methods to `Semigroup` and `Monoid` interfaces for combining collections of elements.
  - **Scope:**
    - **In scope:** `Semigroup`, `Monoid` interfaces in `@src/main/java/io/github/senthilganeshs/fj/typeclass/`.
    - **Out of scope:** Modifying `Collection` or any existing data structure implementation.
  - **Implementation Tasks:**
    - [ ] Add `static <T> T combine(Semigroup<T> s, T a, T b)` to `Semigroup`.
    - [ ] Add `static <T> T combineN(Semigroup<T> s, T a, int n)` to `Semigroup`.
    - [ ] Add `static <T> T combineAll(Monoid<T> m, Iterable<T> ts)` to `Monoid`.
    - [ ] Add `static <A, B> B foldMap(Monoid<B> m, java.util.function.Function<A, B> f, Iterable<A> as)` to `Monoid`.
    - [ ] Add `static <T> T intercalate(Monoid<T> m, T sep, Iterable<T> ts)` to `Monoid`.
    - [ ] Create unit tests in `src/test/java/io/github/senthilganeshs/fj/typeclass/MonoidUtilsTest.java` verifying these methods with custom types and existing monoids (e.g., `INTEGER_SUM`).

- [ ] **🎟️ [TICKET-002]: Eq, Ord, and Hashable Static Utilities**
  - **Description:** Add static utility methods to `Eq`, `Ord`, and `Hashable` for comparison and hashing logic.
  - **Scope:**
    - **In scope:** `Eq`, `Ord`, `Hashable` interfaces in `@src/main/java/io/github/senthilganeshs/fj/typeclass/`.
  - **Implementation Tasks:**
    - [ ] Add `static <T> boolean notEq(Eq<T> eq, T a, T b)` to `Eq`.
    - [ ] Add `static <T> T min(Ord<T> ord, T a, T b)` and `max(Ord<T> ord, T a, T b)` to `Ord`.
    - [ ] Add `static <T> T clamp(Ord<T> ord, T low, T high, T val)` to `Ord`.
    - [ ] Add `static <T> boolean between(Ord<T> ord, T low, T high, T val)` to `Ord`.
    - [ ] Add `static <A, B> Eq<A> contramap(Eq<B> eq, java.util.function.Function<A, B> f)` to `Eq`.
    - [ ] Add `static <A, B> Ord<A> contramap(Ord<B> ord, java.util.function.Function<A, B> f)` to `Ord`.
    - [ ] Create unit tests in `src/test/java/io/github/senthilganeshs/fj/typeclass/OrdUtilsTest.java`.

- [ ] **🎟️ [TICKET-003]: Functor and Bifunctor Static Utilities**
  - **Description:** Add static utility methods to `Functor` and `Bifunctor` for mapping operations.
  - **Scope:**
    - **In scope:** `Functor`, `Bifunctor` interfaces in `@src/main/java/io/github/senthilganeshs/fj/typeclass/`.
  - **Implementation Tasks:**
    - [ ] Add `static <W, A, B> Higher<W, B> map(Functor<W> f, java.util.function.Function<A, B> fn, Higher<W, A> fa)` to `Functor`.
    - [ ] Add `static <W, A, B> Higher<W, B> as(Functor<W> f, B b, Higher<W, A> fa)` to `Functor`.
    - [ ] Add `static <W, A> Higher<W, Void> voidF(Functor<W> f, Higher<W, A> fa)` to `Functor`.
    - [ ] Add `static <W, A> Higher<W, io.github.senthilganeshs.fj.ds.Tuple<A, A>> tupled(Functor<W> f, Higher<W, A> fa)` to `Functor`.
    - [ ] Add `static <W, A, B, C, D> Higher<Higher<W, C>, D> bimap(Bifunctor<W> f, java.util.function.Function<A, C> fa, java.util.function.Function<B, D> fb, Higher<Higher<W, A>, B> fab)` to `Bifunctor`.
    - [ ] Create unit tests in `src/test/java/io/github/senthilganeshs/fj/typeclass/FunctorUtilsTest.java`.

- [ ] **🎟️ [TICKET-004]: Applicative and Monad Static Utilities**
  - **Description:** Add static utility methods to `Applicative` and `Monad` for sequential and effectful composition.
  - **Scope:**
    - **In scope:** `Applicative`, `Monad` interfaces in `@src/main/java/io/github/senthilganeshs/fj/typeclass/`.
  - **Implementation Tasks:**
    - [ ] Add `static <W, A, B, C> Higher<W, C> liftA2(Applicative<W> app, java.util.function.BiFunction<A, B, C> fn, Higher<W, A> fa, Higher<W, B> fb)` to `Applicative`.
    - [ ] Add `static <W, A, B> Higher<W, io.github.senthilganeshs.fj.ds.Tuple<A, B>> product(Applicative<W> app, Higher<W, A> fa, Higher<W, B> fb)` to `Applicative`.
    - [ ] Add `static <W, A, B> Higher<W, B> flatMap(Monad<W> m, java.util.function.Function<A, Higher<W, B>> fn, Higher<W, A> fa)` to `Monad`.
    - [ ] Add `static <W, A> Higher<W, A> flatten(Monad<W> m, Higher<W, Higher<W, A>> ffa)` to `Monad`.
    - [ ] Add `static <W, A> Higher<W, A> ifM(Monad<W> m, Higher<W, Boolean> cond, Higher<W, A> ifTrue, Higher<W, A> ifFalse)` to `Monad`.
    - [ ] Create unit tests in `src/test/java/io/github/senthilganeshs/fj/typeclass/MonadUtilsTest.java`.

- [ ] **🎟️ [TICKET-005]: Traversable and Profunctor Static Utilities**
  - **Description:** Add static utility methods to `Traversable` and `Profunctor`.
  - **Scope:**
    - **In scope:** `Traversable`, `Profunctor` interfaces in `@src/main/java/io/github/senthilganeshs/fj/typeclass/`.
  - **Implementation Tasks:**
    - [ ] Add `static <W, G, A, B> Higher<G, Higher<W, B>> traverse(Traversable<W> t, Applicative<G> app, java.util.function.Function<A, Higher<G, B>> fn, Higher<W, A> fa)` to `Traversable`.
    - [ ] Add `static <W, G, A> Higher<G, Higher<W, A>> sequence(Traversable<W> t, Applicative<G> app, Higher<W, Higher<G, A>> fga)` to `Traversable`.
    - [ ] Add `static <W, A, B, C, D> Higher<Higher<W, A>, D> dimap(Profunctor<W> p, java.util.function.Function<A, B> f, java.util.function.Function<C, D> g, Higher<Higher<W, B>, C> pbc)` to `Profunctor`.
    - [ ] Create unit tests in `src/test/java/io/github/senthilganeshs/fj/typeclass/TraversableUtilsTest.java`.
