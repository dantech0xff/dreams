---
title: Material 3 NavigationBar — Visibility, Icons, UX Patterns
date: 2026-05-06
status: DONE
scope: Material3 BOM 2026.02.01, Compose
---

# Findings

## 1. When to Hide Bottom NavigationBar (Material 3 spec)

Material 3 guidelines recommend hiding the NavigationBar on:
- Fullscreen / immersive content (video, photo viewers, full-bleed shaders) — applies to our `ShowcaseScreen` (full-bleed AGSL canvas) and likely `LessonDetailScreen` (full-screen detail with hero image and sliders, where bottom space matters for scroll).
- Modal flows / focused tasks (forms with bottom actions).

For Dreams: hide bar on `Route.LessonDetail` and `Route.Showcase`. Show on `LessonRoot`, `LessonList`, `ShowcaseRoot`, `SettingsRoot`.

Reference: https://m3.material.io/components/navigation-bar/guidelines

## 2. Hiding Technique — Recommendation: AnimatedVisibility in `bottomBar` slot, route-driven

Compared options:

| Option | Pros | Cons |
|---|---|---|
| (a) `AnimatedVisibility` wrapping NavigationBar in Scaffold's `bottomBar` slot | Smooth slide animation, single source of truth, plays well with edge-to-edge | Slight Scaffold inset wobble at boundary; mitigated by `slideInVertically/slideOutVertically` |
| (b) Conditionally include bottomBar (param swap) | Simplest | No animation, jarring snap, layout jump |
| (c) `CompositionLocal` overridden by each screen | Decentralized | Screens must opt-in; easy to forget; harder to reason about; non-canonical |
| (d) Read top of active back stack from shell (computed) | Centralized + animated | Same as (a), with route-aware logic — this is what (a) becomes when implemented properly |

**Recommendation: (a) + (d) combined** — `AnimatedVisibility(visible = isFullscreenRoute(top))` inside Scaffold's bottomBar; visibility derived from `topLevel.backStack.lastOrNull()`.

Rationale: smooth slide, matches reduced-motion via `AnimatedVisibility` inheriting `LocalDensity` and our motion state (we'll wrap with a 0ms tween when reduced-motion=true).

```kotlin
val top by remember(topLevel) { derivedStateOf { topLevel.backStack.lastOrNull() } }
val showBar = top is Route.LessonRoot || top is Route.LessonList ||
              top is Route.ShowcaseRoot || top is Route.SettingsRoot

Scaffold(
    bottomBar = {
        AnimatedVisibility(
            visible = showBar,
            enter = if (motion.reducedMotion) fadeIn(snap()) else slideInVertically { it },
            exit = if (motion.reducedMotion) fadeOut(snap()) else slideOutVertically { it },
        ) { DreamsBottomBar(...) }
    }
)
```

## 3. Insets When Bar Hidden

With `AnimatedVisibility` inside Scaffold's `bottomBar` slot, Scaffold automatically recomputes `padding` for the content lambda. When the bar slides out, content reclaims that vertical space (Scaffold reads bar size via the slot's measured height; `AnimatedVisibility` height collapses to 0 when invisible).

For edge-to-edge, ensure `MainActivity.enableEdgeToEdge()` is set (already is, line 13). Fullscreen screens (`ShowcaseScreen`) use `Modifier.fillMaxSize()` which now extends to bottom system bar. They handle their own insets via `Modifier.systemBarsPadding()` if needed (current ShowcaseScreen does NOT — confirm with manual check; if back arrow is under nav gesture area, add `Modifier.navigationBarsPadding()` to the back-text Box).

## 4. Tap-Current-Tab-To-Pop-To-Root

Material 3 / Android UX: this is the expected, ubiquitous pattern (Now-in-Android, Instagram, YouTube, Twitter all do it). Implementation is ~10 lines on the `TopLevelBackStack` helper (see researcher-260506-0724-navigation3-multi-stack.md §4). **Recommend including** — cost is trivial, UX win is real.

## 5. Tab Icons (material-icons-core only — no extended dep)

The `androidx.compose.material:material-icons-core` artifact ships a small curated set. Verified-available filled icons:

| Tab | Recommended | Alternates | Notes |
|---|---|---|---|
| Lesson | `Icons.Filled.Menu` | `Icons.Filled.List` | "Menu" = hamburger lines; "List" = bulleted list. Both core. |
| Showcase | `Icons.Filled.Star` | `Icons.Filled.PlayArrow` | Star = featured/showcase; PlayArrow = "play" connotation matches video-recording showcases. |
| Settings | `Icons.Filled.Settings` | — | Definitely in core. |

`Icons.Filled.Settings`, `Icons.Filled.Star`, `Icons.Filled.Menu`, `Icons.Filled.PlayArrow`, `Icons.Filled.List`, `Icons.Filled.Home` are all confirmed core. (The "extended" artifact = `material-icons-extended`, ~6MB; we want to avoid it.)

**Final picks:**
- Lesson → `Icons.Filled.List` (clearer than Menu for "list of categories")
- Showcase → `Icons.Filled.Star`
- Settings → `Icons.Filled.Settings`

Verify by importing from `androidx.compose.material.icons.filled.*` after declaring `implementation("androidx.compose.material:material-icons-core")` (likely already transitively present via Material3 — confirm in build.gradle.kts during phase-01).

## 6. NavigationBar Configuration for 3 Items

Material 3 spec: 3–5 destinations. Three is fine, on the lower end.

- **Labels: always show.** With only 3 items there is plenty of room; hiding labels would harm accessibility and discoverability. Use `NavigationBarItem` with `label = { Text(...) }` (no `alwaysShowLabel = false`).
- **Height:** Default Material 3 NavigationBar height = 80dp; do NOT override.
- **Behavior on scroll:** No hide-on-scroll. Bottom bar is persistent for tab roots (only hides on detail/fullscreen via route-based logic above).
- **Selected item:** Use `Icons.Filled.*` for selected, can use `Icons.Outlined.*` for unselected — but Outlined ships only in `material-icons-extended`. To stay core-only: use Filled for both, rely on the M3 NavigationBarItem's built-in indicator pill + selected color tinting for selection state. Acceptable per M3 spec.

## References

- Material 3 NavigationBar guidelines: https://m3.material.io/components/navigation-bar/guidelines
- Material 3 NavigationBar specs: https://m3.material.io/components/navigation-bar/specs
- Compose Material Icons (core artifact): https://mvnrepository.com/artifact/androidx.compose.material/material-icons-core
- Compose Material3 NavigationBar API: https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary

# Unresolved Questions

- Confirm `material-icons-core` is on Dreams' classpath (Material3 BOM may pull it transitively, but explicit is safer). Phase-01 step.
- Verify `ShowcaseScreen.kt` back arrow position works edge-to-edge when bar hides; may need `navigationBarsPadding()` added.

**Status:** DONE
