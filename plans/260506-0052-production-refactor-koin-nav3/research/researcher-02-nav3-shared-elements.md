---
name: Navigation 3 & Shared Element Transitions Research
description: Nav3 API surface, ViewModel scoping, migration tactics from nav-compose 2.x, shared-element transitions for production, accessibility patterns
type: reference
---

# Navigation 3 & Compose Shared-Element Transitions

## 1. Nav3 API Surface & Artifacts

**Stable Status:** Navigation 3 reached stability (1.1.0-rc01+); safe for production.

**Core Dependencies:**
```kotlin
androidx.navigation3:navigation3-runtime        // Core navigation state
androidx.navigation3:navigation3-ui             // NavDisplay, scene strategies
org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-navigation3  // ViewModel scoping
io.insert-koin:koin-compose-navigation3:3.6+   // Koin integration
```

**Key Types:**
- `NavBackStack<K>` — transparent list managing back stack; no NavController wrapper
- `NavEntry<K>` — individual back stack item with key K, scoped state retention
- `NavDisplay` — composable rendering entries; auto-animates transitions via `List<SceneStrategy>`
- `entryProvider { entry<Route> { Content() } }` — DSL mapping routes to content
- `rememberNavBackStack<K>()` — memoized back stack with config-change/process-death persistence
- `@Serializable` — kotlinx.serialization annotation for type-safe route classes

**Back Stack Operations:**
```kotlin
backStack.add(Route.Detail(id=5))       // Push destination
backStack.removeLastOrNull()             // Pop (Nav2's popBackStack → this)
backStack.removeLast()                   // Pop w/ exception if empty
```

**Deep Links:** Nav3 status unclear from official docs; likely requires manual URI→Route mapping (unresolved).

---

## 2. Nav3 + ViewModel Scoping

**Lifecycle Decorator Pattern** (required for Koin scoping):
```kotlin
NavDisplay(
  navBackStack = backStack,
  entryProvider = { entry ->
    entry<Route> { route, modifier -> Screen(route, modifier) }
  },
  navEntryDecorators = listOf(
    rememberSaveableStateHolderNavEntryDecorator(),
    rememberViewModelStoreNavEntryDecorator()  // Scope VMs to entry
  )
)
```

**Koin + Nav3 Integration:**
```kotlin
// In Koin module:
navigation<DetailRoute> { params ->
  val viewModel = koinViewModel<DetailViewModel>(
    parameters = { parametersOf(params.id) }
  )
  DetailScreen(viewModel)
}

// Use rememberViewModelStoreNavEntryDecorator() in NavDisplay
// ⚠️ Known issue: koinViewModel not scoped to Nav3 entry without decorator
```

**State Persistence:**
- `rememberNavBackStack()` auto-persists across config changes + process death
- No manual SavedStateHandle needed; built-in
- ViewModels live as long as entry stays on back stack, cleared on pop

---

## 3. Migration from Nav2 (androidx.navigation:navigation-compose 2.8.5)

**Breaking Changes:**

| Nav2 | Nav3 |
|-----|-----|
| `NavController.navigate("route")` | `backStack.add(Route.Detail(id))` |
| `navController.popBackStack()` | `backStack.removeLastOrNull()` |
| `NavHost { NavGraph { composable("route") { } } }` | `NavDisplay { entryProvider { entry<Route> { } } }` |
| String routes | `@Serializable data class Route(...)` |
| Opaque state | Transparent `NavBackStack<K>` list |

**Nested Graphs & Dialogs:**
- Nav2 nested graphs → Nav3 compose nested `NavBackStack` holders (different model)
- Dialog destinations → SceneStrategy.FullyExpanded or custom strategy
- No XML nav graph equivalents

**Type-Safe Routes (Nav3 Way):**
```kotlin
@Serializable
sealed class Route {
  @Serializable data class Detail(val id: Int) : Route()
  @Serializable object Gallery : Route()
}
```

**Key Gotcha:** Animated transitions now declarative via `NavDisplay`'s SceneStrategy API (vs implicit in NavHost); explicit control required.

---

## 4. Shared Element Transitions (Compose 1.7+/1.8+)

**Status:** Experimental as of 1.7.0-beta01; production use at own risk (API may change).

**Core Components:**

```kotlin
SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
  NavDisplay(navBackStack, entryProvider = { entry ->
    entry<Route> { route, modifier ->
      when (route) {
        is Route.Gallery -> GalleryScreen(route, modifier)
        is Route.Detail -> DetailScreen(route, modifier)
      }
    }
  })
}
```

