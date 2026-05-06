# Phase 04 — Navigation 3 Migration

## 1. Context Links
- Parent: [plan.md](plan.md)
- Depends on: [phase-03-viewmodels-and-uistate.md](phase-03-viewmodels-and-uistate.md)
- Inputs: `research/researcher-02-nav3-shared-elements.md` §1–3
- Docs: https://developer.android.com/guide/navigation/navigation-3 | https://developer.android.com/guide/navigation/navigation-3/migration-guide | https://insert-koin.io/docs/reference/koin-compose/navigation3/

## 2. Overview
- **Date:** 2026-05-06
- **Description:** Replace `androidx.navigation:navigation-compose 2.8.5` with `androidx.navigation3` 1.1.0-rc01. Define `@Serializable Route` sealed interface. Replace `NavHost`/`NavController` with `NavDisplay`/`NavBackStack`. Wire `rememberViewModelStoreNavEntryDecorator()` for Koin VM scoping per entry.
- **Priority:** P1
- **Implementation status:** pending
- **Review status:** pending

## 3. Key Insights
- Current routes (PlaygroundNavHost.kt:14-21): string templates `"lesson/{id}"`, `"showcase/{id}"`. Nav3 replaces with `@Serializable data class Route.LessonDetail(val id: String)`.
- The id-prefix routing logic (`PlaygroundNavHost.kt:34`: `if (id.startsWith("showcase-")) Routes.showcase(id) else Routes.lesson(id)`) leaks domain knowledge into nav. Move to repo: `LessonRepository.routeFor(id): Route` OR keep prefix check at click site (locked: keep simple at gallery click).
- `rememberNavBackStack<Route>(Route.Landing)` auto-persists via `Saver` over config change + process death (researcher-02 §2).
- `rememberViewModelStoreNavEntryDecorator()` is **the bridge** between Nav3 entry lifetime and Koin's `koinViewModel()` — without it, VMs never clear on pop.
- Nav3 `NavDisplay` accepts `entryProvider { entry<Route> { } }` DSL — type-safe dispatch.
- **Deep links:** scout shows no current intent-filters. Skip deep link rebuild this phase; document as TODO.

## 4. Requirements

### Functional
- Sealed `Route` interface with: `Landing`, `Gallery`, `LessonDetail(id: String)`, `Showcase(id: String)`, `AboutAgsl` (optional, currently sheet-based).
- Forward navigation: `backStack.add(Route.X)`. Back: `backStack.removeLastOrNull()`.
- LessonDetail/Showcase VMs receive `id` via `SavedStateHandle["lessonId"]`.
- Back button (system + UI) pops back stack correctly. Empty stack → finish activity.
- Lesson VM scoped to entry: navigating away clears VM; returning re-creates fresh.

### Non-Functional
- Drop `libs.androidx.navigation.compose` from build.gradle (no Nav2 leftover).
- Type-safety: route arg `id` is `String` at compile time, no `entry.arguments?.getString("id").orEmpty()` shenanigans.

## 5. Architecture

```
MainActivity
  └── DreamsTheme {
         val backStack = rememberNavBackStack<Route>(Route.Landing)
         NavDisplay(
           backStack = backStack,
           entryDecorators = listOf(
             rememberSceneSetupNavEntryDecorator(),
             rememberSavedStateNavEntryDecorator(),
             rememberViewModelStoreNavEntryDecorator() ← Koin VM scope source
           ),
           entryProvider = entryProvider {
             entry<Route.Landing>     { LandingScreen(onOpenGallery = { backStack.add(Route.Gallery) }) }
             entry<Route.Gallery>     { GalleryScreen(onLessonClick = { id -> backStack.add(routeFor(id)) }) }
             entry<Route.LessonDetail>{ route -> LessonDetailScreen(onBack = { backStack.removeLastOrNull() }) }
             entry<Route.Showcase>    { route -> ShowcaseScreen(onBack = { backStack.removeLastOrNull() }) }
           }
         )
       }
```

