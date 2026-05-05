# Feature Scaffold Guide

## Workflow

1. **Ask feature name** — e.g., "login", "profile", "checkout"
2. **Ask complexity** — needs async ops? needs side effects?
3. **Detect base package** — scan existing code for package name
4. **Generate files** — Contract, Reducer, ViewModel
5. **Verify** — compile check

## Step 1: Ask Feature Details

Use `AskUserQuestion`:

**Question 1:** "Feature name?" (free text, e.g., "login")

**Question 2:** "Does this feature need async operations (API calls, database)?"
- Yes → use `DreStoreViewModel` + `AsyncOp` type
- No → use `DreStoreViewModel` with `Nothing` as AsyncOp type

**Question 3:** "Does this feature need side effects (analytics, navigation, toasts)?"
- Yes → include `Effect` sealed interface
- No → use `Nothing` for Effect type

## Step 2: Detect Base Package

Scan for existing Kotlin files to find package convention:
```
Grep pattern: "^package " in app/src/main/kotlin/ or app/src/main/java/
```

Example result: `com.example.myapp` → feature goes in `com.example.myapp.feature.login`

## Step 3: Generate Files

### File 1: Contract ({Feature}Contract.kt)

**With async ops + effects:**
```kotlin
package {basePackage}.feature.{featureLower}

import dev.drekt.core.DreAction
import dev.drekt.core.DreAsyncOp
import dev.drekt.core.DreEffect
import dev.drekt.core.DreState

data class {Feature}State(
    // TODO: define state fields
) : DreState {
    companion object {
        val Initial = {Feature}State()
    }
}

sealed interface {Feature}Action : DreAction {
    // TODO: define actions
}

sealed interface {Feature}Effect : DreEffect {
    // TODO: define side effects
}

sealed interface {Feature}AsyncOp : DreAsyncOp {
    // TODO: define async operations
}
```

**Without async ops (simple):**
```kotlin
package {basePackage}.feature.{featureLower}

import dev.drekt.core.DreAction
import dev.drekt.core.DreEffect
import dev.drekt.core.DreState

data class {Feature}State(
    // TODO: define state fields
) : DreState {
    companion object {
        val Initial = {Feature}State()
    }
}

sealed interface {Feature}Action : DreAction {
    // TODO: define actions
}

sealed interface {Feature}Effect : DreEffect {
    // TODO: define side effects
}
```

### File 2: Reducer ({Feature}Reducer.kt)

**With async ops:**
```kotlin
package {basePackage}.feature.{featureLower}

import dev.drekt.core.ReduceResult
import dev.drekt.core.Reducer

class {Feature}Reducer : Reducer<{Feature}State, {Feature}Action, {Feature}Effect, {Feature}AsyncOp> {

    override fun reduce(
        state: {Feature}State,
        action: {Feature}Action,
    ): ReduceResult<{Feature}State, {Feature}Effect, {Feature}AsyncOp> = when (action) {
        // TODO: handle each action
    }
}
```

**Without async ops:**
```kotlin
package {basePackage}.feature.{featureLower}

import dev.drekt.core.SimpleReduceResult
import dev.drekt.core.SimpleReducer

class {Feature}Reducer : SimpleReducer<{Feature}State, {Feature}Action, {Feature}Effect> {

    override fun reduce(
        state: {Feature}State,
        action: {Feature}Action,
    ): SimpleReduceResult<{Feature}State, {Feature}Effect> = when (action) {
        // TODO: handle each action
    }
}
```

### File 3: ViewModel ({Feature}ViewModel.kt)

**With async ops:**
```kotlin
package {basePackage}.feature.{featureLower}

import dev.drekt.android.DreStoreViewModel

class {Feature}ViewModel(
    reducer: {Feature}Reducer = {Feature}Reducer(),
) : DreStoreViewModel<{Feature}State, {Feature}Action, {Feature}Effect, {Feature}AsyncOp>(
    reducer = reducer,
) {
    override val initialState = {Feature}State.Initial

    override suspend fun executeAsyncOp(op: {Feature}AsyncOp, stateSnapshot: {Feature}State) {
        when (op) {
            // TODO: handle async operations, call dispatch() with result action
        }
    }

    // TODO: add public methods that call dispatch()
}
```

**Without async ops:**
```kotlin
package {basePackage}.feature.{featureLower}

import dev.drekt.android.DreStoreViewModel

class {Feature}ViewModel(
    reducer: {Feature}Reducer = {Feature}Reducer(),
) : DreStoreViewModel<{Feature}State, {Feature}Action, {Feature}Effect, Nothing>(
    reducer = reducer.asFullReducer(),
) {
    override val initialState = {Feature}State.Initial

    override suspend fun executeAsyncOp(op: Nothing, stateSnapshot: {Feature}State) {
        // Nothing type — never called
    }

    // TODO: add public methods that call dispatch()
}
```

## Step 4: Create Files

Create feature directory and write all files:
```
{sourceDir}/{basePackagePath}/feature/{featureLower}/
├── {Feature}Contract.kt
├── {Feature}Reducer.kt
└── {Feature}ViewModel.kt
```

## Step 5: Verify

Run compile:
```bash
./gradlew :app:compileDebugKotlin
```

## Step 6: Suggest Next Steps

- "Wire into Compose UI" — show `collectAsStateWithLifecycle()` pattern
- "Add unit tests" — show `assertReduce` / `assertNoChange` pattern
- "Scaffold another feature" — loop back
