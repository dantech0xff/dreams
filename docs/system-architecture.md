# Dreams: System Architecture

## High-Level System Diagram

```
┌─────────────────────────────────────────────────────────┐
│              Compose UI Layer (Feature)                 │
├─────────────────────────────────────────────────────────┤
│ LessonCategories | LessonList | LessonDetail | Showcase │
│ ShowcaseList | Settings (Each owns ViewModel + UiState) │
└────────────┬────────────────────────────┬────────────────┘
             │                            │
    Navigation3 Routes (@Serializable)    │
             │                            │
             └──────────────┬─────────────┘
                            │
┌───────────────────────────▼─────────────────────────────┐
│          Domain Layer (Interfaces)                       │
├─────────────────────────────────────────────────────────┤
│ LessonRepository (sealed)                                │
│ UserPrefsRepository (sealed)                             │
└───────────────────────────┬────────────────────────────┐
                            │                            │
         ┌──────────────────┘                            │
         │                                               │
┌────────▼──────────────┐              ┌────────────────▼──┐
│  Data Layer           │              │ Core Layer         │
├──────────────────────┤              ├─────────────────┤
│ LessonRepositoryImpl  │              │ RuntimeShader   │
│ • LessonRegistry     │              │ • AGSL utils    │
│ • 23 lesson sources  │              │ • Uniforms      │
│ • showcases()        │              │                 │
│                      │              │ Motion          │
│ UserPrefsRepoImpl     │              │ • Reduced-motion│
│ • DataStore backing  │              │ • Tween → snap  │
│ • Prefs flow         │              │                 │
└───────────┬──────────┘              └────────────────┘
            │
    ┌───────▴──────────┐
    │  DataStore Prefs │
    │  (dreams_prefs)  │
    │  • lastLessonId  │
    │  • favorites     │
    │  • paramOverride │
    │  • colorOverride │
    │  • themeMode     │
    │  • dynamicColor  │
    │  • reducedMotion │
    └──────────────────┘
```

---

## Dependency Injection (Koin 4.2.0)

### Module Composition

```kotlin
// core/di/AppModule.kt
val appModule = module {
    // Cross-cutting singletons (logging, dispatchers, etc.)
}

// core/di/DataModule.kt
val dataModule = module {
    single<DataStore<Preferences>> { 
        PreferenceDataStoreFactory.create(produceFile = { ... })
    }
    single<LessonRepository> { LessonRepositoryImpl() }
    single<UserPrefsRepository> { UserPrefsRepositoryImpl(get()) }
}

// core/di/FeatureModule.kt
val featureModule = module {
    viewModel { LessonCategoriesViewModel(get()) }
    viewModel { (categoryName: String) -> LessonListViewModel(get(), get(), categoryName) }
    viewModel { (lessonId: String) -> LessonDetailViewModel(get(), get(), lessonId) }
    viewModel { ShowcaseListViewModel(get()) }
    viewModel { (lessonId: String) -> ShowcaseViewModel(get(), lessonId) }
}
```

### Initialization
- **App.onCreate():** `startKoin { modules(appModule, dataModule, featureModule) }`
- **Parameter injection:** `parametersOf(route.lessonId)` passes lesson ID to ViewModel via Koin DSL
- **Verification:** `KoinModulesCheckTest.dataModule.verify()` ensures all singletons resolve

### Scope Lifecycle
- **appModule + featureModule singletons:** App lifetime
- **ViewModels:** Navigation entry lifetime (via `rememberViewModelStoreNavEntryDecorator`)
- **DataStore:** Singleton, shared by all repositories

---

## Navigation (Navigation3 1.1.1 + Bottom Tab Shell)

### Architecture: MainShell + TopLevelBackStack
```
MainActivity
    ↓
MainShell()
    ├── SharedTransitionLayout (single layout for all shared-element transitions)
    ├── Scaffold
    │   ├── bottomBar: AnimatedVisibility(
    │   │              visible = !isFullscreenRoute(currentRoute),
    │   │              content = DreamsBottomBar)
    │   └── content: NavDisplay(backStack, ...)
    │       └── currentRoute → appropriate Screen Composable
    │
    └── TopLevelBackStack (singleton helper)
        ├── topLevelKey: TabKey (LESSON | SHOWCASE | SETTINGS)
        ├── topLevelStacks: Map<TabKey, List<Route>> (per-tab stack)
        └── Operations: switchTopLevel(), popToRoot(), removeLast()
```

