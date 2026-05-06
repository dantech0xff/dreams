# Phase 07 — Testing Scaffold

## 1. Context Links
- Parent: [plan.md](plan.md)
- Depends on: [phase-02](phase-02-koin-di-bootstrap.md), [phase-03](phase-03-viewmodels-and-uistate.md), [phase-04](phase-04-navigation3-migration.md), [phase-05](phase-05-datastore-prefs.md), [phase-06](phase-06-shared-element-and-motion.md)
- Inputs: `research/researcher-01-koin-vm-datastore.md` §4
- Docs: https://cashapp.github.io/turbine/ | https://insert-koin.io/docs/reference/koin-test/checkmodules | https://developer.android.com/jetpack/compose/testing

## 2. Overview
- **Date:** 2026-05-06
- **Description:** Stand up a real test scaffold. Add `MainCoroutineRule`, fakes for `LessonRepository` and `UserPrefsRepository`, Turbine-based VM tests, Koin `checkModules()` test, and Compose UI tests for landing→gallery→detail flow + favorite toggle persistence + reduced-motion path.
- **Priority:** P1 (locks the refactor)
- **Implementation status:** pending
- **Review status:** pending

## 3. Key Insights
- Skip shader screenshot tests (locked decision; brittle pixel diff).
- VM tests run on JVM. Compose UI tests run on emulator. `LessonRepositoryImpl` constructor calls `LessonRegistry.bootstrap()` which references `RuntimeShader` only inside `validateAll` — so `bootstrap()` itself is JVM-safe (verified scout: bootstrap singletons just register lesson metadata; no Android API calls).
- Fake `UserPrefsRepository` exposes `MutableStateFlow<UserPrefs>` — tests drive emissions directly.
- Koin `checkModules()` requires `androidContext()` mock; use `mockk<Context>(relaxed = true)` or skip Android-context-dependent singles by splitting `dataModule` into `dataModuleAndroid` (DataStore) and `dataModuleJvm` (LessonRepository) so tests load only JVM-pure half.
- Compose UI tests use `createAndroidComposeRule<MainActivity>()` with `loadKoinModules()` override for fakes.

## 4. Requirements

### Functional Coverage
- **VM tests:** `GalleryViewModelTest`, `LessonDetailViewModelTest`, `ShowcaseViewModelTest`, `LandingViewModelTest`. For each: initial state, key transition, persistence write triggered.
- **Repo tests:** `LessonRepositoryImplTest` (renamed from existing registry test, expanded), `UserPrefsRepositoryImplTest` (write→read round-trip, JSON codec, favorites toggle).
- **Module tests:** `KoinModulesCheckTest` confirms graph resolves.
- **UI tests (`androidTest`):** `NavFlowUiTest` (landing→gallery→detail), `FavoriteTogglePersistenceUiTest`, `ReducedMotionPathUiTest`.

### Non-Functional
- All unit tests run via `./gradlew test` in <30s.
- All instrumented tests run via `./gradlew connectedDebugAndroidTest` in <2min.
- No flaky tests on first 10 runs.

## 5. Architecture

```
test/                           androidTest/
├── core/                       ├── ui/feature/
│   └── di/                     │   └── nav/
│       └── KoinModulesCheckTest│       └── NavFlowUiTest
├── data/                       ├── ui/feature/gallery/
│   ├── lesson/                 │   └── FavoriteTogglePersistenceUiTest
│   │   └── LessonRepositoryImplTest
│   └── prefs/                  └── ui/feature/motion/
│       ├── UserPrefsRepositoryImplTest      └── ReducedMotionPathUiTest
│       └── ParamOverridesCodecTest
├── ui/feature/
│   ├── gallery/GalleryViewModelTest
│   ├── lesson/LessonDetailViewModelTest
│   ├── showcase/ShowcaseViewModelTest
│   └── landing/LandingViewModelTest
└── support/
    ├── MainCoroutineRule.kt
    ├── FakeLessonRepository.kt
    └── FakeUserPrefsRepository.kt
```

