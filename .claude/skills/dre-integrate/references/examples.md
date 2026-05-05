# DRE-KT Examples

## Example 1: Counter (with async ops)

### Contract

```kotlin
data class CounterState(
    val count: Int = 0,
    val loading: Boolean = false,
    val message: String? = null,
) : DreState

sealed interface CounterAction : DreAction {
    data object Increment : CounterAction
    data object Decrement : CounterAction
    data object Reset : CounterAction
    data object LoadRandom : CounterAction
    data class RandomLoaded(val value: Int) : CounterAction
    data class LoadFailed(val error: String) : CounterAction
}

sealed interface CounterEffect : DreEffect {
    data class ShowToast(val text: String) : CounterEffect
}

sealed interface CounterAsyncOp : DreAsyncOp {
    data object FetchRandom : CounterAsyncOp
}
```

### Reducer

```kotlin
class CounterReducer : Reducer<CounterState, CounterAction, CounterEffect, CounterAsyncOp> {
    override fun reduce(state: CounterState, action: CounterAction) = when (action) {
        is CounterAction.Increment -> ReduceResult(state.copy(count = state.count + 1))
        is CounterAction.Decrement -> ReduceResult(state.copy(count = state.count - 1))
        is CounterAction.Reset -> ReduceResult(
            state = state.copy(count = 0),
            sideEffects = listOf(CounterEffect.ShowToast("Counter reset")),
        )
        is CounterAction.LoadRandom -> {
            if (state.loading) ReduceResult(state) // guard clause
            else ReduceResult(state.copy(loading = true), asyncOp = CounterAsyncOp.FetchRandom)
        }
        is CounterAction.RandomLoaded -> ReduceResult(
            state = state.copy(count = action.value, loading = false),
            sideEffects = listOf(CounterEffect.ShowToast("Loaded: ${action.value}")),
        )
        is CounterAction.LoadFailed -> ReduceResult(state.copy(loading = false, message = action.error))
    }
}
```

### ViewModel

```kotlin
class CounterViewModel(
    reducer: CounterReducer = CounterReducer(),
) : DreStoreViewModel<CounterState, CounterAction, CounterEffect, CounterAsyncOp>(
    reducer = reducer,
) {
    override val initialState = CounterState()

    override suspend fun executeAsyncOp(op: CounterAsyncOp, stateSnapshot: CounterState) {
        when (op) {
            is CounterAsyncOp.FetchRandom -> {
                try {
                    delay(1_000)
                    val random = (1..100).random()
                    dispatch(CounterAction.RandomLoaded(random))
                } catch (e: Exception) {
                    dispatch(CounterAction.LoadFailed(e.message ?: "Unknown error"))
                }
            }
        }
    }

    fun increment() = dispatch(CounterAction.Increment)
    fun decrement() = dispatch(CounterAction.Decrement)
    fun reset() = dispatch(CounterAction.Reset)
    fun loadRandom() = dispatch(CounterAction.LoadRandom)
}
```

### Compose UI

```kotlin
@Composable
fun CounterScreen(vm: CounterViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (state.loading) CircularProgressIndicator()
        Text("${state.count}", fontSize = 64.sp)
        Row {
            Button(onClick = vm::decrement) { Text("-") }
            Button(onClick = vm::increment) { Text("+") }
        }
        Row {
            OutlinedButton(onClick = vm::reset) { Text("Reset") }
            OutlinedButton(onClick = vm::loadRandom, enabled = !state.loading) { Text("Random") }
        }
    }
}
```

## Example 2: Settings (simple, no async)

### Contract

```kotlin
data class SettingsState(
    val darkMode: Boolean = false,
    val notifications: Boolean = true,
) : DreState

sealed interface SettingsAction : DreAction {
    data object ToggleDarkMode : SettingsAction
    data object ToggleNotifications : SettingsAction
}

sealed interface SettingsEffect : DreEffect {
    data class ShowToast(val msg: String) : SettingsEffect
}
```

### Reducer + ViewModel

```kotlin
class SettingsReducer : SimpleReducer<SettingsState, SettingsAction, SettingsEffect> {
    override fun reduce(state: SettingsState, action: SettingsAction) = when (action) {
        is SettingsAction.ToggleDarkMode -> SimpleReduceResult(
            state = state.copy(darkMode = !state.darkMode),
            sideEffects = listOf(SettingsEffect.ShowToast(if (!state.darkMode) "Dark mode on" else "Dark mode off")),
        )
        is SettingsAction.ToggleNotifications -> SimpleReduceResult(
            state = state.copy(notifications = !state.notifications),
        )
    }
}

class SettingsViewModel(
    reducer: SettingsReducer = SettingsReducer(),
) : DreStoreViewModel<SettingsState, SettingsAction, SettingsEffect, Nothing>(
    reducer = reducer.asFullReducer(),
) {
    override val initialState = SettingsState()

    override suspend fun executeAsyncOp(op: Nothing, stateSnapshot: SettingsState) {
        // Nothing type — never called
    }

    fun toggleDarkMode() = dispatch(SettingsAction.ToggleDarkMode)
    fun toggleNotifications() = dispatch(SettingsAction.ToggleNotifications)
}
```

## Example 3: Reducer Unit Tests

```kotlin
class CounterReducerTest {
    private val reducer = CounterReducer()

    @Test fun `increment updates count`() {
        reducer.assertReduce(given = CounterState(count = 5), action = CounterAction.Increment) {
            assertThat(state.count).isEqualTo(6)
            assertThat(sideEffects).isEmpty()
            assertThat(asyncOp).isNull()
        }
    }

    @Test fun `load random while loading is rejected`() {
        reducer.assertNoChange(CounterState(loading = true), CounterAction.LoadRandom)
    }

    @Test fun `load random starts async`() {
        reducer.assertReduce(given = CounterState(), action = CounterAction.LoadRandom) {
            assertThat(state.loading).isTrue()
            assertThat(asyncOp).isEqualTo(CounterAsyncOp.FetchRandom)
        }
    }
}
```

## Example 4: SideEffectHandler

```kotlin
class AnalyticsHandler(
    private val tracker: AnalyticsTracker,
) : SideEffectHandler<CounterEffect> {
    override suspend fun handle(effect: CounterEffect) {
        when (effect) {
            is CounterEffect.ShowToast -> tracker.log("toast_shown", mapOf("text" to effect.text))
        }
    }
}

// Wire into ViewModel
class CounterViewModel(
    reducer: CounterReducer = CounterReducer(),
    private val analyticsHandler: AnalyticsHandler,
) : DreStoreViewModel<...>(
    reducer = reducer,
) {
    override val initialState = CounterState()
    override val sideEffectHandlers = listOf(analyticsHandler)
    // ...
}
```