Three bottom tabs with per-tab back stacks:
- **Lesson tab:** LessonCategories → LessonList(categoryName) → LessonDetail(lessonId)
- **Showcase tab:** ShowcaseList → ShowcaseScreen(lessonId)
- **Settings tab:** SettingsScreen (single page, modal About sheet)

### Route Definition
```kotlin
sealed interface Route : NavKey {
    @Serializable
    data object LessonCategoriesRoot : Route
    
    @Serializable
    data class LessonList(val categoryName: String) : Route
    
    @Serializable
    data class LessonDetail(val lessonId: String) : Route
    
    @Serializable
    data object ShowcaseListRoot : Route
    
    @Serializable
    data class Showcase(val lessonId: String) : Route
    
    @Serializable
    data object SettingsRoot : Route
}
```

### Navigation Flow
1. **App launch:** MainActivity → MainShell → Lesson tab shows LessonCategories
2. **Lesson tab drill-down:** LessonCategories → LessonList(categoryName) → LessonDetail(lessonId)
3. **Showcase tab:** ShowcaseList → ShowcaseScreen(lessonId) (full-screen, bar hidden)
4. **Settings tab:** SettingsScreen (full-screen settings page)
5. **Tap current tab:** Pop to root of current tab (platform-standard behavior)
6. **System back from home route:** Exit app (vs. switch tabs)

### Bottom Bar Visibility
- **Animated route-driven logic:** `AnimatedVisibility` in Scaffold receives a `derivedStateOf { backStack.lastOrNull() }`
- **Hidden on:** LessonDetail, Showcase (fullscreen routes)
- **Visible on:** LessonCategories, LessonList, ShowcaseList, SettingsScreen
- **Reduced-motion:** Respects system setting; snap vs. slide+fade animation

### State Preservation
- **Per-tab stacks saved:** TopLevelBackStack.Saver encodes all route stacks (requires fixes for unknown routes + back-to-exit behavior per code review)
- **rememberNavBackStack:** Manages flattened back-stack across all routes
- **rememberViewModelStoreNavEntryDecorator:** Preserves per-route ViewModel across config change
- **Process death:** All route stacks + ViewModel state restored via Bundle serialization (with fallback on saver decode error)

---

## ViewModel & State Management

### Per-Feature ViewModel Pattern

Each screen owns a ViewModel exposing `StateFlow<UiState>`:

#### LessonCategoriesViewModel
```kotlin
val uiState: StateFlow<LessonCategoriesUiState>
// State: categories (4 educational; excludes SHOWCASE), isLoading, error
// Actions: onSelectCategory(categoryName) [navigates via callback]
```

#### LessonListViewModel
```kotlin
val uiState: StateFlow<LessonListUiState>
// State: categoryName, lessons (filtered by category), favorites, lastLessonId, isLoading
// Actions: toggleFavorite(lessonId), markLessonViewed(lessonId) [debounced]
```

#### LessonDetailViewModel
```kotlin
val uiState: StateFlow<LessonDetailUiState>
// State: lesson, paramValues (SnapshotStateMap), lastLessonId update
// Actions: setParamValue(key, value) [debounced], markLessonViewed()
```

#### ShowcaseListViewModel
```kotlin
val uiState: StateFlow<ShowcaseListUiState>
// State: showcases (3 items), isLoading, error
// Actions: onSelectShowcase(lessonId) [navigates via callback]
```

#### ShowcaseViewModel
```kotlin
val uiState: StateFlow<ShowcaseUiState>
// State: lesson, paramValues
// Actions: setParamValue(key, value)
```

### State Immutability
- **UiState:** Immutable data classes (val, copy())
- **Updates:** via `MutableStateFlow<UiState>.update { it.copy(...) }`
- **Reads:** Composables collect `uiState.collectAsState()`, react to changes

