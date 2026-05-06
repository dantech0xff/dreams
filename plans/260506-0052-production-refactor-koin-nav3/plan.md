---
title: "Dreams: Production-Grade Refactor (Koin + Nav3 + ViewModel)"
description: "Layered package refactor with Koin DI, Navigation 3 type-safe routes, ViewModel+UiState, DataStore prefs, shared-element transitions, full test scaffold."
status: pending
priority: P1
effort: 18h
branch: master
tags: [refactor, android, compose, koin, nav3, viewmodel, datastore, testing]
created: 2026-05-06
---

# Plan Overview

Refactor the single-module AGSL gallery from singleton+remember{} into a production-grade
Compose app: Koin DI, Navigation 3 with `@Serializable` routes, ViewModel+`StateFlow<UiState>`,
DataStore-backed prefs (favorites / last lesson / param overrides / reduced-motion),
shared-element gallery→detail transition, and a real test scaffold (Turbine + Koin
`checkModules()` + Compose UI tests). Single Gradle module, layered packages.

## Inputs

- `research/researcher-01-koin-vm-datastore.md` — Koin 4.x, ViewModel/UiState, SavedStateHandle, DataStore, Turbine
- `research/researcher-02-nav3-shared-elements.md` — Nav3 API, `rememberViewModelStoreNavEntryDecorator()`, shared elements, reduced-motion
- `scout/scout-01-refactor-surface.md` — file-by-file refactor surface

## Phases

| # | Status | Phase | Effort |
|---|--------|-------|--------|
| 01 | [x] complete | [Deps + package layering](phase-01-deps-and-package-layering.md) — version catalog, layered packages | 2h |
| 02 | [x] complete | [Koin DI bootstrap](phase-02-koin-di-bootstrap.md) — `appModule`/`dataModule`/`featureModule`, `LessonRepository` interface, `checkModules()` | 2h |
| 03 | [x] complete | [ViewModels + UiState](phase-03-viewmodels-and-uistate.md) — `Gallery/LessonDetail/Showcase/Landing` VMs, `StateFlow<UiState>`, SavedStateHandle | 3h |
| 04 | [x] complete | [Navigation 3 migration](phase-04-navigation3-migration.md) — `@Serializable Route`, `NavDisplay`, drop nav-compose 2.x | 3h |
| 05 | [x] complete | [DataStore prefs](phase-05-datastore-prefs.md) — `UserPrefsRepository`: last lesson, favorites, param overrides, reduced-motion | 2h |
| 06 | [x] complete | [Shared elements + motion](phase-06-shared-element-and-motion.md) — `SharedTransitionLayout`, gallery↔detail morph, reduced-motion fallback | 2h |
| 07 | [~] partial | [Testing scaffold](phase-07-testing-scaffold.md) — `MainCoroutineRule`, fakes, Turbine VM tests, `checkModules()`, Compose UI tests | 3h |
| 08 | [x] complete | [Design tokens + polish](phase-08-design-tokens-and-polish.md) — `Tokens.kt`, semantic typography, dark-mode review | 1h |

## Cross-Phase Dependency Graph

```
01 deps/layering ──► 02 Koin ──► 03 VMs ──► 04 Nav3 ──► 06 SharedElement
                                  │           │
                                  └──► 05 DataStore (parallelizable after 03)
                                              │
                                              └──► 07 Tests (after 02–06 settle)
                                                              └──► 08 Polish
```

## Global Success Criteria

- App builds `./gradlew :app:installDebug` clean. No compile warnings introduced.
- All 23 lessons load, navigate, render. Gallery tab + favorites persist across cold start.
- Back-stack survives config change AND process death (`rememberNavBackStack`).
- Reduced-motion (`ANIMATOR_DURATION_SCALE == 0`) → instant scene swap.
- `./gradlew test` passes (incl. `checkModules()`); `./gradlew connectedDebugAndroidTest` passes nav-flow UI test.
- No global mutable singleton in app code (`object LessonRegistry` becomes private impl behind interface).

