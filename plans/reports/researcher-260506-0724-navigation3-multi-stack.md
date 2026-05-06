---
title: Navigation3 + Bottom Bar — Per-Tab Back Stack Pattern
date: 2026-05-06
status: DONE
scope: androidx.navigation3 1.1.1, Compose Material3 BOM 2026.02.01
---

# Findings

## 1. Canonical Pattern: Single NavDisplay + `TopLevelBackStack`

Per official Android Developers Nav3 recipe (Common UI), the recommended pattern is **NOT** sibling/nested NavDisplays. It is **one parent NavDisplay** consuming a *flattened* back stack that is composed from per-tab `SnapshotStateList`s held in a `TopLevelBackStack` helper class.

Reference shape (paraphrased from official recipe):

```kotlin
class TopLevelBackStack<T : Any>(startKey: T) {
    private var topLevelStacks: LinkedHashMap<T, SnapshotStateList<T>> =
        linkedMapOf(startKey to mutableStateListOf(startKey))

    var topLevelKey by mutableStateOf(startKey); private set

    val backStack = mutableStateListOf<T>(startKey)

    private fun update() {
        backStack.clear(); backStack.addAll(topLevelStacks.flatMap { it.value })
    }

    fun switchTopLevel(key: T) {
        if (topLevelStacks[key] == null) topLevelStacks[key] = mutableStateListOf(key)
        else topLevelStacks.remove(key)?.let { topLevelStacks[key] = it } // re-order LRU
        topLevelKey = key; update()
    }

    fun add(key: T) { topLevelStacks[topLevelKey]?.add(key); update() }

    fun removeLast() {
        topLevelStacks[topLevelKey]?.removeLastOrNull()
        if (topLevelStacks[topLevelKey]?.isEmpty() == true)
            topLevelStacks.remove(topLevelKey).also { topLevelKey = topLevelStacks.keys.last() }
        update()
    }
}
```

Why single NavDisplay wins for our app:
- One `SharedTransitionLayout` works trivially (rule: only one per nav hierarchy — confirmed by AndroidX docs).
- Transition spec, decorators, entry provider centralized.
- Process-death restoration via the standard `rememberSaveable` path (each per-tab `SnapshotStateList` is saveable; entire holder via `rememberSaveable(saver = …)`).

We will NOT pursue siblings (option b) or nested NavDisplays (option a). Both fight Nav3 design and risk lost shared transitions / duplicate decorator wiring.

## 2. State Preservation

`rememberNavBackStack(initial)` internally uses `rememberSaveable` plus kotlinx.serialization on `NavKey`. Survives:
- Config change: yes, automatic.
- Process death: yes, IF every key is `@Serializable` and implements `NavKey`. Existing `Route.kt` already complies.

For our `TopLevelBackStack` helper we cannot use `rememberNavBackStack` directly (that returns a single `NavBackStack`). Two equivalent approaches:

(A) Use a `rememberSaveable` saver that serializes `topLevelStacks` (a map of List<NavKey>) via a custom `Saver` over a JSON-encoded `Map<String, List<String>>` (using kotlinx.serialization with the polymorphic `NavKey` serializer). Costlier but full process-death support.

(B) Initialize from `SavedStateHandle` at the shell level — needs a small `MainShellViewModel` whose `SavedStateHandle` round-trips the two state pieces (`topLevelKey`, `topLevelStacks` JSON).

Recommendation: **(A)** — keep state in Composable, no extra ViewModel. The serialization is small (3 short stacks of stable Route classes).

Sketch:

```kotlin
@Composable
fun rememberTopLevelBackStack(start: TabKey): TopLevelBackStack<NavKey> =
    rememberSaveable(saver = TopLevelBackStackSaver) { TopLevelBackStack(start.root) }
```

Across tab switches: `topLevelStacks[tab]` is retained in the holder; switching tabs only changes `topLevelKey`. Drill-down depth preserved. Confirmed.

## 3. Shared-Element Transitions

Single outer `SharedTransitionLayout` with single NavDisplay = the same wiring already in `PlaygroundNavHost.kt` (lines 33–34, `LocalSharedTransitionScope provides this`). Since we're consolidating to one NavDisplay, **no extra work** is needed for shared bounds between Lesson list → Lesson detail. Existing `LessonCard` (`lessonSharedKey`) and `LessonDetailScreen` (`lessonSharedKey`) wiring continues to work.