### Parameter Persistence (200ms Debounce)
```kotlin
// LessonDetailViewModel
private val paramUpdateFlow = MutableSharedFlow<Pair<String, Float>>()

init {
    viewModelScope.launch {
        paramUpdateFlow
            .debounce(200) // User finishes sliding
            .collect { (key, value) ->
                prefs.setParamOverride(lessonId, key, value)
            }
    }
}

fun setParamValue(key: String, value: Float) {
    _ui.update { it.copy(paramValues = /* update locally */) }
    viewModelScope.launch { paramUpdateFlow.emit(Pair(key, value)) }
}
```

---

## Data Layer

### LessonRepository
```kotlin
interface LessonRepository {
    fun allLessons(): List<Lesson>
    fun byCategory(category: LessonCategory): List<Lesson>
    fun byId(lessonId: String): Lesson?
}

// Impl: LessonRepositoryImpl
internal object LessonRegistry {
    fun bootstrap() { /* idempotency guard: if (all.isEmpty()) */ }
}
```

**Why internal + interface:** Hides singleton, prevents direct mutation, testable via fakes.

**Idempotency guard:** `if (all.isEmpty())` before loading 23 lessons; safe for test restart.

### UserPrefsRepository
```kotlin
interface UserPrefsRepository {
    val prefsFlow: Flow<UserPrefs>
    suspend fun setLastLessonId(id: String)
    suspend fun toggleFavorite(id: String): Boolean
    suspend fun setParamOverride(lessonId: String, uniform: String, value: Float)
    suspend fun setColorOverride(lessonId: String, uniform: String, argb: Int)
    suspend fun clearLessonOverrides(lessonId: String)
    suspend fun setReducedMotion(enabled: Boolean)
    suspend fun setUseDynamicColor(enabled: Boolean)
    suspend fun setThemeMode(mode: ThemeMode)
}

// Backed by DataStore<Preferences>
// Keys: "last_lesson_id", "favorites", "param_overrides", "color_overrides",
//       "reduced_motion", "use_dynamic_color", "theme_mode"
```

**Read pattern:** `prefsFlow.collect { snapshot -> _ui.update { ... } }`
**Write pattern:** `prefs.method()` → DataStore → Flow emits new snapshot → ViewModel receives update

---

## Shared-Element Transitions (Compose Material Motion)

### Gallery → LessonDetail Transition
```kotlin
@Composable
fun GalleryScreen() {
    SharedTransitionLayout {
        LazyColumn {
            items(lessons) { lesson ->
                LessonCard(
                    lesson = lesson,
                    modifier = Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState("lesson-card-${lesson.id}"),
                        animatedVisibilityScope = this@AnimatedVisibilityScope
                    )
                )
            }
        }
    }
}

@Composable
fun LessonDetailScreen() {
    SharedTransitionLayout {
        LessonHeader(
            lesson = lesson,
            modifier = Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState("lesson-card-${lesson.id}"),
                animatedVisibilityScope = this@AnimatedVisibilityScope
            )
        )
    }
}
```

**Key:** Both screens use same `sharedContentState` key (`"lesson-card-{id}"`); Motion reconciles bounds.

### Reduced-Motion Integration
```kotlin
val motionEnabled = !prefs.reducedMotionOverride && 
                    systemAnimatorDurationScale > 0

val animationSpec = if (motionEnabled) 
    spring(stiffness = Spring.StiffnessLow) 
else 
    snap()

// Applied to all Transition and animate*AsState() calls
```

---

## Preferences (DataStore)

### Data Structure
```kotlin
data class UserPrefs(
    val lastLessonId: String? = null,
    val favorites: ImmutableSet<String> = persistentSetOf(),
    val paramOverrides: ImmutableMap<String, ImmutableMap<String, Float>> = persistentMapOf(),
    val colorOverrides: ImmutableMap<String, ImmutableMap<String, Int>> = persistentMapOf(),
    val reducedMotionOverride: Boolean = false,
    val useDynamicColor: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DEFAULT,
)
```

### Serialization
- **Favorites:** Preferences string set
- **paramOverrides:** JSON-encoded `Map<lessonId, Map<paramKey, value>>` (per-lesson shader uniforms)
- **colorOverrides:** JSON-encoded `Map<lessonId, Map<uniform, ARGB int>>`
- **themeMode:** String enum value: `light` or `dark`; invalid/legacy values fall back to `dark`
- **DataStore file:** `dreams_prefs` in app's private data dir

