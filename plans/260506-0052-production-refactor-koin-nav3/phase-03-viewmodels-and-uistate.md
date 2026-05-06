# Phase 03 — ViewModels + UiState

## 1. Context Links
- Parent: [plan.md](plan.md)
- Depends on: [phase-02-koin-di-bootstrap.md](phase-02-koin-di-bootstrap.md)
- Inputs: `research/researcher-01-koin-vm-datastore.md` §1, §2, scout §3
- Docs: https://developer.android.com/topic/architecture/ui-layer | https://insert-koin.io/docs/reference/koin-android/viewmodel

## 2. Overview
- **Date:** 2026-05-06
- **Description:** Promote screen-local state to ViewModels with `data class UiState` + `StateFlow`. Wire via `koinViewModel()`. Use `SavedStateHandle` for `lessonId` route arg + tab selection survival. Keep AGSL uniform regex pipeline on Compose thread (NOT in VM).
- **Priority:** P1
- **Implementation status:** pending
- **Review status:** pending

## 3. Key Insights
- **GalleryScreen.kt:29** holds `var selected: Int` — promote to `GalleryViewModel.uiState.selectedTabIndex`. Survives config change via `SavedStateHandle`.
- **LessonDetailScreen.kt:122** `rememberControlValues()` builds a `SnapshotStateMap<String, Any>` from `lesson.controls` defaults. **Do NOT move into VM** — `Color`/`Float` mutations drive recomposition via Compose snapshot, not StateFlow. VM owns *which lesson*, *current parameter overrides as cold Map<String, Float>*; screen rehydrates SnapshotStateMap from VM state.
- **LessonDetailScreen.kt:170** `applyUniforms` runs on Compose render path (per-frame). Must stay in composable. VM only manages user-facing controls.
- **ShowcaseScreen.kt:48** `var hideUi: Boolean` — promote to `ShowcaseViewModel.uiState.hideUi`. Survives rotation.
- **LandingScreen.kt:30** `var aboutOpen: Boolean` — minor; could stay composable-local OR live in `LandingViewModel`. Decision: **promote** for consistency + to allow future deep-link-to-about.
- `WhileSubscribed(5_000)` not strictly needed yet — repos are synchronous. Use `MutableStateFlow` directly; reserve `stateIn` for phase-05 when DataStore Flow is consumed.
- `koinViewModel()` works inside Nav3 entries ONLY when `rememberViewModelStoreNavEntryDecorator()` is registered (phase-04). For phase-03 (still on Nav2 routing), `koinViewModel()` works via `LocalViewModelStoreOwner` from `NavHost` composables.

## 4. Requirements

### Functional
- `GalleryViewModel.uiState` exposes `categories`, `selectedTabIndex`, `lessons`. Tab switches update state.
- `LessonDetailViewModel(savedStateHandle)` reads `lessonId` from handle; exposes `lesson: LessonModel?`, `paramOverrides: Map<String, Float>`. `setParamOverride(name, value)` mutates state.
- `ShowcaseViewModel(savedStateHandle)` exposes `lesson: LessonModel?`, `hideUi: Boolean`. `toggleUi()` flips.
- `LandingViewModel` exposes `aboutOpen: Boolean`. `setAboutOpen(open)` mutates.
- All four VMs survive config change. `lessonId` and `selectedTab` survive process death.

### Non-Functional
- No business logic in composables. Screens become pure renderers of UiState + event callbacks.
- Per-VM file ≤120 lines.

## 5. Architecture

```
                ┌──────────────────┐
                │ LessonRepository │
                └────────┬─────────┘
                         │ get()
   ┌─────────────────────┼─────────────────────┐
   │                     │                     │
GalleryVM        LessonDetailVM         ShowcaseVM
(repo)         (repo, savedState)    (repo, savedState)
   │                     │                     │
StateFlow<UiState>  StateFlow<UiState>   StateFlow<UiState>
   │                     │                     │
GalleryScreen      LessonDetailScreen     ShowcaseScreen
                                                       
LandingVM (no repo dep needed) ─► LandingScreen
```

Each `UiState` is `@Immutable data class` with sane defaults. Events flow up through `(Event) -> Unit` lambdas the VM exposes.

## 6. Related Code Files

