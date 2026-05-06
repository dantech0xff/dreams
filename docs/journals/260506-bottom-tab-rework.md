# Bottom Tab Navigation Shell: Landing → Gallery Replacement

**Date**: 2026-05-06
**Severity**: High
**Component**: Navigation / UI Shell
**Status**: Shipped

## What Shipped

Replaced the single-screen Landing → Gallery TabRow → Detail navigation model with a canonical Material3 bottom-tab shell: 3 persistent tabs (Lesson, Showcase, Settings) backed by a `TopLevelBackStack` helper that flattens per-tab stacks into one observable list for `NavDisplay`. Bottom bar hides on fullscreen/detail routes via route-driven `AnimatedVisibility`. Settings migrated from modal sheet to fullscreen page. Lesson tab introduces category selection as a root screen (4 cards with SHOWCASE filtered out via `LessonCategory.lessonOnly()`). Clean removal of `feature/landing` and `feature/gallery`.

## Two Bugs the Code Review Caught

### 1. The Always-Fall-Back Bug (TopLevelBackStack.kt:create)

**Initial design:** Pre-seeded all 3 tab stacks at construction so `LinkedHashMap.size > 1` from cold launch. That made the back-fallback in `removeLast()` (line 73) ALWAYS fire — pressing back on the home tab teleported the user to whatever tab was inserted second, instead of exiting the app.

**Fix:** Lazy-seed only the start tab at creation. Other tabs materialize in `switchTopLevel()` on first visit. Now when a user has only visited home, the flattened backStack stays at size 1 and `NavDisplay` disables its `BackHandler`, letting system back exit the activity naturally.

**Why it matters:** The Nav3 multi-stack recipe works only if the empty/single-tab state is properly handled. When you have N tabs pre-created, the LRU fallback becomes a trap — there's always a "previous" tab to jump to. Lazy initialization is not an optimization; it's correctness.

### 2. The Crash-on-Restore Bug (TopLevelBackStack.kt:116–124)

**Initial code:** `decodeRoute()` called `error(...)` on unknown route prefixes. Any schema drift (renamed route, removed enum entry, app restored from a backup state file using an older schema) would crash inside `rememberSaveable` with no recovery.

**Fix:** Wrapped the entire `restore` block in `runCatching { ... }.getOrNull()`. On null, `rememberSaveable` falls back to its initial-value lambda — losing nav state on process death but never crashing the activity.

**Why it matters:** Savers are migration boundaries. Treat unknown encodings as "discard, start fresh" not "crash the app". We ship updates. Users install them with old app state in memory. That's not an error case; that's life.

## Held Up / Didn't

- **All plan decisions survived.** Single `NavDisplay` + `TopLevelBackStack` is the right model. The flattening approach (one observable list, per-tab internal ordering) beats trying to swap NavHost graphs.
- **One hidden dependency:** `material-icons-core` had to be added explicitly. Material3 BOM 2026.02.01 doesn't bring it transitively — runtime crash on cold launch without the dep in `app/build.gradle.kts`.

## Open Follow-Ups

- **On-device smoke:** Build/test/static all green, but no real device run yet. Need to verify bottom-bar animations, settings fullscreen layout, and lesson categories grid.
- **Cold-launch flicker:** Favorites briefly show then disappear on LessonList first emission. Async DataStore init — low priority cosmetic fix.
- **Settings GitHub URL:** Placeholder needs to be replaced with canonical repo URL per project policy.

## Files of Note

- `TopLevelBackStack.kt` — The core: multi-stack flattening, lazy seeding, LRU back-fallback, safe Saver.
- `MainShell.kt` — Route-driven `AnimatedVisibility` for bottom bar; shared transition scope setup.
- `TabKey.kt` — Tab enum; tells `NavDisplay` how to wire the three top-level screens.
- Deleted: `feature/landing/`, `feature/gallery/`, `SettingsSheet`, `PlaygroundNavHost`.
