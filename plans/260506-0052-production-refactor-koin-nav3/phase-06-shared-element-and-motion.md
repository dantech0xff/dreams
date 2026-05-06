# Phase 06 — Shared Element + Motion

## 1. Context Links
- Parent: [plan.md](plan.md)
- Depends on: [phase-04-navigation3-migration.md](phase-04-navigation3-migration.md), [phase-05-datastore-prefs.md](phase-05-datastore-prefs.md)
- Inputs: `research/researcher-02-nav3-shared-elements.md` §4, §5
- Docs: https://developer.android.com/develop/ui/compose/animation/shared-elements | https://developer.android.com/jetpack/compose/animation/shared-element

## 2. Overview
- **Date:** 2026-05-06
- **Description:** Wrap `NavDisplay` in `SharedTransitionLayout`. Add `Modifier.sharedBounds()` on gallery card thumbnail → lesson detail hero box. Detect reduced-motion (system `ANIMATOR_DURATION_SCALE == 0` OR user override) → swap to instant scene transition. Polish landing/showcase enter/exit fades.
- **Priority:** P2 (polish; non-blocking for shipping prior phases)
- **Implementation status:** pending
- **Review status:** pending

## 3. Key Insights
- `SharedTransitionLayout` is `@ExperimentalSharedTransitionApi` in Compose 1.7/1.8. Must opt-in.
- Compose BOM 2026.02.01 ships Compose UI 1.8.x → SharedTransitionLayout available.
- Plumbing `AnimatedVisibilityScope` from NavDisplay entry to children: NavDisplay's entry block exposes `AnimatedContentScope` (similar to `AnimatedVisibilityScope`); use `LocalSharedTransitionScope` + `LocalNavAnimatedVisibilityScope` provided helpers OR pass via composition local.
- **Shared element key strategy:** `"lesson-card-${lessonId}"` keyed off lesson id. Same key on gallery thumbnail (within `LessonCard`) and detail hero (`Box` wrapping `LessonPreview` in `LessonDetailScreen`). Showcase route is fullscreen-fade (no thumbnail in showcase entry — different visual model); skip shared elements for `Route.Showcase`.
- **Reduced-motion detection** (researcher-02 §5):
  ```kotlin
  fun systemAnimatorEnabled(ctx: Context): Boolean =
      Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
  ```
  Combine with `userPrefs.reducedMotionOverride` (phase-05). Effective: `reducedMotion = !systemAnimatorEnabled || userOverride`.
- Per-frame shader animation MUST stay running in reduced-motion mode (we already fixed this — commit `492eaee`). Reduced-motion only applies to *transition*, not *content*.

## 4. Requirements

### Functional
- Gallery → LessonDetail: card thumbnail morphs to detail hero region. ~300ms duration.
- Reduced-motion (system or user toggle): instant scene swap, no morph.
- Showcase entry: full-screen cross-fade (no shared element), 250ms.
- Landing → Gallery: cross-fade 200ms.
- Back nav: reverse animation runs.

### Non-Functional
- Shared-element opt-in confined to gallery↔detail flow (single `key` per lesson).
- AGSL canvas inside detail hero must continue rendering during transition (canvas keeps frame-loop active). If frame loop pauses during shared transition layout-pass, fall back to placeholder Color block.

## 5. Architecture

```
MainActivity
  └── DreamsTheme {
        val motion = rememberAppMotionState()  // reads system + user prefs
        SharedTransitionLayout {
          CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavDisplay(
              backStack,
              transitionSpec = motion.scenesSpec,        // tween / instant
              entryProvider = { entry ->
                entry<Route.Gallery> { GalleryScreen(...) }
                entry<Route.LessonDetail> { route -> LessonDetailScreen(...) }
                ...
              }
            )
          }
        }
      }
```

`AppMotionState` (in `core/motion/AppMotionState.kt`):
- `reducedMotion: Boolean` (combine system + user)
- `transitionDurationMs: Int` (300 vs 0)
- `scenesSpec: NavDisplay.SceneStrategy` or transition spec

Gallery thumbnail uses `Modifier.sharedBounds(rememberSharedContentState("lesson-card-$id"), animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current!!)`. Detail screen wraps hero box in matching modifier.

## 6. Related Code Files

