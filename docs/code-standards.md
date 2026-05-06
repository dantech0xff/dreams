# Dreams: Code Standards & Conventions

## Overview

This document establishes coding patterns, conventions, and architectural rules for the Dreams codebase. All new code must adhere to these standards.

---

## Kotlin & Android Conventions

### Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| **Packages** | Reverse domain + descriptor | `com.dantech.dreams.ui.feature.gallery` |
| **Classes** | PascalCase, descriptive | `GalleryViewModel`, `LessonDetailScreen` |
| **Objects** | PascalCase (Kotlin objects are singletons) | `LessonRegistry` |
| **Interfaces** | PascalCase, no `I` prefix | `LessonRepository`, `UserPrefsRepository` |
| **Functions** | camelCase, verb-noun | `toggleFavorite()`, `setParamValue()` |
| **Properties** | camelCase | `uiState`, `lessonId`, `selectedTabIndex` |
| **Constants** | UPPER_SNAKE_CASE | `PREFS_FILE`, `DEBOUNCE_MILLIS` |
| **Private fields** | camelCase with `_` prefix if mutable | `_ui` (MutableStateFlow), `_paramUpdateFlow` |
| **Composables** | PascalCase, noun-first | `LessonCard()`, `GalleryScreen()` |
| **Lambda params** | Single char (common) or descriptive | `{ lesson -> ... }`, `{ (key, value) -> ... }` |

### File Organization

**One public type per file.** Exception: Sealed interface + direct subtypes may share a file.

```
package com.dantech.dreams.ui.feature.gallery

// GalleryViewModel.kt
class GalleryViewModel(...) : ViewModel()

// GalleryUiState.kt
data class GalleryUiState(...)

// GalleryScreen.kt
@Composable
fun GalleryScreen(...)

// LessonCard.kt
@Composable
fun LessonCard(...)
```

---

## Architecture Patterns

### Package Layering

```
├── core/            (Low-level utilities & DI)
│   ├── agsl/        (AGSL shaders, RuntimeShader utils)
│   ├── di/          (Koin modules)
│   └── motion/      (Reduced-motion logic)
├── data/            (Repositories, entities, persistence)
│   ├── lesson/      (LessonRepositoryImpl, Lesson entity, sources)
│   └── prefs/       (UserPrefsRepositoryImpl, UserPrefs)
├── domain/          (Interfaces, business logic contracts)
│   └── lesson/      (LessonRepository interface)
└── ui/              (Composables, ViewModels, navigation)
    ├── feature/     (Feature screens: gallery, lesson, etc.)
    ├── theme/       (Tokens, colors, typography)
    └── common/      (Shared Composables, utilities)
```

**Rule:** Avoid cross-layer dependencies (no `ui` → `data`, no `data` → `ui`).
**Flow:** `ui/feature` → `domain` → `data` & `core`.

### ViewModel Pattern

Every screen owns a ViewModel exposing immutable `StateFlow<UiState>`:

```kotlin
class GalleryViewModel(
    private val repo: LessonRepository,
    private val prefs: UserPrefsRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    
    private val _ui = MutableStateFlow(buildInitial())
    val uiState: StateFlow<GalleryUiState> = _ui.asStateFlow()
    
    init {
        // Subscribe to prefs Flow, update _ui
        viewModelScope.launch {
            prefs.prefsFlow.collect { snapshot ->
                _ui.update { it.copy(favorites = snapshot.favorites) }
            }
        }
    }
    
    fun selectTab(index: Int) {
        _ui.update { it.copy(selectedTabIndex = index, lessons = repo.byCategory(...)) }
    }
    
    private fun buildInitial(): GalleryUiState { ... }
}
```

**Guidelines:**
- **One ViewModel per screen** (GalleryViewModel, LessonDetailViewModel, etc.)
- **Expose `StateFlow<UiState>` publicly**, everything else private
- **No mutable state in Compose** (no `remember { mutableStateOf(...) }` for complex state)
- **SavedStateHandle for keys** that must survive process death (e.g., selected tab index)
- **viewModelScope** for all async work (Flows, coroutines)
- **Never expose MutableStateFlow** — use `asStateFlow()` to expose read-only version

