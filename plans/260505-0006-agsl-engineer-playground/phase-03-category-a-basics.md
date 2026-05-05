# Phase 03 — Category A: Basics (uniforms, coords, gradients)

## Context Links
- `plan.md`
- `phase-02-lesson-framework.md` (uses `AgslCanvas` Brush mode + `LessonRegistry`)
- `research/researcher-260505-0011-demo-curriculum.md` (Category A lesson list)

## Overview
- **Priority:** P2
- **Status:** pending
- **Effort:** 3h
- 6 short, progressive lessons introducing uniforms, fragCoord/resolution, time, and gradient math. Each lesson teaches exactly one new concept.

## Key Insights
- Lesson order matters — each builds on the prior one's AGSL trick.
- All Category A shaders use `AgslCanvas` Brush mode (no input shader needed).
- `uv = fragCoord / resolution` is introduced in lesson 1 and reused throughout.
- Color uniforms via `setColorUniform` accept `Color.toArgb()` int — wrap in helper.

## Requirements
- 6 lessons compile and render at 60fps on API 33 emulator.
- Each lesson under 150 LOC including AGSL string.
- Slider control on lessons 2, 4, 5 (animation speed / radius / color).

## Architecture
Each lesson is a top-level `object` in `shaders/basics/` self-registering with `LessonRegistry`. Pattern:
```kotlin
object SolidColor {
    init { LessonRegistry.register(LessonModel(id="basics-01-solid", ...)) }
    private val SRC = """...AGSL..."""
}
```

## Lessons
| # | id | Title | New concept | AGSL trick |
|---|----|-------|-------------|------------|
| 1 | basics-01-solid | Solid Color | uniform color | `return color;` |
| 2 | basics-02-animated-color | Animated Color | uniform time + sin | `mix(c1, c2, 0.5+0.5*sin(time))` |
| 3 | basics-03-linear-gradient | Linear Gradient | normalized coords | `mix(a, b, uv.x)` |
| 4 | basics-04-radial-gradient | Radial Gradient | length() from center | `length(uv-0.5)` |
| 5 | basics-05-polar-coords | Polar Coordinates | atan/length | `atan(p.y, p.x)` swirl |
| 6 | basics-06-vignette | Animated Vignette | smoothstep | `smoothstep(r, r+0.05, length(uv-0.5))` |

## Related Code Files
**Create (one per lesson):**
- `shaders/basics/SolidColor.kt`
- `shaders/basics/AnimatedColor.kt`
- `shaders/basics/LinearGradient.kt`
- `shaders/basics/RadialGradient.kt`
- `shaders/basics/PolarCoords.kt`
- `shaders/basics/AnimatedVignette.kt`

**Modify:**
- `LessonRegistry.bootstrap()` — touch each new object so `init` fires.

## Implementation Steps
1. Implement lessons 1→6 in order; verify each on emulator before moving on.
2. For lessons with sliders, expose `LessonControl.FloatRange` and write values to shader uniforms in `LessonDetailScreen`.
3. Add `screenRecordingHint` to lesson 5 (polar swirl is the most visually appealing of the set).

## Todo List
- [ ] 6 lesson files registered
- [ ] All render at 60fps on emulator
- [ ] Sliders functional on lessons 2/4/5

## Success Criteria
Gallery shows 6 Basics lessons, all render correctly, sliders mutate uniforms live.

## Risk Assessment
| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Coord-space confusion (AGSL origin top-left) | Med | Low | Document in lesson conceptIntro text |

## Security Considerations
None.

## Next Steps
Phase 04 (SDF) reuses the same registration pattern.
