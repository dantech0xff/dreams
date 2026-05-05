# Phase 06 — Category D: RenderEffect Post-Processing on Composables

## Context Links
- `plan.md`
- `phase-02-lesson-framework.md` (uses `AgslCanvas` RenderEffect mode)
- `phase-05-category-c-noise.md` (dissolve + displacement reuse `NOISE_HELPERS`)
- Chet Haase RenderEffect post: https://medium.com/androiddevelopers/agsl-made-in-the-shade-r-7d06d14fe02a

## Overview
- **Priority:** P2
- **Status:** pending
- **Effort:** 4h
- 6 lessons applying AGSL as `RenderEffect` to existing Composable content (image + text card). First category to use `setInputShader("content", ...)` pattern.

## Key Insights
- `RenderEffect.createRuntimeShaderEffect(shader, inputName)` requires the AGSL `uniform shader inputName;` declaration matching the Kotlin string.
- Sample input via `inputName.eval(fragCoord)` in AGSL — fragCoord is in pixel space.
- `RenderEffect` MUST be `remember`'d keyed on shader source — re-allocating per recomposition tanks performance.
- Same sample Composable (image + headline + body text "Sample Card") reused across all 6 lessons → DRY via `SampleContent.kt`.
- Ripple-on-tap reads `pointerInput { detectTapGestures }` → writes uniform centerX/centerY + tap time → shader animates outward wave.

## Requirements
- 6 lessons render the same SampleContent with different post-FX.
- All 6 are interactive: blur (radius slider), aberration (strength slider), ripple (tap to trigger), dissolve (auto-loop), displacement (animated), pixelate (cell size slider).
- No allocations per frame (verified via Layout Inspector / quick profile).

## Lessons
| # | id | Title | New concept | AGSL trick |
|---|----|-------|-------------|------------|
| 1 | postfx-01-blur | Box Blur | input shader sampling | avg of 9 `content.eval(coord+offset)` |
| 2 | postfx-02-chromatic-aberration | Chromatic Aberration | per-channel offset | sample R/G/B at offset coords |
| 3 | postfx-03-ripple-tap | Ripple on Tap | pointer → uniform | offset coord by `sin(dist*k - t)*amp` |
| 4 | postfx-04-dissolve | Dissolve Transition | noise-mask alpha | discard via `step(threshold, fbm(uv))` * mix |
| 5 | postfx-05-displacement-glass | Liquid Glass Displace | normal-map style offset | sample at `uv + (fbm-0.5)*strength` |
| 6 | postfx-06-pixelate | Pixelate / Mosaic | floor + cell size | sample at `floor(uv*cells)/cells` |

## Architecture
```
ui/playground/common/
  SampleContent.kt          // shared image + text card composable
shaders/posteffect/
  Blur.kt
  ChromaticAberration.kt
  RippleTap.kt              // exposes pointerInput inside lesson
  Dissolve.kt
  DisplacementGlass.kt
  Pixelate.kt
```

`SampleContent` ships with 1 bundled drawable (royalty-free landscape) + headline + paragraph; loaded via Coil from `painterResource`.

## Related Code Files
**Create:** files above + add 1 drawable `res/drawable/sample_landscape.webp` (sourced royalty-free, ≤200KB).

**Modify:** `LessonRegistry.bootstrap()`.

## Implementation Steps
1. Pick royalty-free sample image (Unsplash/Pexels CC0); add to drawables.
2. Build `SampleContent` composable.
3. Extend `LessonModel` (if needed) with `postEffectContent: (@Composable () -> Unit)?` slot — default to `SampleContent`.
4. Implement lesson 1 (blur) end-to-end first; resolve any `setInputShader` wiring issues here.
5. Implement lessons 2→6.
6. RippleTap: add `pointerInput(Unit) { detectTapGestures { offset -> writeUniform("center", offset.x, offset.y); writeUniform("tapTime", currentTime) } }`.
7. Profile each lesson on mid-range device — confirm no per-frame allocations in Logcat.

## Todo List
- [ ] Sample drawable added + license note in README
- [ ] `SampleContent.kt`
- [ ] 6 lesson files registered
- [ ] Ripple tap interaction works
- [ ] Per-frame allocation check passes

## Success Criteria
Tapping any postfx lesson shows live image+text with the named effect applied; ripple lesson responds to taps; no jank.

## Risk Assessment
| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| `RenderEffect` re-allocated each recomp | High if naive | High | `remember(src){ shader }` + `remember(shader,uniformsDep){ effect }` |
| Sample image asset bloat | Low | Low | Use webp, ≤200KB |
| `setInputShader` name mismatch silently sampling black | Med | Med | Document in `AgslCanvas` KDoc; cold-start validator can't catch this — add lesson-level smoke check |

## Security Considerations
- Drawable license MUST be CC0 / public domain — note source in README.
- No file/network IO.

## Next Steps
Phase 07 builds on Categories B+C+D for the showcase combos.