### UiState Pattern

Immutable data class representing screen rendering state:

```kotlin
data class GalleryUiState(
    val categories: ImmutableList<LessonCategory> = persistentListOf(),
    val selectedTabIndex: Int = 0,
    val lessons: ImmutableList<Lesson> = persistentListOf(),
    val favorites: ImmutableSet<String> = persistentSetOf(),
    val lastLessonId: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)
```

**Guidelines:**
- **Data class with `val`** — no mutable fields
- **Use Kotlin Immutable Collections** (`ImmutableList`, `ImmutableSet`, `ImmutableMap`) for API stability
- **Sealed interfaces for errors** (alternative to error: String? pattern):
  ```kotlin
  sealed interface GalleryUiState {
      data class Loaded(...) : GalleryUiState
      data class Error(val message: String) : GalleryUiState
  }
  ```
- **One UiState per ViewModel** (not multi-union types unless semantically clear)

### Repository Pattern

Interfaces in `domain/`, implementations in `data/`:

```kotlin
// domain/lesson/LessonRepository.kt
interface LessonRepository {
    fun allLessons(): List<Lesson>
    fun byCategory(category: LessonCategory): List<Lesson>
    fun byId(lessonId: String): Lesson?
}

// data/lesson/LessonRepositoryImpl.kt
class LessonRepositoryImpl : LessonRepository {
    // Use private singletons (LessonRegistry) for bootstrap
    override fun allLessons(): List<Lesson> = LessonRegistry.all
    override fun byCategory(category: LessonCategory): List<Lesson> = LessonRegistry.byCategory(category)
    override fun byId(lessonId: String): Lesson? = LessonRegistry.all.find { it.id == lessonId }
}

// Internal singleton (never expose directly)
internal object LessonRegistry {
    private val all = mutableListOf<Lesson>()
    
    init {
        bootstrap()
    }
    
    fun bootstrap() {
        if (all.isEmpty()) { // Idempotency guard
            // Load all 23 lessons from sources
        }
    }
}
```

**Guidelines:**
- **Interfaces in `domain/`**, implementations in `data/`
- **Singletons hidden behind interface** — no direct `object LessonRegistry` access from UI
- **Idempotency guard** in bootstrap (`if (all.isEmpty())`)
- **No suspend functions for read operations** (synchronous cached reads preferred for lesson data)
- **Suspend only for I/O** (DataStore writes: `toggleFavorite()`, `setParamOverride()`)

### Dependency Injection (Koin)

Three-module DI setup:

```kotlin
// core/di/AppModule.kt
val appModule = module {
    // Cross-cutting singletons (logging, dispatchers, etc.)
}

// core/di/DataModule.kt
val dataModule = module {
    single<DataStore<Preferences>> { ... }
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

// App.onCreate()
startKoin {
    androidContext(this@DreamsApp)
    modules(appModule, dataModule, featureModule)
}
```

**Guidelines:**
- **Three modules:** `appModule`, `dataModule`, `featureModule`
- **No cross-module interdependencies** (except `featureModule` depends on singletons from other modules via `get()`)
- **ViewModels with parameters:** Use Koin DSL `(param: Type) -> ViewModel(...)` syntax
- **Inject via constructor** (Koin handles resolution)
- **Test via `dataModule.verify()`** in unit tests (no Android context needed)

### DataStore Preferences

