# Dreams: Codebase Summary

Generated from repomix analysis of the Dreams Android Kotlin app.

---

## Project Structure

**Single-module Gradle app** with layered package architecture:

```
app/src/main/java/com/dantech/dreams/
├── core/                           # Low-level utilities & DI
│   ├── agsl/                       # AGSL shader execution utils
│   ├── di/                         # Koin DI modules (3 total)
│   └── motion/                     # Motion & reduced-motion logic
├── data/                           # Data layer (repos, entities, persistence)
│   ├── lesson/                     # LessonRepositoryImpl, Lesson entity
│   │   └── source/                 # Lesson sources (6 categories × 19 lessons + 3 showcases)
│   │       ├── basics/             # 6 basic AGSL lessons
│   │       ├── sdf/                # 6 SDF lessons
│   │       ├── noise/              # 6 noise lessons
│   │       ├── posteffect/         # 5 post-effect lessons
│   │       └── showcase/           # 3 showcase demos
│   └── prefs/                      # UserPrefsRepositoryImpl + UserPrefs entity
├── domain/                         # Domain layer (interfaces only)
│   └── lesson/                     # LessonRepository interface
└── ui/                             # UI layer (Composables, ViewModels)
    ├── feature/                    # Feature screens (4 screens + settings)
    │   ├── landing/                # Landing screen + LandingViewModel
    │   ├── gallery/                # Gallery screen + GalleryViewModel + GalleryUiState
    │   ├── lesson/                 # LessonDetail screen + LessonDetailViewModel
    │   ├── showcase/               # Showcase screen + ShowcaseViewModel
    │   ├── settings/               # Settings bottom sheet
    │   ├── common/                 # Shared Composables (LessonCard, transitions)
    │   └── nav/                    # Navigation (Route sealed interface, nav logic)
    └── theme/                      # Design tokens (Tokens.kt, colors, typography)

app/src/test/java/com/dantech/dreams/        # JVM unit tests (mirror structure)
app/src/androidTest/java/com/dantech/dreams/ # Instrumented tests + test runner
```

---

## Core Technologies

| Layer | Library | Version | Purpose |
|-------|---------|---------|---------|
| **DI** | Koin | 4.2.0 (BOM) | Service locator, ViewModel scoping |
| **Navigation** | Navigation3 | 1.1.1 | Type-safe routing with `@Serializable` |
| **UI** | Jetpack Compose | Material3 (BOM 2026.02.01) | Declarative UI, Material Design 3 |
| **State** | Kotlin Flow | Stdlib | Reactive state management |
| **Persistence** | DataStore Preferences | AndroidX | Async preference storage |
| **Serialization** | kotlinx.serialization | Stdlib | JSON encoding/decoding for prefs |
| **Testing** | JUnit + Turbine | 4.13 + 1.x | Unit testing, Flow assertions |
| **AGSL** | RuntimeShader | Android 13+ API | GPU fragment shader execution |

---

## Key Types & Contracts

### DI Modules (core/di/)

**AppModule.kt**
- Currently empty; placeholder for cross-cutting singletons (logging, dispatchers)

**DataModule.kt**
- `single<DataStore<Preferences>>` — File-backed preferences (`dreams_prefs`)
- `single<LessonRepository>` — Resolves LessonRepositoryImpl()
- `single<UserPrefsRepository>` — Resolves UserPrefsRepositoryImpl(dataStore)

**FeatureModule.kt**
- `viewModel { LandingViewModel() }` — Landing screen VM
- `viewModel { GalleryViewModel(repo, prefs, handle) }` — Gallery VM (3 constructor deps)
- `viewModel { (lessonId) -> LessonDetailViewModel(repo, prefs, lessonId) }` — Parameterized VM
- `viewModel { (lessonId) -> ShowcaseViewModel(repo, lessonId) }` — Parameterized VM

### Repositories (domain/ + data/)

**LessonRepository** (interface in domain/lesson/)
```kotlin
interface LessonRepository {
    fun allLessons(): List<Lesson>
    fun byCategory(category: LessonCategory): List<Lesson>
    fun byId(lessonId: String): Lesson?
}
```

**LessonRepositoryImpl** (data/lesson/)
- Wraps internal `LessonRegistry` singleton
- Idempotency guard: `if (all.isEmpty()) bootstrap()`
- Loads 23 lessons from 5 source categories on first instantiation

