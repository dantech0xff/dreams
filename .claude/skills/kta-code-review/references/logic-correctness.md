# Logic and Correctness

Errors that the compiler cannot catch but a senior reviewer can. Default severity Major; promote to Critical for crashes on a normal path or data corruption.

## Exception safety

### Don't catch what you cannot handle
```kotlin
// BAD: swallows everything, including bugs and OOM
try { doThing() } catch (e: Exception) { /* ignore */ }

// BAD: prints and continues, leaving the system in a partial state
try { doThing() } catch (e: Exception) { e.printStackTrace() }

// GOOD: catch the specific failure you can recover from
try { parse(input) }
catch (e: NumberFormatException) { return Result.failure(e) }
```
Catching `Throwable` or bare `Exception` and continuing is Critical when the failure could leave invariants broken.

### Never swallow `CancellationException`
```kotlin
// CRITICAL — breaks structured concurrency
try { work() } catch (e: Exception) { log(e) }

// GOOD
try { work() }
catch (e: CancellationException) { throw e }
catch (e: Exception) { log(e) }
```
A `catch (e: Exception)` inside a coroutine that does not rethrow `CancellationException` is **always** a finding. Use `runCatching` only outside coroutine scopes — it has the same problem.

### Resource cleanup
```kotlin
// BAD: leak on exception
val conn = open(); doWork(conn); conn.close()

// GOOD
open().use { conn -> doWork(conn) }
```
Flag any pattern that holds a `Closeable` and does not pair it with `use {}` or `try { } finally { close() }`.

### Don't use exceptions for control flow
```kotlin
// BAD
fun lookup(k: String): V {
    try { return cache[k]!! } catch (_: NullPointerException) { return load(k) }
}

// GOOD
fun lookup(k: String): V = cache[k] ?: load(k)
```

## Error contracts

### Pick one error channel per function
A function should signal failure in one of: throw, `Result<T>`, sealed `Outcome`, nullable. Mixing channels (returns `Result<T>` but also throws on some inputs) is a Major violation.

### `Result<T>` discipline
- Use for boundary parsing, decoding, network calls where the caller will branch on failure.
- Don't use for domain logic with rich error variants — model those with a sealed type.
- Don't `getOrThrow()` immediately after constructing a `Result` — just call the underlying function.

### Don't return `null` to mean "error"
When the caller needs to know *why*, `null` discards the reason. Use a sealed `Outcome` or `Result`.

## Invariants

### Validate at construction
```kotlin
// BAD: invalid object can exist
data class Email(val value: String)

// GOOD: constructor refuses bad input
@JvmInline value class Email private constructor(val value: String) {
    companion object {
        fun of(s: String): Email? = if (s.matches(EMAIL_RE)) Email(s) else null
    }
}
```
A type whose existence does not guarantee its invariants forces every caller to revalidate.

### `require` / `check` / `error`
- `require(condition)` — argument validation; throws `IllegalArgumentException`.
- `check(condition)` — internal state precondition; throws `IllegalStateException`.
- `error("…")` — unreachable / impossible state.
- Each should include a message that names the offending value (without leaking secrets).

```kotlin
require(quantity > 0) { "quantity must be positive, got $quantity" }
```

### Don't let invariants leak past constructors
```kotlin
// BAD: invariant only checked here, callers can mutate later
class Order(var status: Status) { init { require(status == Status.New) } }

// GOOD
class Order private constructor(val id: OrderId, val status: Status) {
    fun close() = copy(status = Status.Closed)
}
```

## Equality, hashCode, ordering

- Override `equals` ⇒ override `hashCode` (and ideally `toString`).
- `equals` must be reflexive, symmetric, transitive, consistent.
- Mutable fields participating in `equals` is a bug-magnet — once put in a `Set`/`Map`, the object's hash drifts.
- `compareTo` must be consistent with `equals` unless explicitly documented otherwise.
- `data class` over a class with mutable fields → Major (hash drifts when fields change).

## Edge cases the reviewer must always check

- Empty input (`emptyList`, `""`, no rows).
- Single-element input.
- Input at the boundary (zero, negative, max, min, leap day, midnight).
- Duplicate input.
- Concurrent access (see `concurrency.md`).
- Whitespace-only / Unicode / non-ASCII strings where validation is regex-based.
- Very large input (does the function load it all into memory?).
- `null` arriving from a platform type (`String!` from Java).
- Time and timezone — naive `LocalDateTime.now()` for "now" is rarely correct.
- Floating-point equality (`==` on `Double` is almost always wrong; use a tolerance).

## Numeric correctness

- Integer overflow: `Int.MAX_VALUE + 1` silently wraps. Use `Math.addExact` or `Long` when the input is unbounded.
- Money in `Double` → Major. Use `Long` (cents) or `BigDecimal`.
- Division by zero — `Int / 0` throws, `Double / 0.0` returns `Infinity`/`NaN`. Both are usually bugs.
- `Random` without a seed in code that needs reproducibility (tests, simulations) → Major.

## Date and time

- `Date`, `Calendar`, `SimpleDateFormat` → use `java.time` (`Instant`, `LocalDate`, `ZonedDateTime`, `Duration`).
- Storing wall-clock time when you mean instants → silent bugs at DST.
- Comparing `Instant` with `LocalDateTime` → category error.
- `Thread.sleep` in production code → almost always wrong; usually masking a missing wait or a race.

## Boolean logic

- De Morgan mistakes: `!(a && b)` ≠ `!a && !b`.
- Short-circuiting required: `if (x != null && x.size > 0)` not `x.size > 0 && x != null`.
- Triple negatives (`!isNotEmpty`) are bugs waiting to happen — rewrite positively.

## Casting and type erasure

```kotlin
// BAD: erased generic, succeeds even if elements are Int
val xs = obj as List<String>

// BETTER
val xs = (obj as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
```
Unchecked casts on generics are Major; the compiler warns for a reason.

## Iteration hazards

- Modifying a collection while iterating it → `ConcurrentModificationException`. Use `iterator.remove()` or build a new list.
- `forEach` with an early `return` exits the *enclosing* function (see `kotlin-language.md` → labeled returns).
- Nested loops with `break` need labels — `break@outer`.

## State machines

- A `when` over a state enum without exhaustive coverage of transitions is a bug breeding ground. Prefer sealed classes and a `transition(state, event): state` function whose `when` is exhaustive.
- Transitions that mutate external state should happen exactly once per event — flag duplicate side effects under retry.

## I/O and parsing

- Trust nothing at the boundary. Parse → validate → construct domain type. Never let raw input reach the domain.
- A parser that throws on malformed input requires every caller to wrap in `try`. Prefer `Result<T>` or `T?` return.
- Decoding without a maximum size is a DoS vector — Critical for network/file input.

## Logging as a correctness tool (not just observability)

- Log at boundaries with enough context to reproduce the failure (input shape, not the secret values).
- A `log.error(e.message)` without the stack trace loses the cause — use `log.error("...", e)`.
- Logging in a hot loop is a performance bug; lift outside.
