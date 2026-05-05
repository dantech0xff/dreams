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
| 01 | [ ] pending | [Deps + package layering](phase-01-deps-and-package-layering.md) — version catalog, layered packages | 2h |
| 02 | [ ] pending | [Koin DI bootstrap](phase-02-koin-di-bootstrap.md) — `appModule`/`dataModule`/`featureModule`, `LessonRepository` interface, `checkModules()` | 2h |
| 03 | [ ] pending | [ViewModels + UiState](phase-03-viewmodels-and-uistate.md) — `Gallery/LessonDetail/Showcase/Landing` VMs, `StateFlow<UiState>`, SavedStateHandle | 3h |
| 04 | [ ] pending | [Navigation 3 migration](phase-04-navigation3-migration.md) — `@Serializable Route`, `NavDisplay`, drop nav-compose 2.x | 3h |
| 05 | [ ] pending | [DataStore prefs](phase-05-datastore-prefs.md) — `UserPrefsRepository`: last lesson, favorites, param overrides, reduced-motion | 2h |
| 06 | [ ] pending | [Shared elements + motion](phase-06-shared-element-and-motion.md) — `SharedTransitionLayout`, gallery↔detail morph, reduced-motion fallback | 2h |
| 07 | [ ] pending | [Testing scaffold](phase-07-testing-scaffold.md) — `MainCoroutineRule`, fakes, Turbine VM tests, `checkModules()`, Compose UI tests | 3h |
| 08 | [ ] pending | [Design tokens + polish](phase-08-design-tokens-and-polish.md) — `Tokens.kt`, semantic typography, dark-mode review | 1h |

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