## 6. Related Code Files

### Modify
- `app/src/test/java/com/dantech/dreams/data/lesson/LessonRegistryTest.kt` → rename to `LessonRepositoryImplTest.kt`, expand with new repo interface
- `core/di/DataModule.kt` — split into `dataModuleAndroid` (DataStore single) and `dataModuleJvm` (LessonRepository) for testability — OPTIONAL, only if checkModules pain forces it

### Create
- `app/src/test/java/com/dantech/dreams/support/MainCoroutineRule.kt`
- `app/src/test/java/com/dantech/dreams/support/FakeLessonRepository.kt`
- `app/src/test/java/com/dantech/dreams/support/FakeUserPrefsRepository.kt`
- `app/src/test/java/com/dantech/dreams/core/di/KoinModulesCheckTest.kt`
- `app/src/test/java/com/dantech/dreams/data/prefs/UserPrefsRepositoryImplTest.kt`
- `app/src/test/java/com/dantech/dreams/data/prefs/ParamOverridesCodecTest.kt`
- `app/src/test/java/com/dantech/dreams/ui/feature/gallery/GalleryViewModelTest.kt`
- `app/src/test/java/com/dantech/dreams/ui/feature/lesson/LessonDetailViewModelTest.kt`
- `app/src/test/java/com/dantech/dreams/ui/feature/showcase/ShowcaseViewModelTest.kt`
- `app/src/test/java/com/dantech/dreams/ui/feature/landing/LandingViewModelTest.kt`
- `app/src/androidTest/java/com/dantech/dreams/ui/feature/nav/NavFlowUiTest.kt`
- `app/src/androidTest/java/com/dantech/dreams/ui/feature/gallery/FavoriteTogglePersistenceUiTest.kt`
- `app/src/androidTest/java/com/dantech/dreams/ui/feature/motion/ReducedMotionPathUiTest.kt`
- `app/src/androidTest/java/com/dantech/dreams/support/TestDreamsApp.kt` — Test `Application` overriding modules with fakes

### Delete
- `app/src/test/java/com/dantech/dreams/ExampleUnitTest.kt` (stub, not useful)
- `app/src/androidTest/java/com/dantech/dreams/ExampleInstrumentedTest.kt` (stub)

## 7. Implementation Steps

1. **Create `MainCoroutineRule.kt`:**
   ```kotlin
   class MainCoroutineRule(val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
       override fun starting(d: Description) { Dispatchers.setMain(dispatcher) }
       override fun finished(d: Description) { Dispatchers.resetMain() }
   }
   ```

2. **Create `FakeLessonRepository.kt`:**
   ```kotlin
   class FakeLessonRepository(seed: List<LessonModel> = sampleLessons()) : LessonRepository {
       private val all = seed.toImmutableList()
       override fun all() = all
       override fun byCategory(c: LessonCategory) = all.filter { it.category == c }.toImmutableList()
       override fun byId(id: String) = all.firstOrNull { it.id == id }
       override fun validate() = emptyList<Pair<String, String>>()
   }
   private fun sampleLessons() = listOf(
       LessonModel("test-basics-1", "Test Basics", LessonCategory.BASICS, 1, "intro", "uniform float time;"),
       LessonModel("showcase-test", "Test Showcase", LessonCategory.SHOWCASE, 1, "intro", "uniform float time;"),
   )
   ```