**Gallery→Detail Pattern:**
```kotlin
// Gallery card
Box(
  modifier = Modifier
    .sharedElement(
      state = rememberSharedContentState(key = "card_${item.id}"),
      animatedVisibilityScope = this
    )
    .fillMaxWidth()
    .height(200.dp)
    .clickable { backStack.add(Route.Detail(item.id)) }
) {
  LessonCard(item)
}

// Detail screen (same Route.Detail entry)
Box(
  modifier = Modifier
    .sharedElement(
      state = rememberSharedContentState(key = "card_${route.id}"),
      animatedVisibilityScope = this
    )
    .fillMaxSize()
) {
  DetailContent(route.id)
}
```

**Bounded vs Full Transforms:**
- `Modifier.sharedElement()` — morphs layout+content in place
- `Modifier.sharedBounds()` — clips to bounding box, smoother for different dimensions
- Use `.sharedBounds()` if detail card expands beyond thumbnail bounds

**AnimatedContent Integration:**
```kotlin
AnimatedContent(targetState = backStack.last(), modifier = Modifier) { route ->
  when (route) {
    is Route.Gallery -> GalleryScreen()
    is Route.Detail -> DetailScreen()
  }
}
```

---

## 5. Reduced-Motion & Accessibility

**Detection Pattern:**
```kotlin
val animatorsEnabled = Settings.Global.getInt(
  context.contentResolver,
  Settings.Global.ANIMATOR_DURATION_SCALE,
  1
) != 0

val durationMs = if (animatorsEnabled) 300L else 0L

AnimatedContent(
  targetState = state,
  transitionSpec = {
    fadeIn(animationSpec = tween(durationMs)) togetherWith
    fadeOut(animationSpec = tween(durationMs))
  }
) { ... }
```

**Static Fallback:**
```kotlin
SharedTransitionLayout {
  if (animatorsEnabled) {
    // Shared element + animate
  } else {
    // No transition, instant layout swap
  }
}
```

---

## Concrete Nav3 + Shared-Element Setup

**File:** `GalleryScreen.kt` (from current codebase)

```kotlin
@Composable
fun GalleryScreen(backStack: NavBackStack<Route>) {
  val lessons = // fetch from VM
  
  SharedTransitionLayout {
    LazyColumn {
      items(lessons) { lesson ->
        Box(
          modifier = Modifier
            .sharedElement(
              state = rememberSharedContentState(key = "lesson_${lesson.id}"),
              animatedVisibilityScope = this@SharedTransitionLayout
            )
            .clickable { backStack.add(Route.LessonDetail(lesson.id)) }
        ) {
          LessonThumbnail(lesson)
        }
      }
    }
  }
}

@Composable
fun DetailScreen(route: Route.LessonDetail, backStack: NavBackStack<Route>) {
  Box(
    modifier = Modifier
      .sharedElement(
        state = rememberSharedContentState(key = "lesson_${route.id}"),
        animatedVisibilityScope = ... // SharedTransitionLayout scope
      )
      .fillMaxSize()
  ) {
    DetailContent(route.id)
  }
}
```

---

## Unresolved Questions

1. **Deep-link parity**: Does Nav3 support `<deep-link>` URIs natively, or manual Route←URI mapping required?
2. **SharedTransitionLayout scope bridge**: How to properly pass `AnimatedVisibilityScope` between NavDisplay entries without plumbing?
3. **Koin scoping edge case**: Does `rememberViewModelStoreNavEntryDecorator()` + Koin work with custom scope factories, or only built-in VMs?
4. **Animation version lock**: Will Compose 1.8 stabilize SharedTransitionLayout, or remain experimental?

---

**Status:** DONE  
**Summary:** Nav3 is production-ready (stable) with transparent back stack, Koin requires `rememberViewModelStoreNavEntryDecorator()` for entry scoping, migration from nav-compose 2.x involves string→@Serializable routes + NavController→NavBackStack, shared-element transitions available experimentally in 1.7+, accessibility requires `ANIMATOR_DURATION_SCALE` check.  
**Concerns:** SharedTransitionLayout API experimental; deep-link support status unclear; scope plumbing may require wrapper composables.

---

### Sources
- [Navigation 3 Guide (developer.android.com)](https://developer.android.com/guide/navigation/navigation-3)
- [Navigation 3 Release Notes](https://developer.android.com/jetpack/androidx/releases/navigation3)
- [Shared Element Transitions in Compose](https://developer.android.com/develop/ui/compose/animation/shared-elements)
- [Navigation 3 Migration Guide](https://developer.android.com/guide/navigation/navigation-3/migration-guide)
- [Koin Navigation 3 Integration](https://insert-koin.io/docs/reference/koin-compose/navigation3/)
- [nav3-recipes (GitHub)](https://github.com/android/nav3-recipes)
