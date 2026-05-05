# Concurrency

Coroutines, Flow, structured concurrency, cancellation, race conditions. Default severity Major; promote to Critical when concurrency violations cause crashes, leaks, or data corruption.

## Structured concurrency — the rule

Every coroutine has a parent `CoroutineScope`. The scope completes only when all its children complete. Breaking this means leaks.

### `GlobalScope` is almost always wrong
```kotlin
// CRITICAL: outlives every caller, no parent, no cancellation
GlobalScope.launch { syncData() }

// GOOD: tie to a meaningful lifecycle
class Sync(private val scope: CoroutineScope) {
    fun start() { scope.launch { syncData() } }
}
```
The only legitimate uses of `GlobalScope` are top-level application code that genuinely lives for the process lifetime, and even there an explicit `applicationScope` is clearer.

### Don't fire-and-forget across boundaries
```kotlin
// BAD: caller has no handle, no cancellation, no error
fun process(item: Item) {
    CoroutineScope(Dispatchers.Default).launch { doWork(item) }
}

// GOOD: accept a scope or be a suspend function
suspend fun process(item: Item) = doWork(item)
```
A non-suspend public function that internally creates a scope is a structured-concurrency violation — it hides async work from the caller.

### `runBlocking` outside `main`/tests
```kotlin
// BAD: blocks the calling thread, deadlocks under wrong dispatcher
fun get(id: Id): User = runBlocking { repo.find(id) }
```
`runBlocking` in production code is a Major finding. Make the function `suspend` or push the boundary up.

## Cancellation

### Cooperative cancellation
A coroutine is cancellable only at suspension points. Tight CPU loops must check.

```kotlin
// BAD: ignores cancellation
suspend fun process(items: List<Item>) {
    for (i in items) heavy(i)   // no suspend → no cancel
}

// GOOD
suspend fun process(items: List<Item>) {
    for (i in items) {
        ensureActive()           // throws CancellationException if cancelled
        heavy(i)
    }
}
// or
suspend fun process(items: List<Item>) = items.forEach {
    yield()                      // suspends + cooperates with cancellation
    heavy(it)
}
```

### Never swallow `CancellationException`
```kotlin
// BREAKS structured concurrency — Critical
try { work() } catch (e: Exception) { log(e) }

// GOOD
try { work() }
catch (e: CancellationException) { throw e }
catch (e: Exception) { log(e) }
```
`runCatching { … }` inside coroutines also swallows `CancellationException`; use only outside coroutine scopes or rethrow explicitly.

### `withContext` cleanup
```kotlin
withContext(NonCancellable) {
    // critical cleanup that must finish even if the parent is cancelled
    file.close()
}
```
Use `NonCancellable` only for cleanup; never wrap business logic to "make it not cancel".

## Dispatchers

- CPU-bound work → `Dispatchers.Default`.
- Blocking I/O → `Dispatchers.IO`.
- UI updates → main / immediate dispatcher of the framework in use.
- `Unconfined` is for tests of immediate suspending behaviour — flag in production.
- A `suspend` function should not assume a dispatcher. If it must run on a specific one, it should call `withContext` internally (and the test author can override the parent dispatcher).

```kotlin
// BAD: fragile, depends on caller's context
suspend fun read(): String = file.readText()

// GOOD
suspend fun read(): String = withContext(Dispatchers.IO) { file.readText() }
```
Best practice in larger codebases: inject a dispatcher provider rather than referencing `Dispatchers` directly — makes tests deterministic. The reviewer should not mandate a specific provider library; just flag direct hard-coded references when they prevent testability.

## `launch`, `async`, `await`

- `launch` for fire-and-forget work whose result is not awaited.
- `async` only when you will `await` on the deferred — otherwise use `launch`.
- An `async { … }` whose `Deferred` is dropped is a leak (and exceptions vanish).
- `awaitAll` for parallel completion; calling `await` sequentially in a loop is sequential, not parallel.

```kotlin
// BAD: sequential despite intent
val a = async { fetchA() }.await()
val b = async { fetchB() }.await()

// GOOD: parallel
coroutineScope {
    val a = async { fetchA() }
    val b = async { fetchB() }
    a.await() to b.await()
}
```

## `coroutineScope` vs `supervisorScope`

