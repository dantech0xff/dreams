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
│   ├── lesson/                     # LessonRepositoryImpl, Lesson entity, showcases() accessor
│   │   └── source/                 # Lesson sources (4 educational + 1 showcase categories)
│   │       ├── basics/             # 6 basic AGSL lessons
│   │       ├── sdf/                # 6 SDF lessons
│   │       ├── noise/              # 6 noise lessons
│   │       ├── posteffect/         # 5 post-effect lessons
│   │       └── showcase/           # 3 showcase demos
│   └── prefs/                      # UserPrefsRepositoryImpl + UserPrefs entity
├── domain/                         # Domain layer (interfaces only)
│   └── lesson/                     # LessonRepository interface
└── ui/                             # UI layer (Composables, ViewModels)
    ├── feature/                    # Feature screens (bottom-tab shell + per-tab screens)
    │   ├── nav/                    # Navigation shell: MainShell, TopLevelBackStack, DreamsBottomBar, TabKey, Route
    │   ├── lessonlist/             # LessonCategories + LessonList screens + VMs + UiStates
    │   ├── lesson/                 # LessonDetail screen + ViewModel
    │   ├── showcase/               # ShowcaseList + Showcase screens + VMs + UiStates
    │   ├── settings/               # Settings screen + AboutAgslSheet
    │   ├── common/                 # Shared Composables (LessonCard, transitions)
    │   └── (deleted: landing/, gallery/)
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
- `viewModel { LessonCategoriesViewModel(repo) }` — Category selection VM
- `viewModel { (categoryName) -> LessonListViewModel(repo, prefs, categoryName) }` — Parameterized lesson list VM
- `viewModel { (lessonId) -> LessonDetailViewModel(repo, prefs, lessonId) }` — Parameterized detail VM
- `viewModel { ShowcaseListViewModel(repo) }` — Showcase list VM
- `viewModel { (lessonId) -> ShowcaseViewModel(repo, lessonId) }` — Parameterized showcase VM

### Repositories (domain/ + data/)

**LessonRepository** (interface in domain/lesson/)
```kotlin
interface LessonRepository {
    fun allLessons(): List<Lesson>
    fun byCategory(category: LessonCategory): List<Lesson>
    fun byId(lessonId: String): Lesson?
    fun showcases(): List<Lesson>  // Returns lessons with SHOWCASE category
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
    @Serializable data object LessonCategoriesRoot : Route
    @Serializable data class LessonList(val categoryName: String) : Route
    @Serializable data class LessonDetail(val lessonId: String) : Route
    @Serializable data object ShowcaseListRoot : Route
    @Serializable data class Showcase(val lessonId: String) : Route
    @Serializable data object SettingsRoot : Route
}
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

**LessonCategoriesUiState**
- categories (4 educational), isLoading, error

**LessonListUiState**
- categoryName, lessons, favorites, lastLessonId, isLoading, error

**LessonDetailUiState**
- lesson, paramValues (SnapshotStateMap), isLoading, error

**ShowcaseListUiState**
- showcases (3 items), isLoading, error

**ShowcaseUiState**
- lesson, paramValues, isLoading, error

### Composables (ui/feature/)

**Screens** (public, injectable ViewModel)
- `LessonCategoriesScreen()` — Selectable list of 4 lesson categories
- `LessonListScreen(categoryName)` — Tabbed or flat list of lessons in category
- `LessonDetailScreen(lessonId)` — Full-screen shader + interactive sliders
- `ShowcaseListScreen()` — List of 3 showcase demos
- `ShowcaseScreen(lessonId)` — Full-screen interactive demo
- `SettingsScreen()` — Settings page (reduced-motion toggle, app info, GitHub link, license)

**Components** (private or shared, state hoisted)
- `LessonCard()` — Lesson card with preview + title + favorite toggle (moved to common/)
- `AboutAgslSheet()` — Bottom sheet explaining AGSL (moved to settings/)
- `DreamsBottomBar()` — 3-tab navigation bar (Lesson | Showcase | Settings)
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
| **DI Setup** | core/di | 3 modules (app, data, feature); includes new lesson/showcase/settings VMs |
| **Lesson Data** | data/lesson + domain/lesson | Repo interface + impl + 26 lessons + showcases() accessor |
| **Preferences** | data/prefs | UserPrefs entity + repo interface + impl |
| **Navigation Shell** | ui/feature/nav | MainShell, TopLevelBackStack, DreamsBottomBar, TabKey, Route (3-tab bottom nav) |
| **Lesson Screens** | ui/feature/lessonlist | LessonCategoriesScreen/VM/UiState, LessonListScreen/VM/UiState |
| **Lesson Detail** | ui/feature/lesson | LessonDetailScreen, ViewModel, UiState |
| **Showcase Screens** | ui/feature/showcase | ShowcaseListScreen/VM/UiState, ShowcaseScreen/VM/UiState |
| **Settings** | ui/feature/settings | SettingsScreen, AboutAgslSheet (moved from landing) |
| **Shared UI** | ui/feature/common | LessonCard (moved from gallery), transitions, animations |
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
- **DI:** `app/src/main/java/com/dantech/dreams/core/di/*.kt` (FeatureModule has 5 VMs)
- **Navigation Shell:** `app/src/main/java/com/dantech/dreams/ui/feature/nav/` (MainShell, TopLevelBackStack, DreamsBottomBar, Route)
- **Lesson Screens:** `app/src/main/java/com/dantech/dreams/ui/feature/lessonlist/`
- **Showcase Screens:** `app/src/main/java/com/dantech/dreams/ui/feature/showcase/`
- **Settings:** `app/src/main/java/com/dantech/dreams/ui/feature/settings/`
- **Lessons Data:** `app/src/main/java/com/dantech/dreams/data/lesson/source/`
- **Tests:** `app/src/test/` and `app/src/androidTest/` (updated to remove GalleryViewModelTest, LandingViewModelTest)
- **Standards:** See `docs/code-standards.md`
- **Architecture:** See `docs/system-architecture.md`

---

## References

- **Bottom Tab Navigation Rework Plan:** `plans/260506-0724-bottom-tab-navigation-rework/plan.md`
- **Code Review Report:** `plans/reports/code-reviewer-260506-0759-bottom-tab-rework.md`
- **Android AGSL Docs:** https://developer.android.com/develop/ui/views/graphics/agsl
- **Compose Navigation3:** https://developer.android.com/jetpack/compose/navigation
- **Koin:** https://insert-koin.io/
- **DataStore:** https://developer.android.com/jetpack/androidx/releases/datastore