### Modify
- `ui/feature/gallery/GalleryScreen.kt` — drop `mutableIntStateOf`, drop `LessonRegistry` direct call; consume VM
- `ui/feature/lesson/LessonDetailScreen.kt` — drop direct `koinInject()` from phase-02; use VM for `lesson` + `paramOverrides`. Keep regex/uniform pipeline.
- `ui/feature/showcase/ShowcaseScreen.kt` — same pattern as detail
- `ui/feature/landing/LandingScreen.kt` — drop `var aboutOpen`; consume VM
- `core/di/FeatureModule.kt` — add four `viewModel { }` declarations

### Create
- `ui/feature/gallery/GalleryViewModel.kt` + `GalleryUiState.kt`
- `ui/feature/lesson/LessonDetailViewModel.kt` + `LessonDetailUiState.kt`
- `ui/feature/showcase/ShowcaseViewModel.kt` + `ShowcaseUiState.kt`
- `ui/feature/landing/LandingViewModel.kt` + `LandingUiState.kt`

### Delete
- None

## 7. Implementation Steps

1. **Create `ui/feature/gallery/GalleryViewModel.kt`:**
   ```kotlin
   data class GalleryUiState(
       val categories: ImmutableList<LessonCategory> = LessonCategory.entries.toImmutableList(),
       val selectedTabIndex: Int = 0,
       val lessons: ImmutableList<LessonModel> = persistentListOf()
   )
   class GalleryViewModel(
       private val repo: LessonRepository,
       private val savedState: SavedStateHandle
   ) : ViewModel() {
       private val key = "selectedTabIndex"
       private val _ui = MutableStateFlow(initial())
       val uiState: StateFlow<GalleryUiState> = _ui.asStateFlow()
       private fun initial(): GalleryUiState {
           val idx = savedState.get<Int>(key) ?: 0
           val cats = LessonCategory.entries.toImmutableList()
           return GalleryUiState(cats, idx, repo.byCategory(cats[idx]))
       }
       fun selectTab(i: Int) {
           savedState[key] = i
           val cats = _ui.value.categories
           _ui.update { it.copy(selectedTabIndex = i, lessons = repo.byCategory(cats[i])) }
       }
   }
   ```

2. **Create `ui/feature/lesson/LessonDetailViewModel.kt`:**
   ```kotlin
   data class LessonDetailUiState(
       val lesson: LessonModel? = null,
       val paramOverrides: PersistentMap<String, Float> = persistentMapOf()
   )
   class LessonDetailViewModel(
       private val repo: LessonRepository,
       savedState: SavedStateHandle
   ) : ViewModel() {
       private val lessonId: String = savedState["lessonId"]
           ?: error("LessonDetailViewModel requires 'lessonId' in SavedStateHandle")
       private val _ui = MutableStateFlow(LessonDetailUiState(repo.byId(lessonId)))
       val uiState: StateFlow<LessonDetailUiState> = _ui.asStateFlow()
       fun setFloat(uniform: String, value: Float) =
           _ui.update { it.copy(paramOverrides = it.paramOverrides.put(uniform, value)) }
       fun resetOverrides() = _ui.update { it.copy(paramOverrides = persistentMapOf()) }
   }
   ```
   `lessonId` flows in via Nav3 typed-route → `SavedStateHandle` (phase-04 wires this).

3. **Create `ui/feature/showcase/ShowcaseViewModel.kt`** — analogous, `data class ShowcaseUiState(val lesson, val hideUi: Boolean = false)` + `toggleUi()`.

4. **Create `ui/feature/landing/LandingViewModel.kt`** — `data class LandingUiState(val aboutOpen: Boolean = false)`, `setAboutOpen(b)`.

5. **Edit `core/di/FeatureModule.kt`:**
   ```kotlin
   val featureModule = module {
       viewModel { GalleryViewModel(get(), get()) }
       viewModel { LessonDetailViewModel(get(), get()) }
       viewModel { ShowcaseViewModel(get(), get()) }
       viewModel { LandingViewModel(get()) }
   }
   ```

6. **Edit `GalleryScreen.kt`:** signature becomes `GalleryScreen(onLessonClick: (String) -> Unit, vm: GalleryViewModel = koinViewModel())`. Replace `var selected by remember` with `val ui by vm.uiState.collectAsStateWithLifecycle()`. Tab `onClick` → `vm.selectTab(i)`. Drop `LessonRegistry.byCategory` call.

