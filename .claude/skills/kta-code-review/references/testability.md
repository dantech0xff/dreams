# Testability

A senior reviewer judges code by how cheap it is to test. Findings here are Major when the design forces awkward tests; Minor when tests would still work but be brittle.

## The three smells of untestable code

1. **Hidden dependencies** — the function reaches out to global state, system clock, environment variables, file system, network, or singletons that the test cannot replace.
2. **Mixed I/O and decisions** — the same function performs business logic and side effects, so asserting on one requires triggering the other.
3. **Hidden time and randomness** — `System.currentTimeMillis()`, `Date()`, `UUID.randomUUID()`, `Math.random()` called directly inside logic.

If any of the three is true, the test will need to mock half the standard library or accept flakiness. Both are Major.

## Pure cores

Push as much logic as possible into pure functions: input → output, no side effects, no time, no I/O. Pure functions are trivially testable and compose cleanly.

```kotlin
// BAD: time + I/O + decision in one function
fun chargeIfDue(user: User) {
    val now = Instant.now()
    if (Duration.between(user.lastChargedAt, now) > Duration.ofDays(30)) {
        billing.charge(user.id)
        repo.update(user.copy(lastChargedAt = now))
    }
}

// GOOD: pure decision + thin shell
fun isDue(now: Instant, lastChargedAt: Instant, period: Duration): Boolean =
    Duration.between(lastChargedAt, now) > period

class ChargeIfDue(
    private val billing: Billing,
    private val users: UserRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(user: User) {
        val now = clock.instant()
        if (!isDue(now, user.lastChargedAt, BILLING_PERIOD)) return
        billing.charge(user.id)
        users.save(user.copy(lastChargedAt = now))
    }
}
```
The pure `isDue` is a one-line test; the shell is a small integration test.

## Seams

A "seam" is a place where the test can substitute a collaborator without changing production code. The four healthy seams in Kotlin:

| Seam | How |
|------|-----|
| Constructor parameter | inject the collaborator |
| Function parameter | pass the dependency in |
| Interface | depend on an interface, swap the implementation |
| Higher-order function | accept a lambda for the variable behaviour |

```kotlin
// BAD: no seam
class PriceCalculator { fun price(p: Product): Money = TaxService.compute(p) }

// GOOD: seam via parameter
class PriceCalculator(private val tax: TaxService) { fun price(p: Product): Money = tax.compute(p) }
```

## Hidden time, randomness, environment

Wrap them. The reviewer should flag any direct call to:

- `System.currentTimeMillis()`, `System.nanoTime()`, `Instant.now()`, `LocalDate.now()`, `Date()`
- `UUID.randomUUID()`, `Random.Default`, `Math.random()`
- `System.getenv()`, `System.getProperty()`
- `File("…")`, `Paths.get(…)` with a hard-coded path

inside a class that also makes decisions. Inject:

```kotlin
fun interface Clock { fun now(): Instant }
fun interface IdGenerator { fun next(): UUID }
```

Tests pass a fixed clock / deterministic generator. Production passes the real one. The reviewer should not mandate a particular library — just point to the missing seam.

## Mocks vs fakes vs the real thing

- **Real thing** — preferred when fast and deterministic (pure functions, in-memory data structures).
- **Fake** — a hand-written, simple in-memory implementation of an interface. Survives interface changes. Reusable across tests.
- **Mock** — programmatic stub. Best for verifying a single interaction at a system boundary.

Heavy mocking is a smell. A test that sets up six mocks to verify one assertion is testing the mock framework, not the code. Often the fix is a fake or a real collaborator.

```kotlin
// SMELL: every collaborator mocked
val a = mock<A>(); val b = mock<B>(); val c = mock<C>()
every { a.foo() } returns ...
every { b.bar() } returns ...
every { c.baz() } returns ...

// BETTER: collapse to a real or fake where possible
val users = InMemoryUserRepository()
val clock = FixedClock(Instant.parse("2024-01-01T00:00:00Z"))
```

## Behaviour, not implementation

Tests assert on observable behaviour: return value, state visible to callers, side effect at a real boundary. Tests that assert on internal call counts are brittle.

```kotlin
// BAD: couples test to implementation
verify(exactly = 1) { repo.findById(any()) }
verify(exactly = 1) { cache.get(any()) }

// GOOD: assert outcome
val result = service.lookup(id)
assertEquals(expected, result)
```
Mock-call assertions are appropriate at integration boundaries (was the email sent? was the SQL update issued?), not for internal collaborators of a unit under test.

## Determinism

Tests must be deterministic. Sources of flakiness:

- Real wall-clock dependencies (`Thread.sleep`, `Instant.now()`).
- Real network or file I/O inside unit tests.
- Coroutine tests using a real dispatcher instead of a virtual-time scheduler.
- Iteration order of `HashMap`/`HashSet` when the test asserts on order.
- Tests that share mutable static state.

Coroutine tests should use a `TestScope` / virtual-time scheduler so timeouts and delays are deterministic. The reviewer should flag direct `Thread.sleep` in tests as Major.

## Visibility for testing

- Don't widen a member's visibility solely so a test can reach it. If a private function is hard to test directly, the unit boundary is wrong.
- `@VisibleForTesting` (or `internal` reachable from `androidTest`/`test`) is acceptable but a hint that the API is leaking.

## Coverage signal vs noise

Coverage tells you what was *executed*, not what was *checked*. A test that runs the function and asserts nothing inflates coverage and hides bugs. Reviewer should sample tests for actual assertions, not just count lines.

A reasonable rule: every public function on a unit under test has at least one test that asserts both the happy path and one failure path. Pure value objects need only constructor / equality coverage.

## Concurrency tests

- A "test that hopes to catch a race by running 10 000 times" rarely catches anything. Either model the race deterministically (virtual time, ordered events) or accept that the race is real and design it out.
- `runTest { … }` (or equivalent) with a virtual scheduler beats `Thread.sleep` for waiting on coroutine completion.
- A flaky concurrency test is **not** a tooling problem — it's signal that the production code has a real race.

## Test data

- One source of truth per fixture. Don't reinitialise the same object three different ways across tests.
- Builders or `copy()` on a base fixture are fine; ad-hoc literals scattered across files diverge.
- A fixture that violates the type's invariants (constructed via reflection or a back door) hides production bugs.

## What good looks like

- Each test names the scenario in one line.
- Arrange / Act / Assert is visible to the reader.
- The unit under test is constructed in the test (not via a container).
- One logical assertion per test. (Multiple `assert*` calls fine if they verify one outcome.)
- The test fails for one reason. If it can fail for five, split it.
- The test reads like a specification of the unit's contract.

## Common findings

| Symptom | Severity | Fix |
|---------|----------|-----|
| Direct `Instant.now()` / `UUID.randomUUID()` inside a class with logic | Major | Inject `Clock` / `IdGenerator` |
| `runBlocking` in tests instead of `runTest` | Major | Use a virtual-time scope |
| Six-mock setup for one assertion | Major | Use a fake or real collaborator |
| `verify(...)` on internal-only methods | Minor | Assert behaviour |
| Test with no assertions | Major | Add expectations |
| `@VisibleForTesting` on growing surface area | Minor | Reconsider unit boundary |
| `Thread.sleep` to "wait for" a coroutine | Major | Virtual time / awaitility |
| Fixture violating type invariants | Major | Build via the public constructor |
| Static mutable state across tests | Critical (flaky) | Reset in `@After` or remove the static |