### Access Pattern
- **Read:** `prefsFlow: Flow<UserPrefs>` (hot, cached in-memory after first read)
- **Write:** Suspend functions (`toggleFavorite()`, `setLastLessonId()`) → DataStore → flow emits new snapshot
- **Atomicity:** DataStore writes are atomic; partial failures don't occur

---

## Lesson Sources

### Package Structure
```
data/lesson/source/
├── basics/       (6 lessons: uniforms, fragCoord, gradients, polar, waves, patterns)
├── sdf/          (6 lessons: circle, rounded box, metaballs, breathing grid, combine, invert)
├── noise/        (6 lessons: hash, value noise, fBM, voronoi, plasma, lava)
├── posteffect/   (5 lessons: blur, aberration, ripple-tap, dissolve, glass)
└── showcase/     (3 lessons: liquid glass, aurora, raymarched sphere)
```

### Lesson Entity
```kotlin
data class Lesson(
    val id: String,              // "basics-001", "showcase-liquid-glass"
    val category: LessonCategory, // BASICS, SDF, NOISE, POST_EFFECT, SHOWCASE
    val title: String,
    val description: String,
    val agslCode: String,         // RuntimeShader AGSL source (460)
    val uniforms: List<Uniform>,  // Parameters: name, type, default, min, max
)

data class Uniform(
    val name: String,
    val type: String,             // "float", "int", "half4"
    val defaultValue: Float,
    val min: Float = 0f,
    val max: Float = 1f,
)
```

### Bootstrap
```kotlin
// LessonRepositoryImpl.init()
init {
    LessonRegistry.bootstrap() // if (all.isEmpty()) load all lessons from sources
}
```

**Guard:** Prevents duplicate lesson loading if module instantiated multiple times (test restarts).

---

## AGSL Execution (Core Layer)

### RuntimeShader Integration
```kotlin
@Composable
fun AGSLRenderer(
    shader: String,
    paramValues: SnapshotStateMap<String, Float>,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val runtimeShader = RuntimeShader(shader)
        
        // Update uniforms from paramValues (Compose thread, per-frame)
        paramValues.forEach { (name, value) ->
            runtimeShader.setFloatUniform(name, value)
        }
        
        // Paint with ShaderBrush
        drawRect(
            brush = ShaderBrush(runtimeShader),
            size = size
        )
    }
}
```

### Thread Model
- **Compose thread:** SnapshotStateMap writes, uniform updates (no blocking)
- **GPU:** Shader compilation + execution async; frame delivery via Compose frame clock
- **No ViewModel thread:** Parameters stay in-memory (SnapshotStateMap), persisted async via debounced DataStore write

### Reduced-Motion Fallback
- **If reduced-motion:** Shader still renders, but transitions to detail use `snap()` animation
- **Full-screen AGSL:** Always renders; no CPU-fallback needed for shaders themselves

---

## Testing Architecture

### Unit Tests (JVM)
- **DI validation:** `KoinModulesCheckTest.dataModule.verify()` (no Android context needed for dataModule)
- **ViewModel tests:** Turbine + UnconfinedTestDispatcher + FakeDataStore
  - Time doesn't advance; debounce completes synchronously when advanceUntilIdle()
  - Mock LessonRepository returns test lessons
- **Prefs tests:** Real DataStore via `PreferenceDataStoreFactory.create(produceFile = { tempFolder.newFile() })`

### Instrumented Tests (androidTest)
- **Setup:** `DreamsTestRunner` extends `AndroidJUnitRunner`, overrides `newApplication()` to inject `TestDreamsApp`
- **TestDreamsApp:** Custom Application subclass that startKoin with fake LessonRepository
- **Fake LessonRepository:** Returns minimal AGSL (`half4 main(float2 fc) { return half4(1); }`) for CI emulator
- **Nav flow tests:** DreamsTestRunner ensures nav routes resolve, back-stack works (deferred: full UI assertions)

### No Global State Mutation
- **LessonRegistry:** `internal object` with idempotency guard; never reset between tests
- **Test isolation:** Each test app instance (via TestDreamsApp) gets fresh Koin graph
- **No singletons in tests:** Koin reloads modules per test class (via `startKoin`)

