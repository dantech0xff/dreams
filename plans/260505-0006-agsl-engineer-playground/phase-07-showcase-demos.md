# Phase 07 — Showcase Demos (LinkedIn-Worthy)

## Context Links
- `plan.md`
- `phase-02-lesson-framework.md` (`AgslCanvas`)
- `phase-04-category-b-sdf.md` (sphere SDF reused for raymarcher)
- `phase-05-category-c-noise.md` (fbm reused for aurora + glass)
- `phase-06-category-d-posteffect.md` (displacement glass technique)
- `research/researcher-260505-0011-demo-curriculum.md` (Showcase S1–S5)
- Reference impl: https://github.com/Mortd3kay/liquid-glass-compose

## Overview
- **Priority:** P2 (project payoff phase)
- **Status:** pending
- **Effort:** 5h
- 3 full-screen "wow" experiences for screen-recording. Each gets dedicated nav route, hero treatment, and a Record-banner explaining the LinkedIn angle. Deferring 2 of the 5 researched showcases (audio-reactive, gradient mesh) to backlog.

## Key Insights
- Showcase ≠ lesson — full-screen, no source viewer by default (toggle to reveal). UX optimized for recording.
- Each showcase ≤300 LOC AGSL because complexity = compile + perf cost.
- Liquid glass reuses RenderEffect from phase 06 displacement; aurora reuses Brush mode + fbm; raymarched sphere is pure Brush mode, single SDF + lambert lighting.
- Add subtle on-screen "Tap to hide UI" hint that fades after 3s — clean recordings.
- Showcase screens registered as a separate `LessonCategory.SHOWCASE` tab in gallery.

## Showcases (3 of 5 from research)
| id | Title | Why LinkedIn-worthy | Reuses |
|----|-------|---------------------|--------|
| showcase-01-liquid-glass | Liquid Glass Overlay | iOS 26-style refractive panel over photo, drag to move | phase 06 displacement + Coil image |
| showcase-02-aurora-ribbons | Aurora Borealis | Flowing colored ribbons over dark sky, looks expensive | phase 05 fbm + polar coords |
| showcase-03-raymarched-sphere | Raymarched Sphere | "I rendered 3D in a fragment shader" wow moment | phase 04 sphere SDF + new lambert lighting |

## Architecture
```
shaders/showcase/
  LiquidGlass.kt
  AuroraRibbons.kt
  RaymarchedSphere.kt
ui/playground/showcase/
  ShowcaseScreen.kt          // shared full-screen scaffold w/ Record banner + UI auto-hide
  RecordHintBanner.kt
```

`ShowcaseScreen` takes an `id` and a `@Composable () -> Unit` body — Record banner overlays, fades after 3s, returns on tap.

## Related Code Files
**Create:**
- `shaders/showcase/LiquidGlass.kt`
- `shaders/showcase/AuroraRibbons.kt`
- `shaders/showcase/RaymarchedSphere.kt`
- `ui/playground/showcase/ShowcaseScreen.kt`
- `ui/playground/showcase/RecordHintBanner.kt`

**Modify:**
- `LessonCategory` — add `SHOWCASE` enum value
- `PlaygroundNavHost` — add `composable("showcase/{id}")` route
- `LessonRegistry.bootstrap()` — register 3 showcase entries
- `GalleryScreen` — render SHOWCASE tab differently (full-bleed cards with looping preview thumbnail)

## Implementation Steps
1. Build `ShowcaseScreen` scaffold + auto-hiding Record banner.
2. **showcase-03 first (lowest risk)**: raymarched sphere — fixed camera, 1 sphere, lambert + ambient. Animate light position with `time`.
3. **showcase-02 aurora**: vertical fbm-warped ribbons, palette = blue→green→magenta. Animate via `time` shifting noise sample y-axis.
4. **showcase-01 liquid glass** (highest risk): Coil-loaded background photo, draggable rounded-rect panel, panel uses RenderEffect that samples background + fbm-displaces UV + adds rim lighting. Drag updates uniform `panelCenter`.
5. Real-device smoke test — must hit 60fps on a Pixel 6 / Galaxy S22-class device.
6. Capture 30s screen recording of each → save to `plans/260505-0006-agsl-engineer-playground/showcase-recordings/` (gitignored or LFS — confirm with user before adding to repo).

## Todo List
- [ ] `ShowcaseScreen` scaffold + Record banner
- [ ] `RaymarchedSphere` rendering
- [ ] `AuroraRibbons` rendering
- [ ] `LiquidGlass` with draggable panel + image background
- [ ] Real-device 60fps verified for all 3
- [ ] 30s recordings captured (storage location TBD with user)

## Success Criteria
3 showcases each render full-screen at 60fps on a 2022+ flagship; each produces a clean 30s loopable recording; LinkedIn post draft (phase 08) embeds these.

## Risk Assessment
| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Liquid glass perf <60fps | Med | High | Reduce displacement octaves; cap panel size; profile with GPU inspector |
| Raymarcher loop bound issues | Low | High | Hard-code 64 max steps as `int` constant |
| Driver-specific shader bugs (Mali/Adreno divergence) | Med | Med | Test on at least one Mali + one Adreno device before declaring done |
| Recording storage in repo bloats clone size | Med | Low | Default to gitignore + cloud upload; confirm with user |

## Security Considerations
- If liquid glass uses a sample photo, must be CC0.
- No new permissions.

## Next Steps
Phase 08 — polish, README, LinkedIn post draft.