### Modify
- `ui/feature/nav/PlaygroundNavHost.kt` — wrap NavDisplay in SharedTransitionLayout; provide composition locals
- `ui/feature/gallery/LessonCard.kt` — add `Modifier.sharedBounds(...)` on thumbnail
- `ui/feature/lesson/LessonDetailScreen.kt` — add matching `Modifier.sharedBounds(...)` on hero `Box` (LessonDetailScreen.kt:69–77 region)
- `ui/feature/landing/LandingScreen.kt` — content already inside `AnimatedContentScope` via NavDisplay; add `Modifier.animateEnterExit()` for hero text if desired (optional polish)

### Create
- `core/motion/AppMotionState.kt` — `data class` + `@Composable rememberAppMotionState(): AppMotionState`
- `core/motion/MotionLocals.kt` — `val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }` + `LocalNavAnimatedVisibilityScope`
- `core/motion/ReducedMotionDetector.kt` — pure fn `Settings.Global` reader (testable)

### Delete
- None

## 7. Implementation Steps

1. **Create `core/motion/ReducedMotionDetector.kt`:**
   ```kotlin
   fun systemAnimatorEnabled(context: Context): Boolean =
       Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
   ```

2. **Create `core/motion/AppMotionState.kt`:**
   ```kotlin
   data class AppMotionState(val reducedMotion: Boolean) {
       val transitionDurationMs: Int get() = if (reducedMotion) 0 else 300
   }
   @Composable
   fun rememberAppMotionState(prefsRepo: UserPrefsRepository = koinInject()): AppMotionState {
       val ctx = LocalContext.current
       val prefs by prefsRepo.prefsFlow.collectAsStateWithLifecycle(initialValue = UserPrefs.DEFAULT)
       val sysEnabled = remember { systemAnimatorEnabled(ctx) }
       return AppMotionState(reducedMotion = !sysEnabled || prefs.reducedMotionOverride)
   }
   ```

3. **Create `core/motion/MotionLocals.kt`:**
   ```kotlin
   val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
   val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }
   ```

4. **Edit `PlaygroundNavHost.kt`** — wrap NavDisplay:
   ```kotlin
   @OptIn(ExperimentalSharedTransitionApi::class)
   @Composable
   fun PlaygroundApp() {
       val motion = rememberAppMotionState()
       val backStack = rememberNavBackStack<Route>(Route.Landing)
       SharedTransitionLayout {
           CompositionLocalProvider(LocalSharedTransitionScope provides this) {
               NavDisplay(
                   backStack = backStack,
                   onBack = { backStack.removeLastOrNull() },
                   transitionSpec = if (motion.reducedMotion) snap() else fadeWithMs(motion.transitionDurationMs),
                   entryDecorators = listOf(
                       rememberSceneSetupNavEntryDecorator(),
                       rememberSavedStateNavEntryDecorator(),
                       rememberViewModelStoreNavEntryDecorator(),
                   ),
                   entryProvider = entryProvider {
                       entry<Route.Landing> {
                           CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                               LandingScreen(onOpenGallery = { backStack.add(Route.Gallery) })
                           }
                       }
                       entry<Route.Gallery> {
                           CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                               GalleryScreen(onLessonClick = { backStack.add(routeForLessonId(it)) })
                           }
                       }
                       entry<Route.LessonDetail> { route ->
                           CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                               LessonDetailScreen(onBack = { backStack.removeLastOrNull() })
                           }
                       }
                       entry<Route.Showcase> { route ->
                           CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                               ShowcaseScreen(onBack = { backStack.removeLastOrNull() })
                           }
                       }
                   }
               )
           }
       }
   }
   ```
   Note: exact NavDisplay `transitionSpec` API may differ; if NavDisplay 1.1.0-rc01 only supports SceneStrategy not transitionSpec, gate animations via `AnimatedContent` inside each entry — adjust per Nav3 final API.

5. **Edit `LessonCard.kt`:** wrap card visual in:
   ```kotlin
   val sharedScope = LocalSharedTransitionScope.current
   val visScope = LocalNavAnimatedVisibilityScope.current
   val mod = if (sharedScope != null && visScope != null) {
       with(sharedScope) {
           Modifier.sharedBounds(
               sharedContentState = rememberSharedContentState("lesson-card-${lesson.id}"),
               animatedVisibilityScope = visScope,
           )
       }
   } else Modifier
   Card(modifier = mod.clickable { onClick() }) { ... }
   ```

