---
title: "Code Review — Bottom Tab Navigation Rework"
agent: code-reviewer
date: 2026-05-06 08:11
plan: plans/260506-0724-bottom-tab-navigation-rework/plan.md
focus: nav shell + per-tab back stack + lessonlist/showcase/settings screens
verdict: DONE_WITH_CONCERNS
---

## Scope

Files reviewed (commit-pending working tree):

- `app/src/main/java/com/dantech/dreams/ui/feature/nav/{MainShell,TopLevelBackStack,DreamsBottomBar,TabKey,Route}.kt`
- `app/src/main/java/com/dantech/dreams/ui/feature/lessonlist/*.kt`
- `app/src/main/java/com/dantech/dreams/ui/feature/showcase/{ShowcaseListScreen,ShowcaseListViewModel,ShowcaseListUiState,ShowcaseScreen,ShowcaseViewModel}.kt`
- `app/src/main/java/com/dantech/dreams/ui/feature/settings/{SettingsScreen,AboutAgslSheet}.kt`
- `app/src/main/java/com/dantech/dreams/ui/feature/common/LessonCard.kt`
- `app/src/main/java/com/dantech/dreams/ui/feature/lesson/{LessonDetailScreen,LessonDetailViewModel}.kt`
- `app/src/main/java/com/dantech/dreams/data/lesson/{LessonCategory,LessonRepositoryImpl,LessonRegistry}.kt`
- `app/src/main/java/com/dantech/dreams/domain/lesson/LessonRepository.kt`
- `app/src/main/java/com/dantech/dreams/MainActivity.kt`
- `app/src/main/java/com/dantech/dreams/core/di/{FeatureModule,DataModule}.kt`
- `app/src/main/java/com/dantech/dreams/core/motion/{AppMotionState,MotionLocals}.kt`
- `app/src/test/java/com/dantech/dreams/support/FakeLessonRepository.kt`
- `app/src/test/java/com/dantech/dreams/core/di/KoinModulesCheckTest.kt`

## Overall

Solid rework. Helper class is small, focused, and follows the official Nav3 multi-stack recipe. Saver implementation is correct for the current closed route set. Composition ordering (single SharedTransitionLayout above Scaffold above NavDisplay) matches Nav3 docs. Per-NavEntry ViewModelStore preserves VMs across rotation; Koin `parametersOf(...)` re-injects after process death. Plan-stated visibility/pop rules are implemented exactly.

Two issues warrant attention before ship: (1) MAJOR — system back from the home tab does not exit the app; jumps tab instead. (2) MAJOR — saver `restore()` will crash the activity if persisted state contains an unknown encoded route after a future Route refactor.

---

## Findings

### CRITICAL
*(none)*

### MAJOR

#### M1 — Back from home tab jumps to LRU previous tab; never exits app
**File:** `app/src/main/java/com/dantech/dreams/ui/feature/nav/TopLevelBackStack.kt:58-72`

