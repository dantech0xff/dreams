# Performance

Most performance findings are Minor. Promote to Major when the cost is on a hot path (per-frame, per-request, per-element of a large stream) or grows with input size.

## Algorithmic complexity

A senior reviewer reads a function and estimates Big-O. Most "slow code" is one accidental nested loop.

```kotlin
// O(n²): contains is O(m) inside an O(n) loop
val matches = items.filter { it.id in otherList.map { o -> o.id } }

// O(n + m)
val ids = otherList.mapTo(HashSet()) { it.id }
val matches = items.filter { it.id in ids }
```

Common O(n²) patterns to flag:

- `list.contains` inside a loop over another list of comparable size.
- `find` / `firstOrNull` inside a loop instead of a precomputed map.
- `map { … }.first { … }` — first the whole list is mapped, then filtered.
- `string += x` inside a loop (each `+=` allocates).

## Eager vs lazy

`Collection` operators are eager: each step allocates a new list. `Sequence` is lazy: one allocation per element across the chain.

```kotlin
// allocates twice (after map, after filter)
val xs = list.map { f(it) }.filter { it.ok }.first()

// fuses; one element flows through
val xs = list.asSequence().map { f(it) }.filter { it.ok }.first()
```

Rule of thumb:
- Small list (< 100), simple chain → `Collection` is fine and reads better.
- Large list, multi-step chain, or only need first N → `asSequence()`.
- Don't mix — `asSequence()` then `toList()` then another `asSequence()` is worse than the original.

## Allocation

### Avoid in hot paths
- `String` concatenation in loops → `StringBuilder` or `joinToString`.
- `mutableListOf<T>().also { … }.toList()` patterns when an `Array` would do.
- `Pair`, `Triple`, `data class` in inner loops — boxing-heavy when the callee is generic.
- Capturing lambdas in loops allocate per iteration; non-capturing lambdas (or method references) don't.

### Boxing
- `List<Int>` boxes every element. `IntArray` does not.
- `Map<Int, …>` boxes keys; `IntObjectMap` (or hand-rolled) avoids it where the workload justifies the complexity.
- Generic type parameters force boxing of primitives — flag in numeric hot paths.

### Collection sizing
```kotlin
// reallocates several times as it grows
val out = mutableListOf<T>()
for (x in source) out += transform(x)

// pre-sized
val out = ArrayList<T>(source.size)
```
Small wins, but trivial to add when the size is known.

## I/O patterns

- N+1 queries (loop calls a repo for each element) → batch.
- Reading a file line-by-line vs `readText` (whole file in memory) — pick by expected size.
- HTTP without connection reuse → flag if the client is constructed per call.
- Synchronous I/O on a UI / latency-critical thread → Major.

## Strings

- `String.format` is convenient but slower than templates / concatenation for hot paths.
- `Regex` compilation is expensive — hoist to a `private val` rather than constructing per call.
- `toLowerCase`/`toUpperCase` allocate; reuse cached results when comparing.
- `+=` on `String` inside a loop is O(n²) total cost.

## Hash data structures

- `HashSet`/`HashMap` initial capacity defaults to 16 — pre-size if the final size is known.
- `LinkedHashMap` preserves insertion order at a small cost — use only when needed.
- A type used as a hash key must have a stable `hashCode`. Mutating a field that participates in `hashCode` after insertion corrupts the structure.

## Concurrency cost

- Locks have overhead; uncontended locks are cheap but not free. In hot paths, prefer atomic primitives or single-writer confinement.
- `synchronized` on `this` exposes the lock to outside code that might also synchronize on `this` → use a private lock object.
- `ConcurrentHashMap.compute` is faster than `get`/`put` pairs that race.
- Coroutine launch has overhead; firing thousands of `launch` per second to do trivial work is wasteful — batch or use a `Channel`.

## JVM and Android specifics (when applicable)

- Reflection is slow and breaks tooling — avoid in hot paths.
- `enumValues<T>()` allocates an array each call; cache.
- On Android, autoboxing into `List<Long>` of timestamps in a render loop is a frame-stealer; use `LongArray`.
- ProGuard/R8 can fold inlines but cannot fix algorithmic mistakes.

## Measurement first

A reviewer should not "optimise" code that is not in a hot path. Premature optimisation costs readability for no benefit. Flag the *suspicion* and ask for a benchmark when the code path's hotness is unclear.

```
This function appears to allocate per element in a render-loop call site.
Suggest measuring with JMH/microbenchmark before changing.
```

## Common findings

| Pattern | Severity | Fix |
|---------|----------|-----|
| `contains` inside a loop, both sides ≥ a few hundred elements | Major | Build a `Set` once |
| `map { … }.first { … }` on long lists | Minor | `asSequence()` or `firstOrNull { … }` directly |
| `for (x in list) result += x` on `String` | Major | `joinToString` or `StringBuilder` |
| `Regex("…")` constructed per call | Minor | Hoist to `private val` |
| `mutableListOf` then `toList` per call on a hot path | Minor | Reuse a builder, or return as `List` directly |
| Boxing in primitive arithmetic (`List<Int>` for a histogram) | Minor | `IntArray` |
| Sync I/O on UI thread | Major | Move to background dispatcher |
| `async`/`launch` per element when a single suspending pass would do | Minor | Process in one coroutine |
| Reflection in inner loops | Major | Resolve once, cache |
| `Date`/`SimpleDateFormat` reconstructed per call | Minor | Hoist (and `SimpleDateFormat` is not thread-safe — use `DateTimeFormatter`) |
