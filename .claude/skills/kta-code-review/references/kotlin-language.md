# Kotlin Language

Senior-level Kotlin idioms. Most issues here are Minor; promote to Major when an antipattern hides a bug or breaks a contract.

## Null safety

### Force-unwrap (`!!`) on data the function does not own
```kotlin
// BAD: crash on any null payload
fun toDomain(dto: UserDto) = User(id = dto.id!!, name = dto.name!!)

// GOOD: encode the parse failure
fun toDomain(dto: UserDto): User? =
    User(id = dto.id ?: return null, name = dto.name ?: return null)
```
`!!` is acceptable only when an invariant proves non-null at that point and is local to the function (e.g. immediately after a `require(x != null)` that doesn't smart-cast through a property).

### Platform types
```kotlin
// BAD: silently treats a possibly-null Java return as non-null
val length = javaApi.getName().length

// GOOD: assert intent at the boundary
val name: String = requireNotNull(javaApi.getName()) { "name from javaApi" }
val length = name.length
```

### Unsafe collection access
```kotlin
list[0]              // throws on empty
map.getValue(k)      // throws if absent
list.first()         // throws on empty

list.firstOrNull()
map[k]               // returns V?
```

## Smart casts and `when`

### Use sealed hierarchies + exhaustive `when`
```kotlin
sealed interface Result<out T> {
    data class Ok<T>(val value: T) : Result<T>
    data class Err(val message: String) : Result<Nothing>
}

fun <T> render(r: Result<T>) = when (r) {
    is Result.Ok -> r.value.toString()
    is Result.Err -> r.message
}            // no `else` needed — exhaustive
```
Flag `when` on sealed types that uses `else -> ...` to swallow new variants; require explicit handling.

### `is` checks over casts
```kotlin
// BAD
val u = obj as User; u.name

// GOOD
if (obj is User) obj.name else null
```

### `as?` over `as` for untrusted input
```kotlin
val user = obj as? User ?: return  // safe at boundaries
```

## Data and immutability

### `val` over `var` — default to immutable
```kotlin
// BAD: mutable list reused across calls
class Cart { val items = mutableListOf<Item>() }

// GOOD: immutable snapshot returned, mutation inside is private
class Cart {
    private val _items = mutableListOf<Item>()
    val items: List<Item> get() = _items.toList()
}
```
A public `val` exposing a `MutableXxx` is a Major finding — callers can mutate the owner's state.

### `data class` only when value semantics fit
- Use for DTOs, value objects, sealed-hierarchy variants.
- Avoid for mutable entities, types with identity, or when `equals/hashCode` over all fields is wrong.
- A `data class` with a `var` property is suspicious — typically a sign that one of `equals/hashCode/copy` will surprise the caller.

### `copy()` to evolve
```kotlin
val v2 = v1.copy(status = Status.Closed)
```
Flag manual constructor reconstruction when `copy` would do.

### Equality
- Override `equals` only with `hashCode`; the contract is non-negotiable.
- For value objects with custom equality, also override `toString` for debuggability.
- `data class` over a class with mutable fields will produce a hash that drifts — Major.

## Functions

### Expression bodies
```kotlin
// VERBOSE
fun double(x: Int): Int { return x * 2 }
// IDIOMATIC
fun double(x: Int) = x * 2
```
Promote to function block when the body has multiple statements or non-trivial control flow.

### Default and named arguments over overloads
```kotlin
// BAD
fun connect(host: String) = connect(host, 80, 30.seconds)
fun connect(host: String, port: Int) = connect(host, port, 30.seconds)
fun connect(host: String, port: Int, timeout: Duration) { ... }

// GOOD
fun connect(host: String, port: Int = 80, timeout: Duration = 30.seconds) { ... }
```

### Top-level functions over utility classes
A class containing only `companion object { fun … }` with no state is usually a top-level file in disguise. Prefer top-level.

## Scope functions

Pick by intent. The wrong scope function obscures meaning.

| Function | Receiver | Returns | Use for |
|----------|----------|---------|---------|
| `let` | `it` | block result | nullable chaining, transformation |
| `run` | `this` | block result | object configuration that returns something |
| `also` | `it` | original | side effect (logging, validation) |
| `apply` | `this` | original | builder-style setup |
| `with` | `this` | block result | non-null receiver, expression |

```kotlin
user?.let { send(it) }                       // null guard
val sb = StringBuilder().apply { append("a"); append("b") }  // builder
items.forEach { it.also(::audit).let(::store) }              // side effect then transform
```
Flag `apply` used to compute a value (use `run`), or `let` whose `it` is unused (drop the wrapper).

## Generics

### Variance
- `out T` (covariance) for producers (`List<out T>`), `in T` (contravariance) for consumers (`Comparator<in T>`).
- A type parameter that only appears in return position should be `out`; only in argument position, `in`.

### Star projection vs `Any?`
```kotlin
fun describe(c: Collection<*>) = "size=${c.size}"   // we never read element type
```
Use `*` when you don't care about the element type; `Collection<Any?>` is wrong because it accepts `Collection<String>` only by accident.

### Reified for type tokens
```kotlin
inline fun <reified T> Gson.fromJson(json: String): T = fromJson(json, T::class.java)
```
Flag passing `Class<T>` parameters when an `inline reified` would remove the parameter.

## `inline` and `crossinline`

- `inline` is a tool for high-frequency higher-order functions and for `reified`. Don't inline cold paths — it bloats call sites.
- `noinline` an unused lambda; `crossinline` when a lambda must not allow non-local return.
- Flag `inline fun` with no lambda parameters and no `reified` — likely cargo-cult.

## Flow of control

### `return` from lambda
```kotlin
items.forEach {
    if (!it.valid) return        // returns from enclosing function — surprising
}
items.forEach {
    if (!it.valid) return@forEach   // returns from lambda
}
```
Prefer labeled returns or `firstOrNull` / `any` style:
```kotlin
val invalid = items.firstOrNull { !it.valid } ?: return
```

### `if` / `when` as expressions
Use them as expressions to avoid mutable accumulators:
```kotlin
val msg = when (status) {
    Status.Ok -> "ready"
    Status.Err -> "failed"
}
```

## API design

- Public API parameters should be `val` (data class) or named with sensible defaults; avoid positional booleans.
- Two adjacent `Boolean` parameters → introduce an enum or `@JvmInline value class`.
- Return concrete `List`, `Set`, `Map` — not `Iterable` / `Collection` — when the caller will index, size, or iterate twice.
- `Sequence` is for lazy chains over large or infinite data; for small lists it's overhead.

## `@JvmInline value class`

```kotlin
@JvmInline value class UserId(val raw: String)
@JvmInline value class Cents(val raw: Long)
```
Use for primitive-obsession (string-typed IDs, money-as-Long). Catches argument-order bugs at compile time. Don't wrap a type that has multiple meaningful operations — make a real class.

## Error signalling

- `Result<T>` for parse / decode / boundary operations where failure is data, not exceptional.
- Sealed `Outcome` for richer domain errors.
- Throw exceptions for programmer errors (`require`, `check`, `error`) and truly exceptional infrastructure failures.
- Don't return `null` for "error" when callers need the reason — that loses information.

## `lateinit` and `Delegates.notNull()`

- `lateinit var` only for non-null properties initialised before first use by a framework (DI / view binding) and never mutated thereafter — though syntactically `var`, treat as write-once.
- `lateinit` on `String` or generic types with no public initialiser is a smell.
- `Delegates.notNull<Int>()` for primitives where `lateinit` doesn't apply.

## Object expressions / `object`

- `object` for true singletons (no state that depends on construction).
- An `object` holding `var` fields is shared mutable state — Major unless protected.
- Object expressions (`object : Listener { ... }`) are fine; SAM lambdas are usually cleaner if the interface is functional.

## File and naming hygiene

- One top-level `class` per file unless the others are tightly coupled (e.g. sealed hierarchy).
- File name matches the primary class.
- Package matches directory.
- Internal helpers go in the same file or a private top-level function — not a new "Utils" class.

## Common smells

| Smell | Fix |
|-------|-----|
| `if (x == null) throw …; x.foo()` | `requireNotNull(x).foo()` or `checkNotNull` |
| `list.size > 0` | `list.isNotEmpty()` |
| `map.containsKey(k); map[k]` | one `map[k]?.let { … }` |
| `for (i in list.indices) list[i]` | `list.forEachIndexed` or just `list.forEach` |
| `mutableListOf<T>().apply { add(a); add(b) }` | `listOf(a, b)` if immutable |
| `obj?.let { it } ?: default` | `obj ?: default` |
| `try { … } catch (e: Exception) { null }` | `runCatching { … }.getOrNull()` |
| `Pair<A, B>` in public API | a named `data class` |
