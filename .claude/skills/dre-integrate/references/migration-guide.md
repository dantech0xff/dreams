# Migration Guide: MVI/MVVM → DRE

## Workflow

1. **Ask which ViewModel** — user provides file path or class name
2. **Read existing code** — analyze current state, actions, side effects
3. **Map to DRE types** — classify existing code into State/Action/Effect/AsyncOp
4. **Generate DRE files** — create Contract, Reducer, ViewModel
5. **Show diff** — explain what changed and why
6. **Verify** — compile check

## Step 1: Identify Target

Use `AskUserQuestion`: "Which ViewModel do you want to migrate? (file path or class name)"

Then read the file and all related files (state, events, intents, etc.).

## Step 2: Analyze Existing Code

### From MVVM (LiveData/StateFlow + repository calls in ViewModel)

Identify:
- `MutableStateFlow` / `MutableLiveData` → becomes `DreState`
- Public methods with business logic → becomes `DreAction`
- `viewModelScope.launch` blocks → becomes `DreAsyncOp`
- Navigation events / Toasts / Analytics → becomes `DreEffect`

**Before (MVVM):**
```kotlin
class LoginViewModel(private val repo: AuthRepo) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state = _state.asStateFlow()

    fun login(email: String, password: String) {
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            try {
                val user = repo.login(email, password)
                _state.update { it.copy(loading = false, user = user) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message) }
            }
        }
    }
}
```

**After (DRE):**
- State mutation → pure Reducer
- `repo.login()` → AsyncOp
- Error handling → result action dispatched back

### From MVI (Intent + Reducer pattern)

Identify:
- `Intent` / `Event` sealed class → rename to `DreAction`, implement marker
- `State` data class → implement `DreState`
- `SideEffect` / `Effect` → implement `DreEffect`
- Existing reducer function → wrap in `Reducer` interface
- Middleware / async processing → becomes `DreAsyncOp`

## Step 3: Mapping Rules

| MVVM/MVI Concept | DRE Equivalent |
|---|---|
| `data class UiState(...)` | `data class XState(...) : DreState` |
| `sealed class Intent/Event` | `sealed interface XAction : DreAction` |
| `sealed class SideEffect` | `sealed interface XEffect : DreEffect` |
| `viewModelScope.launch { repo.call() }` | `sealed interface XAsyncOp : DreAsyncOp` |
| Reducer function (if exists) | `class XReducer : Reducer<...>` |
| State mutation in ViewModel | Move to Reducer (pure function) |
| Repository/API calls | `executeAsyncOp()` in ViewModel |
| Navigation/Toast/Analytics | `SideEffectHandler` |

## Step 4: Migration Steps

### 4a. Extract Contract

Pull all types into `{Feature}Contract.kt`:
- Move state class, add `: DreState`
- Move intents/events to sealed interface, add `: DreAction`
- Identify side effects, create sealed interface with `: DreEffect`
- Identify async work, create sealed interface with `: DreAsyncOp`

### 4b. Create Reducer

Move ALL state transition logic out of ViewModel into pure Reducer:
- Every `_state.update { }` → becomes a `when` branch in `reduce()`
- Conditional state changes → guard clauses in Reducer
- NO I/O in Reducer — if a branch does I/O, split into:
  - Sync part → state change + `asyncOp` in ReduceResult
  - Async part → `executeAsyncOp` in ViewModel

### 4c. Slim Down ViewModel

ViewModel becomes thin:
- Extend `DreStoreViewModel`, override `initialState` and optionally `sideEffectHandlers`
- Public methods just call `dispatch(Action)`
- `executeAsyncOp` handles I/O, calls `dispatch(ResultAction)`
- Remove `MutableStateFlow`, `viewModelScope.launch` for state work

### 4d. Extract SideEffectHandlers (optional)

If feature has analytics/navigation/toasts:
- Create handler class implementing `SideEffectHandler<XEffect>`
- Override `sideEffectHandlers` property in ViewModel

## Step 5: Verify

```bash
./gradlew :app:compileDebugKotlin
```

Then suggest writing reducer unit tests using `assertReduce` / `assertNoChange`.

## Common Pitfalls

- **Don't put I/O in Reducer** — if you see `suspend`, `delay`, `withContext` → it's an AsyncOp
- **Don't read `state.value` in `executeAsyncOp`** — use `stateSnapshot` parameter
- **Don't dispatch from SideEffectHandler** — use AsyncOp for feedback loops
- **Don't skip error handling in `executeAsyncOp`** — catch + dispatch error action