`removeLast()` at depth 1 falls through to "pop LRU history" whenever `topLevelStacks.size > 1`. Initial seed inserts all three tabs in `LinkedHashMap`, so `keys = [LessonRoot, ShowcaseRoot, SettingsRoot]`. Pressing system back from `LessonRoot` (the user's first launch state, no tab switches yet) computes `previous = keys[lastIndex - 1] = ShowcaseRoot` and jumps there — instead of exiting the app.

**Why it's wrong.** The "LRU previous tab" pattern only makes sense if the user explicitly switched tabs in this session. Default Material expectation: pressing back on the start destination exits.

**Fix.** Track *visited* (LRU-touched) tabs separately from the seeded set, or only pop history when `topLevelKey` is not the start route AND the LRU "previous" was actually visited this session. Simplest:

```kotlin
// Track "visited" via LinkedHashMap re-insertion — switchTopLevel already does this.
// removeLast: only pop tab history if user actually switched at least once.
private var hasSwitchedTab = false
fun switchTopLevel(root: Route) { ...; hasSwitchedTab = true }

fun removeLast() {
    val current = topLevelStacks[topLevelKey] ?: return
    if (current.size > 1) { current.removeAt(current.lastIndex); rebuild(backStack); return }
    if (!hasSwitchedTab) return  // let NavDisplay surface back to system → activity exits
    val keys = topLevelStacks.keys.toList()
    val previous = keys[keys.lastIndex - 1]
    topLevelKey = previous
    rebuild(backStack)
}
```

When `removeLast()` returns without mutating `backStack`, NavDisplay falls through to platform back → activity finishes. Verify by `adb shell input keyevent KEYCODE_BACK` from `LessonRoot` — should exit, not jump.

Also persist `hasSwitchedTab` in the saver (one bit; trivially encodable).

#### M2 — Saver `restore` crashes on unknown encoded route (forward-compat bomb)
**File:** `app/src/main/java/com/dantech/dreams/ui/feature/nav/TopLevelBackStack.kt:108-116, 128-134`

`decodeRoute` calls `error("Unknown encoded route: $s")` on any unrecognized prefix. If a future build adds a route (or removes one), restoring saved state from a previous app version → `IllegalStateException` thrown inside `Saver.restore` → `rememberSaveable` lambda throws inside composition → activity crashes on launch with no recovery path. Same problem if the saved-state buffer is corrupt or empty.

**Why it's wrong.** Savers must be defensive. Mobile apps must survive version upgrades and corrupt state.

**Fix.** Wrap `restore` in `runCatching { ... }.getOrElse { TopLevelBackStack.create(initialStart) }`. Tag the saver with a version int; on mismatch, fall back. Skeleton:

```kotlin
private val TopLevelBackStackSaver = Saver<TopLevelBackStack, List<List<String>>>(
    save = { ... },
    restore = { saved ->
        runCatching {
            require(saved.isNotEmpty() && saved[0].size >= 2 && saved[0][0] == "TOP")
            val top = decodeRoute(saved[0][1])
            val stacks = saved.drop(1).associate { row ->
                require(row.isNotEmpty())
                decodeRoute(row[0]) to row.drop(1).map(::decodeRoute)
            }
            require(stacks.isNotEmpty())
            TopLevelBackStack.restore(top, stacks)
        }.getOrNull()  // null → rememberSaveable will fall through to factory
    },
)
```

Returning `null` from `restore` makes `rememberSaveable` invoke its factory — clean recovery.

### MINOR

#### m1 — Unused import in MainShell
**File:** `app/src/main/java/com/dantech/dreams/ui/feature/nav/MainShell.kt:15`

`import androidx.compose.material3.Text` — not referenced. Lint flag.

**Fix.** Delete the line.

#### m2 — Dead `rememberCoroutineScope()` call in AboutAgslSheet
**File:** `app/src/main/java/com/dantech/dreams/ui/feature/settings/AboutAgslSheet.kt:17`

`rememberCoroutineScope()` is invoked, result discarded. Allocates a `CoroutineScope` per recomposition slot for nothing. Looks like leftover from copy-paste of old landing version.

**Fix.** Remove line 17 and the `rememberCoroutineScope` import on line 10.

#### m3 — `koinViewModel(key = categoryName)` is redundant given NavEntry isolation
**File:** `app/src/main/java/com/dantech/dreams/ui/feature/lessonlist/LessonListScreen.kt:28`

Each `Route.LessonList(categoryName)` produces its own NavEntry with its own ViewModelStore via `rememberViewModelStoreNavEntryDecorator`. Same screen never re-uses a ViewModelStore across different `categoryName` values. The explicit `key = categoryName` is therefore dead defense; harmless but signals confusion about the surrounding model.

**Fix (optional KISS).** Drop the `key`:
```kotlin
vm: LessonListViewModel = koinViewModel { parametersOf(categoryName) },
```

#### m4 — `LessonListViewModel` reads `repo.byCategory` synchronously in field init; favorites flicker on first frame
**File:** `app/src/main/java/com/dantech/dreams/ui/feature/lessonlist/LessonListViewModel.kt:22-36`

`buildInitial()` runs at field-init (before `init {}` block). `LessonRegistry.bootstrap()` is triggered by `LessonRepositoryImpl.init`, which Koin invokes the first time the singleton is resolved — that happens before VM construction. So the lessons are present (verified). However, `prefs.prefsFlow` first emission is async (DataStore disk read), so the initial `_ui` has `favorites = persistentSetOf()`. UI renders empty hearts then re-renders with favorites filled in once `collect { ... update }` fires. Brief flicker on cold-launched LessonList.

**Why it's only minor.** Visible only ~50-200ms; cosmetic.

**Fix (optional).** Initialize `_ui` with `prefsFlow` `.first()` collected synchronously inside an inline `runBlocking` (no — bad on main thread) — better: gate UI rendering on a `loaded: Boolean` flag, or accept the flicker (KISS). Recommend leaving as-is; flag for later only if QA reports it.

#### m5 — `decodeRoute("LL:")` produces `LessonList(categoryName = "")`; survives but renders "Unknown category"
**File:** `app/src/main/java/com/dantech/dreams/ui/feature/nav/TopLevelBackStack.kt:112-114`

If a category enum ever gains/renames a value and old saved state restores `LL:OLDNAME`, `LessonCategory.valueOf` returns null → screen shows "Unknown category". Acceptable degradation but combine with M2 fix to nuke whole saved state instead of leaving user stuck on an error screen.

#### m6 — `derivedStateOf { ... lastOrNull() }` reads through full SnapshotStateList
**File:** `app/src/main/java/com/dantech/dreams/ui/feature/nav/MainShell.kt:45-47`

`backStack.lastOrNull()` triggers a Snapshot read on the *whole* list (any structural change re-runs the lambda). For ≤10 entries this is irrelevant. NIT — keep as-is.

### NIT

#### n1 — `SettingsScreen.kt:39` uses placeholder GitHub URL
`https://github.com/dantech0xff/dreams` — verify the public repo actually exists at that path, otherwise external "GitHub" row dead-ends users in browser.

#### n2 — `Route.LessonList(categoryName: String)` — string instead of enum reference
Plan locks this as an `String` to keep `Route` `@Serializable` without bringing the enum into the nav layer. Acceptable. KISS over type-safety here. NIT only because `LessonCategory.valueOf` swallows the typo case.

#### n3 — `TabKey` hard-codes 3 tabs as enum
Per plan KISS decision. Acceptable. Future tab additions = extend enum + handle decode case in saver. Documented.

#### n4 — `ShowcaseScreen.kt:24` default `vm = koinViewModel()` would crash if invoked without args
The `MainShell` entry overrides this with `parametersOf(route.lessonId)`, so unreachable. But the dangling default is a footgun if `ShowcaseScreen` is ever called from elsewhere (preview, test). Either drop the default or make it explicit-required.

#### n5 — `popToRoot()` uses `current.first()` then `clear/add` — could just `removeRange(1, lastIndex+1)`
Two-allocation strategy; trivial. Style nit.

---

## Adversarial scout — edge-case attempts

| # | Scenario | Verdict | Notes |
|---|----------|---------|-------|
| 1 | Rotate mid drill-down (`LessonList(BASICS) → LessonDetail("test-basics-1")`) | PASS | `rememberSaveable` round-trips; `rememberViewModelStoreNavEntryDecorator` preserves VMs |
| 2 | Process death restoration after deep nav across two tabs (Lesson@Detail, switched to Showcase, killed) | PASS — but see M2 | Saver encoding correct for current routes; LRU order in `LinkedHashMap` preserved by `associate` |
| 3 | Tab switch during NavDisplay transition into Detail | PASS | NavDisplay cancels in-flight transitions; bar `AnimatedVisibility` correctly re-derives |
| 4 | Tap current tab at depth 1 (Settings tab root, tap Settings) | PASS (no-op) | `popToRoot` early-returns when `size <= 1` |
| 5 | System back from `LessonRoot` (user just opened the app) | **FAIL → M1** | Jumps to ShowcaseRoot instead of exiting |
| 6 | Restore from corrupt/empty saved state | **FAIL → M2** | `decodeRoute` throws `IllegalStateException` inside composition |
| 7 | Drill `LessonList → LessonDetail`, rotate, drill again, rotate; back twice | PASS | Each Detail entry has its own ViewModelStore by NavEntry; saver round-trips list |
| 8 | Drill into Showcase, kill app, relaunch | PASS | `routeForLessonId` not used in restore path; encoded as `SH:...` directly |
| 9 | App-version upgrade adds a new Route subtype, user has saved state without it | **FAIL → M2** | Same crash path; M2 fix covers this |

---

## Plan TODO check

Plan locked decisions vs implementation:

| Decision | Implemented? |
|---|---|
| Single `NavDisplay` + `TopLevelBackStack` | ✓ |
| `AnimatedVisibility` route-driven bottom bar | ✓ |
| `MainShell` rename | ✓ (PlaygroundNavHost gone, MainActivity wires MainShell) |
| `LessonCategory.lessonOnly()` filter retained | ✓ |
| `LessonRepository.showcases()` accessor added | ✓ |
| Tap-current-tab → pop-to-root | ✓ (DreamsBottomBar.kt:17) |
| About AGSL → settings sheet | ✓ |

Phase 5 cleanup: confirmed no surviving references to `PlaygroundApp`, `LandingScreen`, `LandingViewModel`, `GalleryScreen`, `GalleryViewModel`. Tests for the deleted VMs removed. `FakeLessonRepository.showcases()` implemented.

---

## Recommended actions (priority order)

1. **M1** — Add `hasSwitchedTab` gate so back from home tab exits the app. Persist in saver.
2. **M2** — Wrap saver `restore` in `runCatching { ... }.getOrNull()` and validate row shape; falls back to fresh state cleanly across app upgrades and corruption.
3. **m1, m2** — Trivial cleanup: drop unused import + dead `rememberCoroutineScope()` call.
4. **n1** — Verify GitHub URL is live before ship.
5. **m3** — Optional KISS: drop redundant `key=` from `LessonListScreen` Koin call.

Items M1+M2 are blocking quality gates; everything else is post-ship polish.

---

## Metrics

- Files changed (net): ~25 (creates) + 7 (deletes) per plan summary
- LOC scanned for review: ~1.5k
- New helper LOC: TopLevelBackStack 140, MainShell 137, DreamsBottomBar 26, TabKey 27 — well under per-file 200-line target
- Lint: not run cleanly (BOM resolution issue noted in task; unrelated)
- Tests: green (validated upstream)
- Type safety: full Kotlin null-safety; `Route` is sealed, exhaustive when-branches enforced

---

## Unresolved questions

1. Should `M1` exit go via `Activity.finish()` explicitly, or rely on `NavDisplay.onBack` returning unhandled (current behavior is unclear in nav3 1.1.1 docs — verify)? If NavDisplay always swallows back when `backStack` non-empty, the LRU-pop guard alone is insufficient and needs a `BackHandler(enabled = !atHomeRoot) { topLevel.removeLast() }` wrapper instead.
2. Persisted-state schema versioning — is there appetite for a one-byte `version` prefix on the saver (cheap insurance for future Route additions)?
3. `ShowcaseListScreen` shows raw `screenRecordingHint` to end users ("Recording: ...") — was this intended public copy or an internal hint for the dev recording demos? UX-only, not a code issue.

---

**Status:** DONE_WITH_CONCERNS
**Summary:** Rework is structurally clean and matches plan; two MAJOR bugs (back-to-exit behavior, saver crash on unknown route) should be fixed before ship.
