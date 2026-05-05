# Phase 04 — Category B: SDF Shapes & Patterns

## Context Links
- `plan.md`
- `phase-02-lesson-framework.md`
- `research/researcher-260505-0011-demo-curriculum.md` (Category B)
- Inigo Quilez SDF reference: https://iquilezles.org/articles/distfunctions/

## Overview
- **Priority:** P2
- **Status:** pending
- **Effort:** 3h
- 6 lessons introducing signed distance fields, smooth blending, periodic patterns. Bridges Basics → Noise.

## Key Insights
- All SDF lessons return `smoothstep(0.0, edge, sdf(p))` for antialiased edges — codify as AGSL helper string injected into each lesson.
- `opSmoothUnion` (Quilez) is the metaball trick — visually striking, simple math.
- Use `mod(uv * scale, 1.0)` for tiling; introduces concept of repeating space.

## Requirements
- 6 lessons render at 60fps.
- Metaballs lesson exposes 2 sliders (blob count proxy via animated phase, smoothness factor).
- Breathing grid uses `time` uniform.

## Lessons
| # | id | Title | New concept | AGSL trick |
|---|----|-------|-------------|------------|
| 1 | sdf-01-circle | Circle SDF | distance field basics | `length(p) - r` |
| 2 | sdf-02-rounded-box | Rounded Box | box SDF | `length(max(abs(p)-b,0))-r` |
| 3 | sdf-03-metaballs | Smooth Metaballs | smoothUnion | `opSmoothUnion(d1,d2,k)` |
| 4 | sdf-04-checkerboard | Checkerboard | mod + step | `mod(floor(uv*n).x+floor(uv*n).y,2)` |
| 5 | sdf-05-breathing-grid | Breathing Grid | tiling + time | `mod(uv*n,1)-0.5` + animated radius |
| 6 | sdf-06-isolines | Isolines | fract on distance | `fract(length(p)*n)` rings |

## Related Code Files
**Create:**
- `shaders/sdf/CircleSdf.kt`
- `shaders/sdf/RoundedBoxSdf.kt`
- `shaders/sdf/Metaballs.kt`
- `shaders/sdf/Checkerboard.kt`
- `shaders/sdf/BreathingGrid.kt`
- `shaders/sdf/Isolines.kt`

**Optional shared:** `shaders/sdf/SdfHelpers.kt` — `const val SDF_HELPERS = "..."` with `opSmoothUnion`, `sdCircle`, `sdBox`, prefixed to each lesson source.

**Modify:** `LessonRegistry.bootstrap()`.

## Implementation Steps
1. Create `SdfHelpers.kt` with reusable AGSL functions (DRY).
2. Implement lessons 1→6, prefixing each AGSL string with `SDF_HELPERS`.
3. Tag metaballs as `screenRecordingHint = "Try smoothness slider 0.05 → 0.5 for goo effect"`.

## Todo List
- [ ] `SdfHelpers.kt` with reusable AGSL snippets
- [ ] 6 lesson files registered
- [ ] Metaballs sliders functional
- [ ] All render at 60fps on emulator

## Success Criteria
Gallery SDF tab shows 6 lessons, all antialiased, metaballs blend smoothly.

## Risk Assessment
| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| AGSL string concat increases compile cost | Low | Low | Helpers are small; cold-start validator catches errors once |

## Security Considerations
None.

## Next Steps
Phase 05 (Noise) — reuses helper-string pattern for noise primitives.
