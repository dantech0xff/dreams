# Recomposition Optimization Checks

Targeted checks for one @Composable. Each check: pattern → fix → severity.

## 1. Unstable parameter types

**Pattern**
```kotlin
@Composable fun Foo(items: List<Item>, tags: Map<String, String>)
```
`List`, `Map`, `Set`, `MutableState<T>` flagged unstable → recomposes on parent recomposition even if equal.

**Fix**
- Wrap data class with `@Immutable` if all fields are val + immutable types.
- Use `kotlinx.collections.immutable.ImmutableList` / `PersistentList`.
- Or hoist the unstable param up and pass primitive snapshots.

**Severity:** high if param drives leaf recompose; medium if leaf is cheap.

## 2. Lambda recreated every recomposition

**Pattern**
```kotlin
Button(onClick = { viewModel.doX() }) { ... }
```
Every recomposition creates a new lambda → child sees a new instance.

**Fix**
```kotlin
val onClickX = remember(viewModel) { { viewModel.doX() } }
Button(onClick = onClickX) { ... }
```
Or use method reference: `onClick = viewModel::doX`.

**Severity:** medium (high inside `LazyColumn` items).

## 3. Reading state too high

**Pattern**
```kotlin
@Composable fun Screen(vm: VM) {
    val state by vm.uiState.collectAsState()
    Header() // doesn't use state
    Body(state.text)
    Footer() // doesn't use state
}
```
Header + Footer recompose on every state change.

**Fix**
- Push state read into the consumer composable, OR
- Pass `() -> String` lambda producer so only Body re-reads.
- Wrap volatile fields in `derivedStateOf` if computed.

**Severity:** high.

## 4. Missing `key()` in loops

**Pattern**
```kotlin
LazyColumn { items(list) { item -> Row(item) } }
```
On reorder/insert, all items recompose.

**Fix**
```kotlin
LazyColumn { items(list, key = { it.id }) { item -> Row(item) } }
```

**Severity:** high for lists > 10 items or with animation.

## 5. Inline Modifier recreation

**Pattern**
```kotlin
Box(modifier = Modifier.size(48.dp).padding(8.dp).clip(CircleShape))
```
Inside a hot recompose path → repeated Modifier chain allocation.

**Fix**
- Hoist as constant: `private val IconModifier = Modifier.size(48.dp)...`
- Or `remember { Modifier... }` if it depends on local state.

**Severity:** low (only matters in Lazy lists / animations).

## 6. Computed value without `derivedStateOf`

**Pattern**
```kotlin
val showButton = scrollState.value > 100
```
Recomposes on every scroll pixel.

**Fix**
```kotlin
val showButton by remember { derivedStateOf { scrollState.value > 100 } }
```

**Severity:** high in scroll/drag scenarios.

## 7. `MutableState` instead of `State` parameter

**Pattern**
```kotlin
@Composable fun Foo(text: MutableState<String>)
```
Couples caller. Hard to test. Recomposes parent.

**Fix**
```kotlin
@Composable fun Foo(text: String, onTextChange: (String) -> Unit)
```

**Severity:** medium.

## 8. `CompositionLocal` overuse

**Pattern**
Reading multiple `CompositionLocal` at root, passing values down.

**Fix**
- Read at the lowest needed level.
- Project-specific: prefer `<Prefix>Theme.colors/typo/spacing` accessors over `LocalContentColor`.

**Severity:** low.

## 9. `LaunchedEffect` with mutable key

**Pattern**
```kotlin
LaunchedEffect(state) { ... } // state is whole object
```
Restarts effect on any field change.

**Fix**
- Use the smallest necessary key: `LaunchedEffect(state.id)`.
- `Unit` if it should run once.

**Severity:** medium.

## 10. Side-effect in composition body

**Pattern**
```kotlin
@Composable fun Foo() {
    analytics.track("seen")  // runs every recompose
    ...
}
```

**Fix**
```kotlin
LaunchedEffect(Unit) { analytics.track("seen") }
```

**Severity:** high (correctness, not just perf).

## Output format for findings

```
[recomposition][high] L42–L48: <pattern name>
  Issue: <one sentence>
  Fix: <concrete change, named API>
```