6. **Edit `LessonDetailScreen.kt:69–77`** (the hero `Box`): apply same `Modifier.sharedBounds` with key `"lesson-card-${lesson.id}"`. Ensure modifier is *outermost* on the Box that wraps `LessonPreview`.

7. **Reduced-motion smoke path:** in adb, `settings put global animator_duration_scale 0`. Launch app. Tap a lesson → instant swap, no morph. `settings put global animator_duration_scale 1` to restore.

8. **User-toggle UI (deferred to phase-08 settings sheet):** for now, `UserPrefsRepository.setReducedMotion()` write path exists from phase-05 but no UI surface. Document in todo for phase-08.

9. **Verify shader doesn't pause during transition:** record video of gallery→detail morph and confirm AuroraRibbons (in landing) and the lesson canvas time keep ticking. `withFrameNanos` loop in `rememberShaderTime` is composition-scoped, so it should keep running as long as composables exist.

10. **Build + manual QA pass:** card morph feels right (not jittery), back-morph reverses cleanly, fullscreen showcase fades cleanly without shared element collision.

## 8. Todo
- [ ] `ReducedMotionDetector` + `AppMotionState` + `MotionLocals` created
- [ ] `PlaygroundNavHost` wrapped in `SharedTransitionLayout`
- [ ] `LessonCard` thumbnail uses `Modifier.sharedBounds` keyed on lesson id
- [ ] `LessonDetailScreen` hero Box uses matching `Modifier.sharedBounds`
- [ ] Reduced-motion path verified via adb global animator scale 0
- [ ] Reduced-motion user toggle wired (UI surface in phase-08)
- [ ] No shader animation stutter during transition

## 9. Success Criteria
- Gallery card → detail hero: smooth 300ms morph at 60fps on emulator.
- `adb shell settings put global animator_duration_scale 0` → no morph, instant scene swap, lessons still render.
- `userPrefs.reducedMotionOverride = true` → same instant behavior even with system anims on.
- Showcase route uses simple cross-fade (no shared-element coupling).

## 10. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| `SharedTransitionLayout` API breaking change between Compose UI 1.8 stable and BOM pin | Med | High | Pin Compose UI 1.8.x explicitly via BOM. Annotate all sites with `@OptIn(ExperimentalSharedTransitionApi::class)`. If API regresses, fall back to plain `AnimatedContent` cross-fade |
| AGSL canvas re-creates RuntimeShader during transition (compile cost ≈ 1-5ms × 60fps drops frames) | Med | High | `AgslBrushCanvas`/`AgslRenderEffectCanvas` already memoize via `rememberRuntimeShader`. Verify the shared-bounds layout pass doesn't invalidate the remember. If it does, hoist shader into VM-scoped or composition-stable holder |
| Shared-element key collision between gallery and detail | Low | Med | Use unique-per-lesson key `lesson-card-${id}`. Detail uses same — guaranteed match |
| Reduced-motion check via `Settings.Global` requires no permissions but caches at compose-init only | Low | Low | Keys live for app lifetime once read. Acceptable; recheck on activity recreation already covers it |
| Composition local scope plumbing is brittle (forgetting CompositionLocalProvider in one entry breaks anim) | Med | Low | Pattern-test all entries; add lint helper in phase-08 if recurring |

## 11. Security Considerations
- `Settings.Global.ANIMATOR_DURATION_SCALE` read is permission-free.
- No new attack surface.

## 12. Next Steps
- Phase-07 includes UI test for: tap gallery card → detail screen rendered with lesson hero.
- Phase-08 surfaces reduced-motion toggle in a settings sheet/screen.

## Unresolved Questions
- **NavDisplay native transition API:** does `NavDisplay` 1.1.0-rc01 take `transitionSpec` as first-class param, or is animation governed entirely by `SceneStrategy`? Adjust step-4 once verified.
- **Shared-bounds vs sharedElement:** `sharedBounds` better for size-changing morphs (researcher-02 §4). Lesson card → fullscreen detail is a major resize → `sharedBounds`. Confirm during impl.
- **AGSL canvas remember-stability under shared transitions:** measure on real device.