```kotlin
// data/prefs/UserPrefs.kt
data class UserPrefs(
    val lastLessonId: String = "",
    val favorites: Set<String> = emptySet(),
    val paramOverrides: Map<String, Map<String, Float>> = emptyMap(),
    val reducedMotionOverride: Boolean = false,
)

// data/prefs/UserPrefsRepository.kt
interface UserPrefsRepository {
    val prefsFlow: Flow<UserPrefs>
    suspend fun toggleFavorite(lessonId: String)
    suspend fun setLastLessonId(lessonId: String)
    suspend fun setParamOverride(lessonId: String, key: String, value: Float)
    suspend fun setReducedMotionOverride(enabled: Boolean)
}

// data/prefs/UserPrefsRepositoryImpl.kt
class UserPrefsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : UserPrefsRepository {
    
    companion object {
        private val LAST_LESSON_KEY = stringPreferencesKey("last_lesson_id")
        private val FAVORITES_KEY = stringPreferencesKey("favorites")
        // ... other keys
    }
    
    override val prefsFlow: Flow<UserPrefs> = dataStore.data
        .map { it.toUserPrefs() }
        .catch { emit(UserPrefs()) }
    
    override suspend fun toggleFavorite(lessonId: String) {
        dataStore.edit { prefs ->
            val current = prefs[FAVORITES_KEY]?.let { json.decodeFromString<Set<String>>(it) } ?: emptySet()
            val updated = if (lessonId in current) current - lessonId else current + lessonId
            prefs[FAVORITES_KEY] = json.encodeToString(updated)
        }
    }
}
```

**Guidelines:**
- **Flow-based reads** (`prefsFlow`) — hot, multicast, cached in-memory
- **Suspend writes** — atomic via DataStore.edit()
- **JSON serialization** for complex types (Set, Map) via kotlinx.serialization
- **Error handling:** Catch exceptions in Flow, emit default state, log
- **No blocking reads** — always use Flow

---

## Composable Conventions

### Screen Pattern

```kotlin
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel = koinViewModel(),
    onNavigateToLesson: (lessonId: String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = { GalleryTopAppBar() },
    ) { padding ->
        GalleryContent(
            uiState = uiState,
            onSelectTab = viewModel::selectTab,
            onToggleFavorite = viewModel::toggleFavorite,
            onNavigateToLesson = onNavigateToLesson,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
private fun GalleryContent(
    uiState: GalleryUiState,
    onSelectTab: (Int) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onNavigateToLesson: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Decompose large screens into smaller private composables
    // Pass state + callbacks explicitly
}
```

**Guidelines:**
- **Public screen composables take ViewModel** (injected via `koinViewModel()`)
- **Collect state with `collectAsState()`** — triggers recompose on state change
- **Pass state + callbacks to private composables** (no global state reads)
- **Use `Modifier` parameter** as last parameter (compose convention)
- **Private composables for breakdown** — keep public composables concise
- **No side effects in composables** — all side effects happen in ViewModel via coroutines

### Composable Modifiers

Apply modifiers left-to-right, from outermost to innermost:

```kotlin
@Composable
fun LessonCard(
    lesson: Lesson,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable { /* navigate */ }
            .sharedBounds(
                sharedContentState = rememberSharedContentState("lesson-card-${lesson.id}"),
                animatedVisibilityScope = this@AnimatedVisibilityScope,
            ),
    ) {
        // ...
    }
}
```

**Order:**
1. Size & layout (fillMaxWidth, height, etc.)
2. Spacing (padding, margin equivalents)
3. Visual effects (background, border, shadow)
4. Interaction (clickable, draggable)
5. Animation & transitions (sharedBounds, animateContentSize)

### State Hoisting

Always hoist mutable state to the parent that owns the behavior:

```kotlin
// Bad: State in composable
@Composable
fun BadTabs() {
    var selectedTab by remember { mutableStateOf(0) }
    // ...
}

// Good: State in ViewModel, passed as parameter
@Composable
fun GoodTabs(
    selectedTabIndex: Int,
    onSelectTab: (Int) -> Unit,
) {
    // Read selectedTabIndex, call onSelectTab
}
```

---

## Testing Conventions

### Unit Test Structure