VMs receive `lessonId` via `SavedStateHandle` because Nav3 entry serializes route args into `SavedStateHandle` automatically (validate per researcher-02; if not, manually do `parameters = { parametersOf(route.id) }` in Koin DSL).

## 6. Related Code Files

### Modify
- `ui/feature/nav/PlaygroundNavHost.kt` — full rewrite as `NavDisplay`-based
- `MainActivity.kt` — call updated `PlaygroundApp()`
- `ui/feature/lesson/LessonDetailScreen.kt` — drop `lessonId: String` param; rely on VM via SavedStateHandle
- `ui/feature/showcase/ShowcaseScreen.kt` — same as detail
- `app/build.gradle.kts` — remove nav-compose 2.x dep (added in phase-01)
- `gradle/libs.versions.toml` — remove `navigationCompose` version + library entry

### Create
- `ui/feature/nav/Route.kt` — sealed `Route` with `@Serializable` subtypes

### Delete
- None (NavHost is rewritten in place)

## 7. Implementation Steps

1. **Create `ui/feature/nav/Route.kt`:**
   ```kotlin
   import kotlinx.serialization.Serializable
   sealed interface Route {
       @Serializable data object Landing : Route
       @Serializable data object Gallery : Route
       @Serializable data class LessonDetail(val id: String) : Route
       @Serializable data class Showcase(val id: String) : Route
   }
   fun routeForLessonId(id: String): Route =
       if (id.startsWith("showcase-")) Route.Showcase(id) else Route.LessonDetail(id)
   ```

2. **Rewrite `ui/feature/nav/PlaygroundNavHost.kt`:**
   ```kotlin
   @Composable
   fun PlaygroundApp() {
       val backStack = rememberNavBackStack<Route>(Route.Landing)
       NavDisplay(
           backStack = backStack,
           onBack = { backStack.removeLastOrNull() },
           entryDecorators = listOf(
               rememberSceneSetupNavEntryDecorator(),
               rememberSavedStateNavEntryDecorator(),
               rememberViewModelStoreNavEntryDecorator(),
           ),
           entryProvider = entryProvider {
               entry<Route.Landing> {
                   LandingScreen(onOpenGallery = { backStack.add(Route.Gallery) })
               }
               entry<Route.Gallery> {
                   GalleryScreen(onLessonClick = { id -> backStack.add(routeForLessonId(id)) })
               }
               entry<Route.LessonDetail> { route ->
                   LessonDetailScreen(onBack = { backStack.removeLastOrNull() })
               }
               entry<Route.Showcase> { route ->
                   ShowcaseScreen(onBack = { backStack.removeLastOrNull() })
               }
           }
       )
   }
   ```

3. **Edit `LessonDetailScreen.kt`:** drop `lessonId: String` param. `LessonDetailViewModel` reads from `SavedStateHandle["lessonId"]`. **Wire route args → SavedStateHandle:** Nav3 typed-route entry should auto-populate `SavedStateHandle` from `@Serializable` fields (verify); if not, in Koin module use:
   ```kotlin
   viewModel { (id: String) -> LessonDetailViewModel(get(), SavedStateHandle(mapOf("lessonId" to id))) }
   ```
   Then in screen: `val vm = koinViewModel<LessonDetailViewModel> { parametersOf(route.id) }`. Pick whichever Nav3+Koin officially supports — researcher-02 hints `koin-compose-navigation3` artifact handles this.

4. **Edit `ShowcaseScreen.kt`:** symmetric change.

5. **Remove Nav2 deps:**
   - `app/build.gradle.kts`: remove `implementation(libs.androidx.navigation.compose)` line
   - `gradle/libs.versions.toml`: remove `navigationCompose` version + library entry

6. **Search & verify NO Nav2 imports remain:**
   ```bash
   grep -r "androidx.navigation.compose" app/src/main/java
   grep -r "rememberNavController\|NavHost(" app/src/main/java
   ```
   Both should be empty.