3. **Create `FakeUserPrefsRepository.kt`:**
   ```kotlin
   class FakeUserPrefsRepository(initial: UserPrefs = UserPrefs.DEFAULT) : UserPrefsRepository {
       private val state = MutableStateFlow(initial)
       override val prefsFlow: Flow<UserPrefs> = state.asStateFlow()
       override suspend fun setLastLessonId(id: String) { state.update { it.copy(lastLessonId = id) } }
       override suspend fun toggleFavorite(id: String): Boolean {
           val now = id !in state.value.favorites
           state.update { it.copy(favorites = if (now) it.favorites + id else it.favorites - id) }
           return now
       }
       override suspend fun setParamOverride(lessonId: String, uniform: String, value: Float) {
           state.update { p ->
               val cur = p.paramOverrides.toMutableMap()
               cur[lessonId] = (cur[lessonId].orEmpty() + (uniform to value))
               p.copy(paramOverrides = cur)
           }
       }
       override suspend fun clearLessonOverrides(lessonId: String) {
           state.update { it.copy(paramOverrides = it.paramOverrides - lessonId) }
       }
       override suspend fun setReducedMotion(b: Boolean) { state.update { it.copy(reducedMotionOverride = b) } }
   }
   ```

4. **`GalleryViewModelTest.kt`** (Turbine pattern):
   ```kotlin
   class GalleryViewModelTest {
       @get:Rule val main = MainCoroutineRule()
       private val repo = FakeLessonRepository()
       private val prefs = FakeUserPrefsRepository()
       @Test fun selectTab_updatesLessons() = runTest {
           val vm = GalleryViewModel(repo, prefs, SavedStateHandle())
           vm.uiState.test {
               val initial = awaitItem()
               assertEquals(0, initial.selectedTabIndex)
               vm.selectTab(LessonCategory.SHOWCASE.ordinal)
               val next = awaitItem()
               assertEquals(LessonCategory.SHOWCASE, next.categories[next.selectedTabIndex])
               assertTrue(next.lessons.any { it.id == "showcase-test" })
           }
       }
   }
   ```

5. **`LessonDetailViewModelTest.kt`:**
   ```kotlin
   @Test fun setFloat_persistsToRepo() = runTest {
       val savedState = SavedStateHandle(mapOf("lessonId" to "test-basics-1"))
       val vm = LessonDetailViewModel(repo, prefs, savedState)
       vm.setFloat("amplitude", 0.5f)
       runCurrent()
       val cur = prefs.prefsFlow.first()
       assertEquals(0.5f, cur.paramOverrides["test-basics-1"]?.get("amplitude"))
   }
   ```

6. **`UserPrefsRepositoryImplTest.kt`** — uses real `PreferenceDataStoreFactory.create(produceFile = { File(tempDir, "test.preferences_pb") })` in `@Rule TemporaryFolder`. Round-trip favorites + overrides + reduced-motion. Reload new instance from same file → state survives.

7. **`ParamOverridesCodecTest.kt`** — encode/decode with empty map, single entry, nested. Malformed JSON → `decodeOverrides("{not json")` returns empty map (not throw).

8. **`KoinModulesCheckTest.kt`** — load `appModule + dataModuleJvm + featureModule` (omit Android-context single via split if needed):
   ```kotlin
   @Test fun graphResolves() {
       koinApplication {
           modules(appModule, dataModuleJvm, featureModule)
       }.checkModules {  // or .verify() per Koin 4 API
           withInstance<SavedStateHandle>()
       }
   }
   ```

9. **`NavFlowUiTest.kt`** (androidTest):
   ```kotlin
   @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()
   @Before fun setup() {
       loadKoinModules(module {
           single<LessonRepository> { FakeLessonRepository() }
           single<UserPrefsRepository> { FakeUserPrefsRepository() }
       })
   }
   @Test fun landingToGalleryToDetail() {
       composeRule.onNodeWithText("Open Gallery").performClick()
       composeRule.onNodeWithText("Test Basics").performClick()
       composeRule.onNodeWithText("intro").assertIsDisplayed()
   }
   ```

10. **`FavoriteTogglePersistenceUiTest.kt`** — same pattern; tap ★ on a card, recreate activity (`activityRule.scenario.recreate()`), confirm ★ still filled.