```kotlin
class GalleryViewModelTest {
    
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private lateinit var viewModel: GalleryViewModel
    private lateinit var fakeRepo: FakeLessonRepository
    private lateinit var fakePrefs: FakeUserPrefsRepository
    
    @Before
    fun setUp() {
        fakeRepo = FakeLessonRepository()
        fakePrefs = FakeUserPrefsRepository()
        viewModel = GalleryViewModel(fakeRepo, fakePrefs, SavedStateHandle())
    }
    
    @Test
    fun selectTab_updatesSelectedTabIndex() = runTest {
        viewModel.selectTab(1)
        
        val state = viewModel.uiState.first()
        assertThat(state.selectedTabIndex).isEqualTo(1)
    }
    
    @Test
    fun toggleFavorite_persistsViaPrefs() = runTest {
        viewModel.toggleFavorite("lesson-123")
        
        advanceUntilIdle() // Wait for debounce/coroutine
        
        assertThat(fakePrefs.favorites).contains("lesson-123")
    }
}
```

**Guidelines:**
- **MainDispatcherRule** for Dispatch.Main = Dispatch.Unconfined in tests
- **Fake implementations** for repositories (no real DataStore in unit tests)
- **Turbine for Flow testing:** `viewModel.uiState.test { awaitItem(); expectNoEvents() }`
- **advanceUntilIdle()** to complete debounce / coroutine work
- **One test per behavior** — clear test names (selectTab_updatesSelectedTabIndex)

### Instrumented Test Setup

```kotlin
// src/androidTest/java/DreamsTestRunner.kt
class DreamsTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application {
        return super.newApplication(cl, TestDreamsApp::class.java.name, context)
    }
}

// src/androidTest/java/TestDreamsApp.kt
class TestDreamsApp : DreamsApp() {
    override fun onCreate() {
        super.onCreate()
        stopKoin() // Stop prod Koin
        startKoin {
            androidContext(this@TestDreamsApp)
            modules(
                appModule,
                dataModule, // Real DataStore, but...
                testFeatureModule, // Fake repositories
            )
        }
    }
}

// build.gradle.kts (app)
android {
    testInstrumentationRunner = "com.dantech.dreams.DreamsTestRunner"
}
```

**Guidelines:**
- **DreamsTestRunner** overrides newApplication to use TestDreamsApp
- **TestDreamsApp** injects fake repositories via alternate Koin module
- **Minimal AGSL stubs** in fake repos for CI emulator (no real shader execution)
- **Real DataStore** in instrumented tests (via `PreferenceDataStoreFactory.create(tempFile)`)

---

## Error Handling

### Exception Handling

```kotlin
// Repositories: catch, log, return safe default or null
override fun byId(lessonId: String): Lesson? = try {
    LessonRegistry.all.find { it.id == lessonId }
} catch (e: Exception) {
    Log.e(TAG, "Failed to fetch lesson $lessonId", e)
    null
}

// DataStore writes: catch, log, don't rethrow
override suspend fun toggleFavorite(lessonId: String) = try {
    dataStore.edit { prefs ->
        // ...
    }
} catch (e: Exception) {
    Log.e(TAG, "Failed to toggle favorite", e)
}

// Flows: emit default, don't crash
override val prefsFlow: Flow<UserPrefs> = dataStore.data
    .map { it.toUserPrefs() }
    .catch { e ->
        Log.e(TAG, "DataStore read failed", e)
        emit(UserPrefs())
    }
```