7. **Update tests:** any test that calls `rememberNavController()` (none currently in repo, but verify after phase-07 builds new ones) → use Nav3 test harness.

8. **Add `koin-compose-navigation3` if needed:** if step-3 Nav3+Koin VM scoping needs the dedicated artifact, add `io.insert-koin:koin-compose-navigation3` library entry to libs.versions.toml.

9. **Build + smoke-test:** `./gradlew :app:installDebug`. Test full nav flow: Landing → Gallery → tap basic lesson → detail renders → back → tap showcase → showcase renders → back → back → finishes activity. Rotate at every level: state survives.

10. **Validate VM lifecycle:** add temporary `Log.d("VM", "init $this")` + `override fun onCleared() { Log.d("VM", "cleared $this") }` in `LessonDetailViewModel`. Navigate to detail, then back. Confirm `onCleared` fires on pop. Remove logs before commit.

## 8. Todo
- [ ] `Route.kt` sealed interface created with @Serializable subtypes
- [ ] `PlaygroundNavHost.kt` rewritten using `NavDisplay` + `rememberNavBackStack`
- [ ] `rememberViewModelStoreNavEntryDecorator()` registered
- [ ] LessonDetailScreen / ShowcaseScreen drop `lessonId` param; VM reads from SavedStateHandle
- [ ] Nav2 dep removed from build.gradle + version catalog
- [ ] No `androidx.navigation.compose` imports remain
- [ ] Smoke test full nav flow (rotate at each step) passes
- [ ] VM `onCleared` fires on back-stack pop (manually verified)

## 9. Success Criteria
- `./gradlew :app:installDebug` clean.
- All 4 nav transitions work (Landing→Gallery, Gallery→Detail/Showcase, back).
- Process death (kill via adb `am force-stop` while detail open → relaunch) restores back stack to detail with same lesson id.
- `grep -r "androidx.navigation.compose\|NavController" app/src/main/java` → empty.

## 10. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| `rememberNavBackStack` Saver fails to serialize `Route.LessonDetail(id)` (kotlinx.serialization not auto-bound) | Med | High | Use `BackStack.saver()` factory or pass `Saver` explicitly with KSerializer. Test process-death restoration |
| Nav3 1.1.0-rc01 API drift from researcher-02 docs | Med | High | Pin to exact rc and validate compile against actual Maven artifacts before phase-04 starts |
| `koinViewModel()` doesn't auto-pick up Nav3 entry's `SavedStateHandle` | High | High | Use explicit `parametersOf(route.id)` and inject `SavedStateHandle` in Koin module manually as in step-3 fallback |
| Lost transitions: Nav2 default cross-fade gone, Nav3 jumps | High | Low | Phase-06 introduces explicit transitions / shared elements. Acceptable jank gap during this phase |
| Showcase id-prefix routing logic split across nav file (`routeForLessonId`) and gallery click handler | Low | Low | Keep single helper in `Route.kt` |

## 11. Security Considerations
- `@Serializable` route classes serialized via `Saver` — no PII in routes (only lesson ids, public).
- No deep links opened to URI-injection attacks (deferred).

## 12. Next Steps
- Phase-05 wires DataStore prefs into VMs.
- Phase-06 adds `SharedTransitionLayout` around `NavDisplay` for gallery↔detail morph.

## Unresolved Questions
- **Nav3 SavedStateHandle auto-binding for typed routes:** does `entry<Route.LessonDetail>` populate the entry-scoped `SavedStateHandle` with `id` automatically? If not, fallback parametersOf + manual `SavedStateHandle(mapOf(...))` per step-3 — verify before implementation.
- **Deep link parity (researcher-02 §1):** parking-lot. No current `<deep-link>` intent-filters in `AndroidManifest.xml`, so zero regression risk. Decide post-rollout whether to add `Route` ↔ URI mapper.
- **`rememberSceneSetupNavEntryDecorator` artifact:** confirm it exists in `navigation3-ui` 1.1.0-rc01 (researcher-02 cites pattern; specific decorator name may differ).
