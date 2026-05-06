# Dreams: System Architecture

## High-Level System Diagram

```
┌─────────────────────────────────────────────────────────┐
│              Compose UI Layer (Feature)                 │
├─────────────────────────────────────────────────────────┤
│ Landing | Gallery | LessonDetail | Showcase | Settings  │
│ (Each owns a ViewModel + UiState)                        │
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
│                      │              │                 │
│ UserPrefsRepoImpl     │              │ Motion          │
│ • DataStore backing  │              │ • Reduced-motion│
│ • Prefs flow         │              │ • Tween → snap  │
└───────────┬──────────┘              └────────────────┘
            │
    ┌───────▴──────────┐
    │  DataStore Prefs │
    │  (dreams_prefs)  │
    │  • lastLessonId  │
    │  • favorites     │
    │  • paramOverride │
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
    viewModel { LandingViewModel() }
    viewModel { GalleryViewModel(get(), get(), get()) }
    viewModel { (lessonId: String) -> LessonDetailViewModel(get(), get(), lessonId) }
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

## Navigation (Navigation3 1.1.1)

### Route Definition
```kotlin
sealed interface Route : NavKey {
    @Serializable
    data object Landing : Route
    
    @Serializable
    data object Gallery : Route
    
    @Serializable
    data class LessonDetail(val lessonId: String) : Route
    
    @Serializable
    data class Showcase(val lessonId: String) : Route
}

fun routeForLessonId(id: String): Route =
    if (id.startswith("showcase-")) Route.Showcase(id) else Route.LessonDetail(id)
```

### Navigation Flow
1. **Landing** (entry point) → tap → **Gallery**
2. **Gallery** (tab-swipeable) → tap card → **LessonDetail** (via shared-element transition)
3. **LessonDetail** → back → **Gallery** (at last-viewed tab + scroll position)
4. **Gallery** → tap showcase card → **Showcase** (fullscreen, immediate AGSL)
5. **Showcase** → back or tap → **Gallery**

### State Preservation
- **SavedStateHandle:** GalleryViewModel stores `selectedTabIndex` key; recovers tab position on config change
- **rememberNavBackStack:** Custom composable manages back-stack across destination changes
- **rememberSaveableStateHolderNavEntryDecorator:** Preserves nested Compose state per nav entry
- **Process death:** Navigation state + ViewModel state restored via system Bundle serialization

---

## ViewModel & State Management

### Per-Feature ViewModel Pattern

Each screen owns a ViewModel exposing `StateFlow<UiState>`:

#### LandingViewModel
```kotlin
val uiState: StateFlow<LandingUiState>
// Minimal state: ready, loading, error
```

#### GalleryViewModel
```kotlin
val uiState: StateFlow<GalleryUiState>
// State: selectedTabIndex, categories, lessons, favorites, lastLessonId
// Actions: selectTab(index), toggleFavorite(lessonId)
```

#### LessonDetailViewModel
```kotlin
val uiState: StateFlow<LessonDetailUiState>
// State: lesson, paramValues (SnapshotStateMap), lastLessonId update
// Actions: setParamValue(key, value) [debounced], markLessonViewed()
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
    suspend fun toggleFavorite(lessonId: String)
    suspend fun setLastLessonId(lessonId: String)
    suspend fun setParamOverride(lessonId: String, key: String, value: Float)
    suspend fun setReducedMotionOverride(enabled: Boolean)
}

// Backed by DataStore<Preferences>
// Keys: "last_lesson_id", "favorites" (JSON array), "param_overrides_{lessonId}" (JSON), "reduced_motion"
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
    val lastLessonId: String = "",
    val favorites: Set<String> = emptySet(),
    val paramOverrides: Map<String, Map<String, Float>> = emptyMap(),
    val reducedMotionOverride: Boolean = false,
)
```

### Serialization
- **Favorites:** JSON-encoded `Set<String>` via kotlinx.serialization
- **paramOverrides:** JSON-encoded `Map<lessonId, Map<paramKey, value>>` (per-lesson shader uniforms)
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
| **Lesson data** | `data/lesson` | LessonRepositoryImpl, Lesson entity, lesson sources (Basics, SDF, Noise, PostEffect, Showcase) |
| **Prefs** | `data/prefs` | UserPrefsRepositoryImpl, UserPrefs data class |
| **Domain interfaces** | `domain/lesson` | LessonRepository interface (sealed, impl hidden) |
| **Navigation** | `ui/feature/nav` | Route sealed interface, routeForLessonId(), NavDisplay, back-stack logic |
| **UI Features** | `ui/feature/{landing,gallery,lesson,showcase,settings}` | Screens, ViewModels, UiState classes |
| **Shared UI** | `ui/feature/common` | LessonCard, SharedTransitionLayout helpers, animation specs |
| **Theme** | `ui/theme` | Tokens.kt, colors, typography, spacing, Material3 defaults |

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