---

## Code Organization by Concern

| Concern | Package | Key Files |
|---------|---------|-----------|
| **DI** | `core/di` | AppModule, DataModule, FeatureModule, KoinModulesCheckTest |
| **AGSL** | `core/agsl` | RuntimeShader utils, shader sources (assets) |
| **Motion** | `core/motion` | Reduced-motion logic, animation spec resolution |
| **Lesson data** | `data/lesson` | LessonRepositoryImpl, Lesson entity, lesson sources (Basics, SDF, Noise, PostEffect, Showcase), showcases() accessor |
| **Prefs** | `data/prefs` | UserPrefsRepositoryImpl, UserPrefs data class, ThemeMode enum |
| **Domain interfaces** | `domain/lesson` | LessonRepository interface (sealed, impl hidden) |
| **Navigation shell** | `ui/feature/nav` | MainShell, TopLevelBackStack, DreamsBottomBar, TabKey, Route sealed interface |
| **Lesson screens** | `ui/feature/lessonlist` | LessonCategoriesScreen/VM/UiState, LessonListScreen/VM/UiState |
| **Lesson detail** | `ui/feature/lesson` | LessonDetailScreen, LessonDetailViewModel, LessonDetailUiState |
| **Showcase screens** | `ui/feature/showcase` | ShowcaseListScreen/VM/UiState, ShowcaseScreen/VM/UiState |
| **Settings** | `ui/feature/settings` | SettingsScreen, DisplaySettingsSection, AboutAgslSheet |
| **Shared UI** | `ui/feature/common` | LessonCard (moved from gallery), SharedTransitionLayout helpers, animation specs |
| **Theme** | `ui/theme` | Oscilloscope Workbench Material3 schemes, Tokens.kt, typography, spacing |

---

## Error Handling & Resilience

### Lesson Loading Failures
- **Bootstrap:** `try-catch` in `LessonRegistry.bootstrap()`; logs error, proceeds with partial list
- **Lesson not found:** `byId()` returns null; UI shows placeholder or error toast

### DataStore Read Failures
- **Flow emission:** Exception logged, Flow doesn't emit; last-known state retained
- **Write failures:** `UserPrefsRepositoryImpl` wraps with try-catch, logs, doesn't rethrow (graceful degradation)

### Navigation Failures
- **Invalid route:** Navigation3 logs warning, doesn't navigate (back-stack unchanged)
- **Deep link:** Validated before routing; invalid deep links ignored

### Shader Compilation Failures
- **Invalid AGSL:** Fallback to error shader (solid red Canvas); error logged
- **Runtime crash:** Don't expose to UI; frame-drop recovery via Compose frame clock

---

## Performance Characteristics

| Aspect | Measurement | Target |
|--------|-------------|--------|
| **App startup** | Lesson bootstrap + first Gallery frame | <2s |
| **Lesson bootstrap** | Load 23 lessons from sources | <500ms |
| **Gallery tab switch** | Recompose + list layout | <100ms |
| **Slider update** | SnapshotStateMap write → frame render | <16ms (60 FPS) |
| **DataStore write** | Debounce + persist param override | <300ms (200ms debounce + 100ms write) |
| **Favorite toggle** | Toggle + DataStore persist | <100ms |

---

## Glossary

| Term | Definition |
|------|-----------|
| **Koin** | Service locator / DI framework; `startKoin { modules(...) }` |
| **ViewModel** | Lifecycle-aware state holder; survives config change |
| **SavedStateHandle** | Bundle passed to ViewModel; survives process death |
| **StateFlow** | Hot Flow; always has latest value, multicast |
| **UiState** | Immutable data class representing UI rendering state |
| **Navigation3** | Type-safe routing with `@Serializable` route classes |
| **DataStore** | Async-first preference storage; replaces SharedPreferences |
| **AGSL** | Android Graphics Shading Language (GLSL-like) |
| **Reduced-motion** | System setting (ANIMATOR_DURATION_SCALE) + app pref for disabling animations |
| **Shared-element** | Cross-screen Compose transition; both screens animate bounds |
| **SnapshotStateMap** | Compose snapshot-aware mutable map; triggers recompose on write |
