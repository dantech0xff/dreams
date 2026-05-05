# Phase 05 — Category C: Noise & Procedural Textures

## Context Links
- `plan.md`
- `phase-02-lesson-framework.md`
- `research/researcher-260505-0011-demo-curriculum.md` (Category C)
- The Book of Shaders — noise: https://thebookofshaders.com/11/
- Inigo Quilez — domain warping: https://iquilezles.org/articles/warp/

## Overview
- **Priority:** P2
- **Status:** pending
- **Effort:** 4h
- 6 lessons covering hash → value noise → fbm → voronoi → plasma → domain-warped lava. The visually richest of the four basic categories.

## Key Insights
- Build a `NoiseHelpers.kt` AGSL snippet bundle (`hash21`, `valueNoise`, `fbm`) reused across lessons 2–6 (DRY).
- Voronoi animated seeds ("cellular life") is the most LinkedIn-worthy lesson here — flag with `screenRecordingHint`.
- fbm is intentionally octave-bounded (loops in AGSL must be statically unrollable).
- Plasma is cheap classic — 2× sine sums → great gradient demo.
- Domain warping (`fbm(p + fbm(p + fbm(p)))`) is the "lava" payoff lesson — bridges to Showcase phase.

## Requirements
- 6 lessons render at ≥45fps on a mid-range device (noise is heavier than SDF).
- fbm lesson exposes octave count (1–6) slider.
- Voronoi exposes cell count (4–32) slider.

## Lessons
| # | id | Title | New concept | AGSL trick |
|---|----|-------|-------------|------------|
| 1 | noise-01-hash | Pseudo-random Hash | hash21 | `fract(sin(dot(p,vec2(12.9898,78.233)))*43758.5453)` |
| 2 | noise-02-value | Value Noise | bilinear interp | `mix(mix(a,b,f.x), mix(c,d,f.x), f.y)` |
| 3 | noise-03-fbm | fBM Clouds | octave loop | `for (int i=0;i<6;i++){ a+=amp*noise(p); p*=2; amp*=.5; }` |
| 4 | noise-04-voronoi | Voronoi Cells | nearest-point search | `min(d, length(p - cell))` |
| 5 | noise-05-plasma | Plasma | sin sums | `sin(uv.x*10+t)+sin(uv.y*10+t)+sin(length(uv)*10)` |
| 6 | noise-06-warped-lava | Warped Lava | domain warping | `fbm(p + fbm(p + fbm(p)))` colored as fire |

## Related Code Files
**Create:**
- `shaders/noise/NoiseHelpers.kt` (`const val NOISE_HELPERS = "..."` with hash21, valueNoise, fbm)
- `shaders/noise/HashLesson.kt`
- `shaders/noise/ValueNoise.kt`
- `shaders/noise/FbmClouds.kt`
- `shaders/noise/VoronoiCells.kt`
- `shaders/noise/Plasma.kt`
- `shaders/noise/WarpedLava.kt`

**Modify:** `LessonRegistry.bootstrap()`.

## Implementation Steps
1. Write `NoiseHelpers.kt` first; manually verify each helper compiles via cold-start validator.
2. Implement lessons 1→6 in order.
3. Bench fbm lesson on mid-range device — if <45fps, reduce default octaves to 4 and document why in conceptIntro.
4. Tag voronoi + warped-lava with `screenRecordingHint`.

## Todo List
- [ ] `NoiseHelpers.kt` with hash/value/fbm
- [ ] 6 lesson files registered
- [ ] fbm + voronoi sliders functional
- [ ] Mid-range device hits ≥45fps on warped-lava

## Success Criteria
Noise tab shows 6 lessons, warped-lava looks like the cover image of a graphics textbook.

## Risk Assessment
| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Loop bound > unrollable | Med | High (compile error) | Cap octaves at 6, use literal `int` constant |
| `sin` precision artifacts on Mali GPUs | Low | Med | Use `float` not `half` for noise math |
| Voronoi double loop stalls low-end GPU | Med | Med | Cap cell count at 32 in slider |

## Security Considerations
None.

## Next Steps
Phase 06 introduces RenderEffect (Category D) — first phase that takes Composable content as input.