**UserPrefsRepository** (interface in domain/lesson/, impl in data/prefs/)
```kotlin
interface UserPrefsRepository {
    val prefsFlow: Flow<UserPrefs>  // Hot, multicast
    suspend fun toggleFavorite(lessonId: String)
    suspend fun setLastLessonId(lessonId: String)
    suspend fun setParamOverride(lessonId: String, key: String, value: Float)
    suspend fun setReducedMotionOverride(enabled: Boolean)
}
```

**UserPrefs** (data class, data/prefs/)
```kotlin
data class UserPrefs(
    val lastLessonId: String = "",
    val favorites: Set<String> = emptySet(),
    val paramOverrides: Map<String, Map<String, Float>> = emptyMap(),
    val reducedMotionOverride: Boolean = false,
)
```

### Lesson Entity (data/lesson/)

**Lesson**
```kotlin
data class Lesson(
    val id: String,                    // "basics-001", "showcase-liquid-glass"
    val category: LessonCategory,      // enum: BASICS, SDF, NOISE, POST_EFFECT, SHOWCASE
    val title: String,
    val description: String,
    val agslCode: String,              // AGSL 460 source
    val uniforms: List<Uniform>,       // Interactive parameters
)

data class Uniform(
    val name: String,
    val type: String,                  // "float", "int", "half4"
    val defaultValue: Float,
    val min: Float = 0f,
    val max: Float = 1f,
)
```

### Navigation Routes (ui/feature/nav/)

**Route** (sealed interface, @Serializable)
```kotlin
sealed interface Route : NavKey {
    @Serializable data object Landing : Route
    @Serializable data object Gallery : Route
    @Serializable data class LessonDetail(val lessonId: String) : Route
    @Serializable data class Showcase(val lessonId: String) : Route
}

fun routeForLessonId(id: String): Route =
    if (id.startsWith("showcase-")) Route.Showcase(id) else Route.LessonDetail(id)
```

### ViewModels (ui/feature/{feature}/)

Each ViewModel exposes `StateFlow<XUiState>` and handles user actions:

**GalleryViewModel**
- Manages tabs, lesson list, favorites
- `selectTab(index)`, `toggleFavorite(lessonId)`
- Subscribes to prefs.prefsFlow, updates UI on change
- SavedStateHandle: persists selected tab across config change

**LessonDetailViewModel**
- Manages lesson display + shader parameter sliders
- `setParamValue(key, value)` — local state update + debounced DataStore persist (200ms)
- Parameter updates via MutableSharedFlow → debounce → persist pipeline

**ShowcaseViewModel**
- Simple VM for full-screen showcase demos
- `setParamValue(key, value)` — local state update

**LandingViewModel**
- Minimal state; navigation is primary

### UiState Classes (ui/feature/{feature}/)

Immutable state classes used by Composables:

**GalleryUiState**
- categories, selectedTabIndex, lessons, favorites, lastLessonId, isLoading, error

**LessonDetailUiState**
- lesson, paramValues (SnapshotStateMap), isLoading, error

**ShowcaseUiState**
- lesson, paramValues, isLoading, error

**LandingUiState**
- Minimal (ready, loading, error)

### Composables (ui/feature/)

**Screens** (public, injectable ViewModel)
- `GalleryScreen()` — Tabbed gallery of lessons
- `LessonDetailScreen()` — Full-screen shader + sliders
- `ShowcaseScreen()` — Full-screen demo
- `LandingScreen()` — Onboarding

**Components** (private or shared, state hoisted)
- `LessonCard()` — Gallery card (sharedBounds animation)
- `SettingsSheet()` — ModalBottomSheet with reduced-motion toggle
- Shared animation specs, motion utilities

---

## Data & Persistence

### DataStore

