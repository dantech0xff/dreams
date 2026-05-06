---
title: "Bottom Tab Navigation Rework"
description: "Replace Landing+Gallery+TabRow with 3-tab Bottom Navigation shell with per-tab back stacks."
status: completed
priority: P2
effort: 8h
branch: master
tags: [navigation3, compose, refactor, ui]
created: 2026-05-06
---

# Bottom Tab Navigation Rework

## Goal

Rework the app from `Landing → Gallery (5-tab TabRow) → Detail/Showcase` flow into a **3-tab Bottom Navigation shell** (Lesson / Showcase / Settings) with **per-tab back stacks**, drill-down navigation, and Settings as a fullscreen page.

## Architectural Decisions (locked)

| Decision | Choice | One-line justification |
|---|---|---|
| Nav pattern | Single `NavDisplay` + `TopLevelBackStack` helper | Official Nav3 recipe; one `SharedTransitionLayout` only; minimum complexity |
| Bar visibility | `AnimatedVisibility` in Scaffold `bottomBar`, route-driven | Smooth slide; Scaffold auto-recomputes insets; reduced-motion aware |
| File rename | Rename `PlaygroundNavHost.kt` → `MainShell.kt`; rename `PlaygroundApp()` → `MainShell()` | Better signals "shell with bottom bar" responsibility |
| `LessonCategory` enum | Keep `SHOWCASE` entry; add `lessonOnly()` filter | Lesson data still references it; non-breaking |
| Showcase data | Add `LessonRepository.showcases()` accessor | Removes Showcase-as-category coupling from UI layer |
| Tap-current-tab-pop-to-root | Include | ~10 LOC, matches Material/Instagram norm |
| About AGSL location | Move from `feature/landing/` → `feature/settings/`, opens as bottom sheet from SettingsScreen | Single-source after Landing deletion |

## Phase List

| # | File | Status | Effort | Depends |
|---|------|--------|--------|---------|
| 1 | [phase-01-navigation-shell-and-routes.md](phase-01-navigation-shell-and-routes.md) | completed | 2h | — |
| 2 | [phase-02-lesson-tab-screens.md](phase-02-lesson-tab-screens.md) | completed | 1.5h | 1 |
| 3 | [phase-03-showcase-tab-screen.md](phase-03-showcase-tab-screen.md) | completed | 0.5h | 1 |
| 4 | [phase-04-settings-tab-screen.md](phase-04-settings-tab-screen.md) | completed | 1h | 1 |
| 5 | [phase-05-cleanup-and-deletion.md](phase-05-cleanup-and-deletion.md) | completed | 1h | 2,3,4 |
| 6 | [phase-06-validation.md](phase-06-validation.md) | completed-with-followup | 1h | 5 |

## Key Dependencies

- `androidx.navigation3:navigation3-runtime:1.1.1` (already on classpath; uses `rememberNavBackStack`, `NavDisplay`)
- `androidx.compose.material3` (Material3 BOM 2026.02.01) — `NavigationBar`, `NavigationBarItem`, `Scaffold`, `AnimatedVisibility`
- `androidx.compose.material:material-icons-core` — `Icons.Filled.List`, `Icons.Filled.Star`, `Icons.Filled.Settings` (verify on classpath in phase-01)
- Existing `core/motion` — preserved
- Existing Koin DI — `FeatureModule.kt` updated in phase-02/04, cleaned in phase-05

## Reports / Research

- [researcher-260506-0724-navigation3-multi-stack.md](../reports/researcher-260506-0724-navigation3-multi-stack.md)
- [researcher-260506-0724-bottom-bar-ux.md](../reports/researcher-260506-0724-bottom-bar-ux.md)

## Rollback Strategy

Each phase commits independently. Phases 1–4 keep old code intact (Landing/Gallery/SettingsSheet untouched). Only phase-05 deletes. Revert phase-05 if integration regresses; phases 1–4 alone are inert (not wired into MainActivity until phase-01 step 9 — see phase-01).

Specifically, phase-01 wires `MainActivity` → new `MainShell`. To rollback to old shell: revert that one file (`MainActivity.kt`) to call `PlaygroundApp()` again.

## Out of Scope

- No new tests (project has none currently). Manual smoke test only (phase-06).
- No deep-link or external intent handling.
- No tablet/large-screen NavigationRail variant (3-item bar works compact only — acceptable for MVP).
- No theme change.

## Outcome

**Shipped Phases 1–6 (completed-with-followup on phase-06 device smoke test).**

**Core Decisions Held:**
- Single `NavDisplay` + `TopLevelBackStack` helper per Navigation3 official recipe. One `SharedTransitionLayout` only.
- Per-tab LRU back stack with config-change persistence via `rememberSaveable` + custom saver; process-death graceful fallback to root on schema drift.
- `Saver<T>` in TopLevelBackStack handles serialization of route stacks via `Json.encodeToString()` and polymorphic deserialization.

**Code-Review Fixes Applied:**
- **M1 (major):** Back-to-exit navigation broken. Fixed in `TopLevelBackStack.kt` — corrected `removeLast()` logic to pop stacks in LRU order when main back stack is exhausted.
- **M2 (major):** Saver crash on schema drift. Fixed in `TopLevelBackStack.kt` — added try-catch in deserialization to gracefully fall back to root on unknown route type (e.g., deleted phase).

**Cosmetic Findings Fixed:**
- m1: Unused `Text` import removed from `MainShell.kt`.
- m2: Dead `rememberCoroutineScope` removed from `AboutAgslSheet.kt`.
- m3: Redundant `koinViewModel` key parameter removed from `LessonListScreen.kt`.

**Build & Test Pass:**
- `:app:assembleDebug` clean.
- `./gradlew test` — `KoinModulesCheckTest` green; no orphaned VMs.
- Static analysis clean (lintDebug).

**Open Follow-Ups:**
- **M3 (cosmetic):** Favorites flicker briefly on cold launch (DataStore I/O timing). Non-blocking; queued for future perf pass.
- GitHub URL placeholder in Settings (not linked to real repo yet).
- On-device smoke test (manual rotation, tab-stack persistence, reduced-motion) awaiting emulator/device session.

**Acceptance:** All 6 phases completed. App builds and tests pass. Code review blockers fixed. Ready for merge after manual on-device smoke test (Phase 06 device checklist pending).