- `coroutineScope` — child failure cancels siblings. Use when "all or nothing".
- `supervisorScope` — child failure is isolated. Use for independent fan-out (UI streams, parallel reads where partial success is acceptable).
- Picking the wrong one is a Major correctness bug.

## Exception propagation

- An uncaught exception in a `launch` child propagates up and cancels siblings (under `coroutineScope`).
- An uncaught exception in `async` is held in the `Deferred` and surfaces on `await`.
- A `CoroutineExceptionHandler` is only invoked at the *root* of a coroutine tree — adding it to a child does nothing.

## Flow

### Cold by default
A `Flow` does not produce until collected. Two collectors → two independent productions. If you want sharing, use `shareIn` / `stateIn`.

### `StateFlow` and `SharedFlow`
- `StateFlow<T>` — always has a value, conflates duplicates by `equals`. Use for "current state".
- `SharedFlow<T>` — multiple replays / no value required. Use for events.
- A `MutableStateFlow` exposed as `MutableStateFlow` (not narrowed to `StateFlow`) lets every caller mutate — Major.

```kotlin
private val _state = MutableStateFlow(initial)
val state: StateFlow<State> = _state.asStateFlow()
```

### `collect` discipline
- `flow.collect { … }` is suspending. It must be inside a scope you control.
- `launchIn(scope)` is fine for "subscribe and forget within this scope".
- Collecting from two sources sequentially when you wanted to merge them → use `combine` / `merge` / `zip`.

### Backpressure
- `buffer()` for producer/consumer rate mismatch.
- `conflate()` to drop intermediate values.
- `collectLatest` to cancel previous when a new value arrives.
- Default unbuffered Flow that crosses a slow boundary will starve the producer — Major in hot paths.

### Operator hazards
- `flowOn(dispatcher)` affects upstream operators only. Placing it last on the chain is a no-op for downstream collection.
- `map { suspend }` is sequential per element. Use `flatMapMerge` (concurrent) when the work is independent.

## Race conditions

### Unsynchronised shared state
```kotlin
// CRITICAL: lost updates under concurrent launch
var counter = 0
repeat(100) { launch { counter++ } }

// GOOD
val counter = AtomicInteger(0)
repeat(100) { launch { counter.incrementAndGet() } }

// OR: confine state to a single coroutine via a Mutex / actor / single-threaded dispatcher
val mutex = Mutex()
suspend fun inc() = mutex.withLock { counter++ }
```

### Read-then-write across suspension
```kotlin
// BAD: another coroutine can interleave between read and write
val current = repo.load()
repo.save(current.copy(seen = true))

// GOOD: server-side compare-and-set, optimistic locking, or single-writer model
```

### Thread-unsafe collections shared across coroutines
- `mutableListOf`, `HashMap`, `LinkedHashSet` — not thread-safe. Reads and writes from multiple threads can corrupt internal structure.
- `Collections.synchronizedList(...)` is safe per-call but iteration still needs an external lock.
- `ConcurrentHashMap`, `CopyOnWriteArrayList` for concurrent access.
- Single-coroutine confinement via a dedicated dispatcher / actor often beats locking.

### Volatile reads of references
- `@Volatile var ref: T?` only guarantees publication of the reference, not the contents. The pointed-to object must itself be safely published.
- "Lazy double-checked locking" without `@Volatile` is a textbook bug.

## Channels

- A `Channel` without a documented capacity policy is a hazard; pick `Channel.RENDEZVOUS`, `BUFFERED`, `CONFLATED`, or an explicit capacity.
- A producer that never closes its `Channel` keeps the consumer suspended forever → leak.

## Common findings

| Symptom | Likely cause | Severity |
|---------|--------------|----------|
| `GlobalScope.launch` | unstructured | Critical |
| `runBlocking` outside main/test | thread starvation, deadlock | Major |
| `catch (Exception)` no rethrow of `CancellationException` | broken cancellation | Critical |
| `async { … }` whose Deferred is dropped | leaked coroutine, lost exception | Major |
| `Dispatchers.IO` referenced directly inside business logic | testability | Major |
| `MutableStateFlow` exposed publicly | unsynchronised writes | Major |
| Mutating shared `MutableList` from multiple `launch` | data race | Critical |
| CPU loop with no `ensureActive`/`yield` | uncancellable | Major |
| `flowOn` placed at end of chain | misplaced dispatcher | Major |
| `withContext(NonCancellable)` wrapping business logic | cancellation hidden | Major |
