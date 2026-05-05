# Phase 02 — Lesson Framework & Navigation

## Context Links
- `plan.md`
- `reports/researcher-260505-0001-agsl-compose-integration.md` (RuntimeShader, ShaderBrush, RenderEffect, withFrameNanos patterns)
- `phase-01-project-setup.md` (deps + package layout)

## Overview
- **Priority:** P1 (blocks all lesson phases 03–07)
- **Status:** pending
- **Effort:** 5h
- Build the reusable scaffolding every lesson plugs into: domain model, registry, navigation, gallery + detail screens, AGSL canvas helper, time-uniform helper, parameter slider component, runtime compile-error guard.

## Key Insights
- One `LessonModel` data class fits all four categories — cheaper than per-category interfaces (KISS).
- `AgslCanvas` exposes two render modes: `Brush` (Category A/B/C) and `RenderEffect` (Category D). Showcase phase 07 reuses both.
- `withFrameNanos` inside `LaunchedEffect(Unit)` writes `time` uniform once per frame — never recreate `RuntimeShader`.
- Cold-start registry validator catches AGSL syntax errors before user sees a crash; emits a list to logcat in `BuildConfig.DEBUG`.
- `LessonRegistry` is a top-level `object` returning `ImmutableList<LessonModel>` — no DI needed.

## Requirements
- Gallery shows 4 category tabs; tapping a lesson navigates to detail.
- Detail screen renders Live Preview + collapsible AGSL Source + Parameter Controls slot.
- AGSL compile errors render as a red diagnostic card in detail (no crash).
- One smoke-test lesson ("Hello AGSL", solid red) proves the pipeline end-to-end.

## Architecture
```
ui/playground/
  PlaygroundNavHost.kt        // routes: gallery, lesson/{id}
  gallery/
    GalleryScreen.kt          // tabs + LazyColumn of LessonCard
    LessonCard.kt
  lesson/
    LessonDetailScreen.kt     // 3 slots: preview / source / controls
    AgslSourceViewer.kt       // monospace, syntax-trivial highlight
    ParameterSlider.kt
  common/
    AgslCanvas.kt             // Brush + RenderEffect variants
    ShaderTimeUniform.kt      // withFrameNanos helper
    AgslErrorCard.kt
data/lesson/
  LessonModel.kt
  LessonCategory.kt           // BASICS, SDF, NOISE, POSTFX, SHOWCASE
  LessonControl.kt            // sealed: FloatRange, ColorPicker
  LessonRegistry.kt           // object, registers lessons by category
shaders/basics/
  HelloAgsl.kt                // smoke-test lesson
```

## Related Code Files
**Modify:**
- `app/src/main/java/com/dantech/dreams/MainActivity.kt` — set content to `PlaygroundApp()` composable

**Create:** every file listed in Architecture block above.

## Implementation Steps
1. `LessonModel(id, title, category, complexity 1–5, conceptIntro, agslSource, controls, screenRecordingHint?)`.
2. `LessonControl` sealed class: `FloatRange(name, min, max, default)`, `ColorPicker(name, default)`.
3. `LessonRegistry`: `private val all = mutableListOf<LessonModel>()`, `fun register(m: LessonModel)`, `fun byCategory(c)`. Lessons self-register from their file (`init` block on a top-level `object HelloAgsl`).
4. `AgslCanvas(modifier, shaderSrc, controls, content?)`:
   - Remember `RuntimeShader(shaderSrc)` keyed on source.
   - Wrap construction in try/catch → emit `AgslErrorCard`.
   - Pull `time` uniform via `ShaderTimeUniform`, `resolution` via `onSizeChanged`.
   - Brush variant: `Modifier.drawBehind { drawRect(brush = ShaderBrush(shader)) }`.
   - RenderEffect variant: wrap `content` in `Modifier.graphicsLayer { renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect() }`.
5. `ShaderTimeUniform(shader, uniformName="time")` — `LaunchedEffect(Unit) { while (true) { withFrameNanos { shader.setFloatUniform(uniformName, it / 1e9f) } } }`.
6. `ParameterSlider(control: FloatRange, value, onValue)` → Material3 `Slider` + label.
7. `GalleryScreen` — `TabRow` of categories, `LazyColumn` of `LessonCard`.
8. `LessonDetailScreen(id)` — `Column { LivePreview; AgslSourceViewer(collapsible); ControlsList }`.
9. `PlaygroundNavHost` — `NavHost` with `composable("gallery")` and `composable("lesson/{id}")`.
10. Cold-start validator: `LessonRegistry.validateAll()` called from `Application` (or first composition) — iterates and tries `RuntimeShader(src)` on each, logs failures.
11. Register `HelloAgsl` smoke-test lesson (returns solid red).
12. Run on API 33 emulator, confirm gallery → detail → red square.

## Todo List
- [ ] `LessonModel` + `LessonCategory` + `LessonControl`
- [ ] `LessonRegistry` with cold-start validator
- [ ] `AgslCanvas` (Brush + RenderEffect modes)
- [ ] `ShaderTimeUniform` helper
- [ ] `ParameterSlider`, `AgslSourceViewer`, `AgslErrorCard`
- [ ] `GalleryScreen` + `LessonCard`
- [ ] `LessonDetailScreen`
- [ ] `PlaygroundNavHost` wired into `MainActivity`
- [ ] `HelloAgsl` smoke lesson registered + verified on device

## Success Criteria
- App launches, gallery shows the smoke lesson, detail screen renders the red square at 60fps.
- Intentionally breaking the smoke shader's source produces a red diagnostic card, not a crash.

## Risk Assessment
| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| RenderEffect re-allocated each frame | Med | High (jank) | `remember(shaderSrc) { RuntimeShader(it) }`; pass shader via state, not re-create |
| `time` uniform tied to system uptime causes float precision loss after ~1h | Low | Low | Modulo by `1000f` before writing |
| Self-registration via `object init` not triggered until referenced | Med | Med | Touch each lesson object once from `LessonRegistry.bootstrap()` called at app start |

## Security Considerations
None.

## Next Steps
Phases 03–06 each add 6 lessons by populating one shaders/{category}/ folder. Phase 07 reuses `AgslCanvas` for showcase screens.