**Guidelines:**
- **Repositories: silent failures** (return null, log, don't throw)
- **Flows: emit default state** on error, log
- **ViewModel: no try-catch** — assume repos handle errors
- **UI: render error state** only if explicitly needed (most errors are logging-only)

### Logging

Use Android Log with TAG:

```kotlin
private companion object {
    private const val TAG = "GalleryViewModel"
}

Log.d(TAG, "Switching to tab $index")
Log.e(TAG, "Failed to load lessons", exception)
```

---

## Performance Guidelines

### Recomposition Optimization

- **Never pass unbound lambdas** to Composables (causes recomposition):
  ```kotlin
  // Bad
  LessonCard(lesson, onClick = { viewModel.selectLesson(lesson.id) })
  
  // Good
  LessonCard(lesson, onClick = { onSelectLesson(lesson.id) })
  ```

- **Use `.stable` on data classes** to help Compose:
  ```kotlin
  @Stable
  data class GalleryUiState(...)
  ```

- **Key Composables in Lists:**
  ```kotlin
  LazyColumn {
      items(lessons, key = { it.id }) { lesson ->
          LessonCard(lesson)
      }
  }
  ```

### Memory & Resource Usage

- **AGSL shaders:** Loaded once in LessonRegistry (bootstrap), cached
- **Bitmap caching:** Avoid loading lesson previews as bitmaps; use Canvas + AGSL
- **Flow subscriptions:** Always unsubscribe via viewModelScope (automatic in ViewModel)

### Frame Rate & Animation

- **Slider updates:** SnapshotStateMap writes are instant (Compose thread)
- **Persistence debounce:** 200ms (user finishes sliding) before DataStore write
- **Transitions:** Use `snap()` for reduced-motion, `spring()` otherwise (no linear animations)

---

## Code Style Guidelines

### Kotlin Idioms

```kotlin
// Use scope functions for initialization
val prefs = UserPrefs(
    lastLessonId = lesson.id,
    favorites = emptySet(),
).also { /* initialize if needed */ }

// Use when for sealed interfaces
when (route) {
    is Route.Landing -> LandingScreen()
    is Route.Gallery -> GalleryScreen()
    is Route.LessonDetail -> LessonDetailScreen(route.lessonId)
    is Route.Showcase -> ShowcaseScreen(route.lessonId)
}

// Destructuring in lambdas
paramUpdateFlow
    .collect { (key, value) ->
        prefs.setParamOverride(lessonId, key, value)
    }

// Use elvis operator
val category = lesson.category ?: LessonCategory.BASICS
```

### Imports

- Sort imports alphabetically
- Remove unused imports before commit
- Use star imports only for `kotlinx.serialization.*`

### Comments

- Explain "why", not "what" (code is self-documenting)
- Document public APIs, non-obvious logic
- Avoid commented-out code; use git history

---

## File Size Targets

| File Type | Max Lines | Guideline |
|-----------|-----------|-----------|
| **ViewModel** | 100 | One ViewModel per screen |
| **UiState** | 50 | Keep state simple, add sealed interfaces if complexity grows |
| **Repository** | 80 | One repo per domain entity (Lesson, Prefs) |
| **Screen Composable** | 100 | Decompose into private composables |
| **Test** | 150 | One test class per component |

If a file exceeds limits, split into focused modules.

---

## Continuous Integration & Commits

### Pre-commit
- Run lint: `./gradlew lint`
- Run tests: `./gradlew test`
- Verify no compile warnings

### Commit Messages

Use conventional commits:

```
feat(gallery): add favorites toggle with persistence

- Implement toggleFavorite() in GalleryViewModel
- Persist favorites via UserPrefsRepository + DataStore
- Add favorite icon UI to LessonCard

Closes #123
```

**Format:** `type(scope): message`
- **type:** `feat`, `fix`, `docs`, `refactor`, `test`, `chore`
- **scope:** affected module or feature
- **message:** imperative, lowercase, no period

### No Force Pushes
Push only to feature branches; let lead merge to main.

---

## Checklist for New Code

- [ ] Follows package layering (no cross-layer dependencies)
- [ ] ViewModel pattern applied (StateFlow<UiState>, no MutableState)
- [ ] Repositories hidden behind interface (no direct impl imports in UI)
- [ ] DI module added (if new singleton or viewModel)
- [ ] Tests written (unit + instrumented if UI)
- [ ] No global mutable state in app code
- [ ] Error handling: graceful fallbacks, logging
- [ ] File size under limits (split if needed)
- [ ] Naming conventions followed (PascalCase classes, camelCase functions)
- [ ] No commented-out code
- [ ] Lint passes: `./gradlew lint`
- [ ] Tests pass: `./gradlew test`