**File:** `dreams_prefs` (in app's private data dir)

**Keys (stringPreferencesKey):**
- `"last_lesson_id"` — Last viewed lesson
- `"favorites"` — JSON-encoded Set<String>
- `"param_overrides_{lessonId}"` — JSON-encoded Map<String, Float>
- `"reduced_motion"` — Boolean

**Access pattern:**
- **Read:** `prefsFlow.collect { snapshot -> ... }` (hot Flow, cached)
- **Write:** Suspend functions in UserPrefsRepository (atomic via dataStore.edit())
- **Error handling:** Flow catches, emits default UserPrefs(), logs

### Lesson Bootstrap

**Path:** data/lesson/source/

Lessons loaded once on app startup via LessonRepositoryImpl init:

- **basics/:** 6 lessons (uniforms, fragCoord, gradients, polar, waves, patterns)
- **sdf/:** 6 lessons (circle, rounded box, metaballs, breathing grid, combine, invert)
- **noise/:** 6 lessons (hash, value noise, fBM, voronoi, plasma, lava)
- **posteffect/:** 5 lessons (blur, aberration, ripple-tap, dissolve, glass)
- **showcase/:** 3 demos (liquid glass, aurora, raymarched sphere)

**Total:** 26 lessons (23 educational + 3 showcases)

---

## Testing

### Unit Tests (app/src/test/)

**KoinModulesCheckTest** (core/di/)
- `dataModule.verify()` — DI resolution validation
- Koin configuration check

**VM Tests** (ui/feature/)
- Per-feature ViewModel tests
- Turbine + UnconfinedTestDispatcher
- FakeLessonRepository + FakeUserPrefsRepository

**Data Layer Tests** (data/)
- LessonRepository behavior
- UserPrefsRepository (real DataStore via tempFile)

### Instrumented Tests (app/src/androidTest/)

**DreamsTestRunner** (extends AndroidJUnitRunner)
- Overrides newApplication() → TestDreamsApp

**TestDreamsApp**
- startKoin with fake LessonRepository (minimal AGSL stubs for CI emulator)
- Real DataStore (via tempFile)

**Nav Flow Tests** (deferred; scaffolding in place)
- Route resolution, back-stack navigation

---

## Performance & Constraints

| Aspect | Constraint | Reason |
|--------|-----------|--------|
| **Min SDK** | 33 | RuntimeShader requirement |
| **Target SDK** | 36 | Latest stable |
| **JVM** | 11+ | Coroutines, Flow, Collections |
| **Lesson bootstrap** | <500ms | 23 lessons loaded on app start |
| **Slider response** | <16ms | SnapshotStateMap per-frame update (60 FPS) |
| **Persistence debounce** | 200ms | Slider stops → persist to DataStore |
| **AGSL execution** | GPU (Android 13+) | ShaderBrush via RuntimeShader |
| **Reduced-motion** | System + app pref | Disables animated transitions (snap only) |

---

## Dependencies (build.gradle.kts)

**Key versioning:**
- Material3 BOM: `2026.02.01`
- Compose Runtime: Latest (from BOM)
- Koin: 4.2.0 (BOM)
- Navigation3: 1.1.1
- DataStore: Latest AndroidX
- kotlinx.serialization: Latest Kotlin stdlib

---

## Code Organization by Concern

| Concern | Package | Notes |
|---------|---------|-------|
| **DI Setup** | core/di | 3 modules (app, data, feature) |
| **Lesson Data** | data/lesson + domain/lesson | Repo interface + impl + 26 lessons |
| **Preferences** | data/prefs | UserPrefs entity + repo interface + impl |
| **Navigation** | ui/feature/nav | Route sealed interface + nav logic |
| **Screens** | ui/feature/{landing,gallery,lesson,showcase} | Composables + VMs + UiState |
| **Settings** | ui/feature/settings | Reduced-motion toggle |
| **Shared UI** | ui/feature/common | LessonCard, transitions, animations |
| **Theme** | ui/theme | Tokens, colors, typography |
| **Core Utils** | core/agsl + core/motion | AGSL RuntimeShader utils, motion logic |

---

## Known Limitations

1. **Instrumented tests incomplete** — UI test assertions scaffolded but not fully implemented (deferred per plan)
2. **No multi-module** — Single app module; future feature modules possible
3. **AGSL stubs on CI** — Real shader execution only on physical device or real emulator

---

## Getting Started for Developers

### Build & Run
```bash
./gradlew :app:installDebug
```

### Run Tests
```bash
./gradlew test              # JVM unit tests
./gradlew connectedDebugAndroidTest  # Instrumented tests
```

### Key Files to Know
- **DI:** `app/src/main/java/com/dantech/dreams/core/di/*.kt`
- **Navigation:** `app/src/main/java/com/dantech/dreams/ui/feature/nav/Route.kt`
- **Lessons:** `app/src/main/java/com/dantech/dreams/data/lesson/source/`
- **Tests:** `app/src/test/` and `app/src/androidTest/`
- **Standards:** See `docs/code-standards.md`
- **Architecture:** See `docs/system-architecture.md`

---

## References

- **Refactor Plan:** `plans/260506-0052-production-refactor-koin-nav3/plan.md`
- **Android AGSL Docs:** https://developer.android.com/develop/ui/views/graphics/agsl
- **Compose Navigation3:** https://developer.android.com/jetpack/compose/navigation
- **Koin:** https://insert-koin.io/
- **DataStore:** https://developer.android.com/jetpack/androidx/releases/datastore