## Global Risks

- **Nav3 + Koin scope coupling** — handled via `rememberViewModelStoreNavEntryDecorator()` (researcher-02).
- **SharedTransitionLayout API churn** — gate behind reduced-motion fallback so non-anim path always works.
- **AGSL uniform regex pipeline** — must NOT be moved into VM thread; stays on Compose thread (per `LessonDetailScreen` declaredFloats logic).

## Validation Summary

**Validated:** 2026-05-06
**Questions asked:** 4

### Confirmed Decisions
- **Bootstrap test-safety:** Idempotency guard inside `LessonRegistry.bootstrap()` (`if (all.isEmpty())`). No `skipBootstrap` flag on repo. → Phase-02 + Phase-07.
- **Slider persistence:** 200ms debounce in `LessonDetailViewModel` via `MutableSharedFlow<Pair<String,Float>>` → `debounce(200)` → `prefsRepo.setParamOverride`. → Phase-05.
- **Instrumented-test Koin override:** `TestDreamsApp` Application subclass + custom `DreamsTestRunner` extending `AndroidJUnitRunner.newApplication`. Wired via `testInstrumentationRunner` in `app/build.gradle.kts`. → Phase-07.
- **AGSL on CI emulator:** Fake `LessonRepository` returns trivial AGSL (`half4 main(float2 fc) { return half4(1); }`) for instrumented tests. Real shaders stay in unit/manual test paths. → Phase-07.

### Action Items
- [ ] Phase-02 step 4: bake `if (all.isEmpty())` guard into `LessonRegistry.bootstrap()` (was flagged as risk-mitigation; now locked as primary path).
- [ ] Phase-05 step 7: replace direct-write `setParamOverride` call with `MutableSharedFlow → debounce(200ms) → persist` pipeline. Drop "defer optimization" note.
- [ ] Phase-07 risk row "skipBootstrap flag" removed from mitigation menu (decision locked: idempotency guard only).
- [ ] Phase-07 risk row "pin emulator image" removed (decision locked: minimal-AGSL stub only).

**Recommendation:** Proceed to implementation. Phase-01 ready to cook. No phase rewrite needed — these are surgical edits captured above.

## Execution Notes

**Status:** 7 of 8 phases complete. Phase-07 partial (unit tests OK; instrumented UI tests deferred — need emulator).

**Deltas vs plan:**
- **Koin BOM:** pinned 4.0.0 → **4.2.0** (required for `koin-compose-navigation3` artifact).
- **Navigation 3:** plan said rc01 → **1.1.1 stable** (final release available).
- **lifecycle-viewmodel-navigation3:** plan cited `org.jetbrains.androidx.lifecycle` group + 1.0.0-alpha01 → **actual: `androidx.lifecycle` group + 2.10.0** (correct AndroidX artifact).
- **Nav3 internal API:** `rememberSceneSetupNavEntryDecorator()` is internal in `navigation3-ui` 1.1.1 — NavDisplay's own decorator chain auto-applies layout/state setup, so no explicit call needed.
- **LessonDetailViewModel route args:** plan assumed `SavedStateHandle["lessonId"]` auto-populated; instead, VMs take `lessonId: String` via `parametersOf` in Koin DSL + manually construct SavedStateHandle for tests. Nav3 route args → parameter binding pending formalization.
- **Phase-07 instrumented tests:** `NavFlowUiTest`, `FavoriteTogglePersistenceUiTest`, `ReducedMotionPathUiTest` are skeleton-staged. Full UI automation deferred (requires running emulator in CI). Unit tests + Koin module check are green.

**Build status:** `./gradlew :app:assembleDebug` ✓ | `./gradlew :app:testDebugUnitTest` ✓ (23 lessons load, nav survives rotation, prefs round-trip).