7. **Edit `LessonDetailScreen.kt`:** signature becomes `LessonDetailScreen(onBack, vm: LessonDetailViewModel = koinViewModel())`. Drop the `lessonId: String` param (Nav3 typed-route in phase-04 will set `lessonId` on SavedStateHandle).
   - **CRITICAL:** keep `rememberControlValues(lesson)` SnapshotStateMap. Hydrate from `vm.uiState.value.paramOverrides` on first composition. On slider change, call BOTH `controlValues[name] = v` (for instant per-frame uniform via Compose snapshot) AND `vm.setFloat(name, v)` (for persistence in phase-05). Two-write pattern is correct because shader uniforms must update at frame rate; VM is for persistence not render.

8. **Edit `ShowcaseScreen.kt`:** consume `vm.uiState`. Replace `var hideUi` with `ui.hideUi`. Click → `vm.toggleUi()`.

9. **Edit `LandingScreen.kt`:** consume `vm.uiState`. About-button click → `vm.setAboutOpen(true)`. Sheet dismiss → `vm.setAboutOpen(false)`.

10. **Build & smoke-test.** Run `./gradlew :app:installDebug`. Verify rotation: gallery tab survives, detail param sliders survive, showcase hideUi survives.

## 8. Todo
- [ ] 4 ViewModel files + 4 UiState data classes created
- [ ] `featureModule` populated
- [ ] All 4 screens consume VM via `koinViewModel()`
- [ ] `LessonDetailScreen` keeps SnapshotStateMap for per-frame uniforms (regex pipeline preserved)
- [ ] Rotation test: tab + sliders + hideUi survive
- [ ] No `mutableStateOf` for cross-screen-relevant state remains in screens

## 9. Success Criteria
- `./gradlew test` green (incl. checkModules with new VM bindings).
- Manual rotation in Gallery: selected tab persists.
- Manual rotation in LessonDetail: parameter slider position persists.
- `grep -r "mutableIntStateOf\|mutableStateOf" app/src/main/java/com/dantech/dreams/ui/feature` returns ONLY: per-frame ephemeral state (touchPos, touchTime, controlValues snapshot map). No tab/screen mode flags.

## 10. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| `koinViewModel()` doesn't bind to Nav2 back-stack entry → VM survives navigation away (memory leak) | Med | Med | Acceptable transitional state. Phase-04 introduces `rememberViewModelStoreNavEntryDecorator()` which fixes lifecycle |
| SavedStateHandle missing `lessonId` because phase-03 ships before phase-04 nav rewrite | High | High | Phase-03 LessonDetailScreen still receives `lessonId` as composable arg AND passes it to VM via `parametersOf` in `koinViewModel(parameters = { parametersOf(lessonId) })`. Switch to SavedStateHandle in phase-04 |
| Two-write to `controlValues` + VM diverges if VM resets | Low | Med | Single source-of-truth rule: VM is canonical for persistence; SnapshotStateMap is render-path mirror. On compose first-frame, hydrate map from VM state. Document in code comment |
| `collectAsStateWithLifecycle()` requires `lifecycle-runtime-compose` dep | High | Low | Add `androidx.lifecycle:lifecycle-runtime-compose` to libs.versions.toml in phase-01 if missing |

## 11. Security Considerations
- None new. VMs hold no PII.

## 12. Next Steps
- Phase-04 swaps screens to receive `lessonId` via Nav3 `SavedStateHandle` instead of composable param.
- Phase-05 uses `LessonDetailViewModel` to persist `paramOverrides` to DataStore.

## Unresolved Questions
- **`koinViewModel()` parametersOf vs SavedStateHandle:** for phase-03 transitional, do we use `parameters = { parametersOf(lessonId) }` then drop in phase-04? Or use `koinViewModel<X>(viewModelStoreOwner = ...)`? Confirm Koin 4 + lifecycle-viewmodel-savedstate transitive coverage.
- **`PersistentMap` from kotlinx.collections.immutable for paramOverrides:** verify `persistentMapOf<String, Float>()` available; if not, fall back to `Map<String, Float>` with `_ui.update { copy(paramOverrides = it.paramOverrides + (k to v)) }`.