11. **`ReducedMotionPathUiTest.kt`** — set `userOverride = true` on `FakeUserPrefsRepository` before recompose, verify navigation completes within 50ms (no transition). Assert via `composeRule.mainClock.autoAdvance = false` + measure frame count.

12. **Wire `TestDreamsApp.kt`** as test runner Application via `AndroidJUnitRunner` subclass `DreamsTestRunner` (override `newApplication` to return `TestDreamsApp`). Update `app/build.gradle.kts` `testInstrumentationRunner` to `com.dantech.dreams.support.DreamsTestRunner`. Test app initializes Koin with fakes-friendly module set (no DataStore Android single).

13. **Run `./gradlew test connectedDebugAndroidTest`.** Address flakes; tighten waits via `composeRule.waitUntil(...)` instead of fixed sleeps.

14. **Delete stub tests** (`ExampleUnitTest.kt`, `ExampleInstrumentedTest.kt`).

## 8. Todo
- [ ] `MainCoroutineRule` + `FakeLessonRepository` + `FakeUserPrefsRepository` created
- [ ] 4 VM tests green
- [ ] `LessonRepositoryImplTest` migrated + expanded
- [ ] `UserPrefsRepositoryImplTest` + `ParamOverridesCodecTest` green
- [ ] `KoinModulesCheckTest` green (with module split if needed)
- [ ] `NavFlowUiTest`, `FavoriteTogglePersistenceUiTest`, `ReducedMotionPathUiTest` green
- [ ] `TestDreamsApp` + `DreamsTestRunner` wired in build.gradle
- [ ] Stub example tests deleted
- [ ] `./gradlew test connectedDebugAndroidTest` green on CI-grade run

## 9. Success Criteria
- `./gradlew test` <30s, all green.
- `./gradlew connectedDebugAndroidTest` all green on emulator API 33.
- VM unit tests do NOT touch Android framework (pure JVM, no Robolectric).
- UI tests survive `activity.recreate()` for persistence claims.

## 10. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| `LessonRepositoryImpl()` constructor calls `LessonRegistry.bootstrap()` which throws on duplicate id when re-instantiated across tests | High | High | Make `LessonRegistry.bootstrap()` idempotent: check `if (all.isEmpty()) { ... }` before registration; OR skip-bootstrap path for `LessonRepositoryImpl(skipBootstrap = true)` in tests. Decide in phase-02 already; revisit here if pain |
| Koin `checkModules()` API renamed in 4.x | Med | Low | Use `koinApplication { }.checkModules()` if extension; consult released docs |
| Compose UI tests flake on shared-transition timing | Med | Med | `mainClock.autoAdvance = false` + manual `advanceTimeBy()`. Or assert post-anim final state with `waitUntil` |
| AGSL canvas in detail screen during UI test crashes (RuntimeShader on emulator) | Med | High | Stub `LessonRepository.byId` to return shader source `"half4 main(float2 fc) { return half4(1); }"` — minimal AGSL valid program, compiles fast |
| `loadKoinModules()` accumulates across tests → state leak | High | Med | Use `@Before { stopKoin(); startKoin { modules(...) } }` per test, OR `KoinTestRule` |

## 11. Security Considerations
- Test DataStore uses `TemporaryFolder` — sandboxed, no PII, scrubbed per-test.
- No real network or device perms exercised.

## 12. Next Steps
- Phase-08 design polish; testing infra is steady-state going forward.
- Add CI workflow (GitHub Actions) running both test suites — out of refactor scope, follow-up.

## Unresolved Questions
- **`checkModules` vs `verify`:** Koin 4 final API name — confirm.
- **Recreating Activity for persistence test:** `activityRule.scenario.recreate()` resets ViewModelStore but not DataStore file. Does `FakeUserPrefsRepository` need to also be persistent across recreate? Use per-test single-instance stored in a `companion object` map keyed by class to span recreate.
- **AGSL on emulator API 33:** confirm Pixel 6 emulator hardware GLES path supports AGSL; otherwise pin emulator image and document.
