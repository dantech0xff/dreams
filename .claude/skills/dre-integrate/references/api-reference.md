# DRE-KT API Reference

## Maven Coordinates

```kotlin
// Core only (no Android dependency)
implementation("io.github.dantech0xff:dre-core:<latest-version>")

// Android ViewModel integration (includes dre-core)
implementation("io.github.dantech0xff:dre-android:<latest-version>")
```

## Marker Interfaces (dre-core)

All types in the DRE pipeline MUST implement these:

```kotlin
interface DreState      // State types
interface DreAction     // Action types
interface DreEffect     // Side effect types
interface DreAsyncOp    // Async operation types
```

## Reducer (dre-core)

Pure function: `(State, Action) → ReduceResult`.

```kotlin
fun interface Reducer<S : DreState, A : DreAction, E : DreEffect, O : DreAsyncOp> {
    fun reduce(state: S, action: A): ReduceResult<S, E, O>
}
```

Rules:
- MUST be pure — no I/O, no coroutines, no side effects
- MUST be deterministic
- Use guard clauses for invalid states

## SimpleReducer (dre-core)

For features without async ops:

```kotlin
fun interface SimpleReducer<S : DreState, A : DreAction, E : DreEffect> {
    fun reduce(state: S, action: A): SimpleReduceResult<S, E>
}
```

## ReduceResult (dre-core)

```kotlin
data class ReduceResult<S : DreState, E : DreEffect, O : DreAsyncOp>(
    val state: S,
    val sideEffects: List<E> = emptyList(),
    val asyncOp: O? = null,
)

typealias SimpleReduceResult<S, E> = ReduceResult<S, E, Nothing>
```

## SideEffectHandler (dre-core)

Fire-and-forget effect consumer. No dispatch callback.

```kotlin
fun interface SideEffectHandler<E : DreEffect> {
    suspend fun handle(effect: E)
}
```

## DreStore (dre-core)

Platform-agnostic dispatch loop. Use directly for non-Android projects.

```kotlin
class DreStore<S : DreState, A : DreAction, E : DreEffect, O : DreAsyncOp>(
    reducer: Reducer<S, A, E, O>,
    initialState: S,
    scope: CoroutineScope,
    dispatchContext: CoroutineContext,
    sideEffectHandlers: List<SideEffectHandler<E>> = emptyList(),
    onAsyncOp: (suspend (O, S) -> Unit)? = null,
)

// Properties
val state: StateFlow<S>

// Methods
fun dispatch(action: A)
fun close()
```

## DreStoreViewModel (dre-android)

Android ViewModel wrapping DreStore. Override `initialState` and optionally `sideEffectHandlers`.

```kotlin
abstract class DreStoreViewModel<S : DreState, A : DreAction, E : DreEffect, O : DreAsyncOp>(
    reducer: Reducer<S, A, E, O>,
    dispatchContext: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel()

// Open properties (override in subclass)
protected open val initialState: S           // MUST override
protected open val sideEffectHandlers: List<SideEffectHandler<E>> = emptyList()

// Properties
val state: StateFlow<S>

// Methods
protected fun dispatch(action: A)
protected abstract suspend fun executeAsyncOp(op: O, stateSnapshot: S)
```

`executeAsyncOp` contract:
- MUST call `dispatch()` with result action when done
- MUST handle errors internally
- MUST use `stateSnapshot`, NOT `state.value`

## Test Utilities (dre-core)

```kotlin
// Assert reducer output
fun Reducer<S, A, E, O>.assertReduce(given: S, action: A, block: ReduceResult<S, E, O>.() -> Unit)

// Assert no state change (guard clauses)
fun Reducer<S, A, E, O>.assertNoChange(given: S, action: A)
```

## Data Flow

```
Action → Reducer (pure) → ReduceResult
                              ├── State (StateFlow, UI observes)
                              ├── Effects (SideEffectHandlers, fire-and-forget)
                              └── AsyncOp? (executeAsyncOp, dispatches result action back)
```