Constraint flagged in AndroidX docs: only ONE `SharedTransitionLayout` per nav hierarchy. With single NavDisplay we are compliant by construction.

## 4. Tap-Current-Tab-To-Pop-To-Root

No built-in helper. Cost is one method on `TopLevelBackStack`:

```kotlin
fun popToRoot() {
    val stack = topLevelStacks[topLevelKey] ?: return
    if (stack.size <= 1) return
    val root = stack.first()
    stack.clear(); stack.add(root)
    update()
}
```

In bottom-bar item click handler:

```kotlin
NavigationBarItem(
    selected = isSelected,
    onClick = {
        if (isSelected) topLevelBackStack.popToRoot()
        else topLevelBackStack.switchTopLevel(tabRoot)
    },
    ...
)
```

Cost: ~10 lines. Recommend including (it's near-free and matches Material/Instagram/YouTube norm).

## 5. Official References

- Android Developers — Nav3 Common UI Recipe (the canonical `TopLevelBackStack` pattern):
  https://developer.android.com/guide/navigation/navigation-3/recipes/common-ui
- Android Developers — Nav3 Save State (serialization & process death):
  https://developer.android.com/guide/navigation/navigation-3/save-state
- Android Developers — Nav3 Basics:
  https://developer.android.com/jetpack/androidx/releases/navigation3
- Android Developers — Shared elements with Navigation:
  https://developer.android.com/develop/ui/compose/animation/shared-elements/navigation
- nav3-recipes repo (Google sample, ongoing): https://github.com/android/nav3-recipes

# Recommended Implementation Shape (Dreams app)

```kotlin
// New: Route additions
@Serializable data object LessonRoot : Route          // tab root
@Serializable data class LessonList(val cat: String) : Route
@Serializable data class LessonDetail(val lessonId: String) : Route
@Serializable data object ShowcaseRoot : Route        // tab root
@Serializable data class Showcase(val lessonId: String) : Route
@Serializable data object SettingsRoot : Route        // tab root (no drill-down)

// MainShell.kt
@Composable
fun MainShell() {
    val motion = rememberAppMotionState()
    val topLevel = rememberTopLevelBackStack(start = LessonRoot)
    val currentTopRoute = topLevel.backStack.lastOrNull()
    val showBar = currentTopRoute !is Route.LessonDetail && currentTopRoute !is Route.Showcase

    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            Scaffold(
                bottomBar = {
                    AnimatedVisibility(
                        visible = showBar,
                        enter = slideInVertically { it },
                        exit = slideOutVertically { it },
                    ) { DreamsBottomBar(topLevel) }
                }
            ) { padding ->
                NavDisplay(
                    backStack = topLevel.backStack,
                    onBack = { topLevel.removeLast() },
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    transitionSpec = ...,
                    popTransitionSpec = ...,
                    modifier = Modifier.padding(padding),
                    entryProvider = entryProvider {
                        entry<Route.LessonRoot> { LessonCategoriesScreen(...) }
                        entry<Route.LessonList> { route -> LessonListScreen(category = ..., ...) }
                        entry<Route.LessonDetail> { route -> LessonDetailScreen(...) }
                        entry<Route.ShowcaseRoot> { ShowcaseListScreen(...) }
                        entry<Route.Showcase> { route -> ShowcaseScreen(...) }
                        entry<Route.SettingsRoot> { SettingsScreen(...) }
                    }
                )
            }
        }
    }
}
```

# Edge Cases

- **System back from drill-down level 2 in any tab** → `topLevel.removeLast()` pops only that tab's stack. Confirmed.
- **System back from a tab root (depth 1)** → `removeLastOrNull` removes the root, then helper falls back to `topLevelStacks.keys.last()` (LRU previous tab). Acceptable, matches YouTube/Instagram. Document this.
- **Switching to a tab whose stack is empty** (never visited) → helper seeds with the root key. Confirmed.
- **Process death during deep drill-down** → `rememberSaveable` restores stacks; ViewModels resolved by Koin's `parametersOf(lessonId)` route param.

# Unresolved Questions

- The exact `Saver` for `TopLevelBackStack<NavKey>` requires kotlinx-serialization polymorphic config — not difficult but worth a small spike during phase-01. Fallback: serialize each tab's stack as `List<String>` (route class name + JSON payload) inline.
- Whether `rememberNavBackStack` could be reused per-tab (3 instances) and combined into a derived flattened list: probably yes, but doesn't match the official recipe shape — not recommended.

**Status:** DONE
