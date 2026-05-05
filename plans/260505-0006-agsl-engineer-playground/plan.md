---
title: "AGSL Engineer Playground"
description: "Curated AGSL lesson gallery + LinkedIn-shareable showcase demos in Compose"
status: pending
priority: P2
effort: 28h
branch: master
tags: [android, agsl, compose, education]
created: 2026-05-05
---

# AGSL Engineer Playground — Implementation Plan

Android app teaching AGSL via curated, swipeable Compose lessons (4 categories, ~24 lessons) plus 3 wow-factor showcase screens designed for screen-recording and LinkedIn sharing.

## Stack & Constraints
- Kotlin / Jetpack Compose / Material3 / Compose BOM `2026.02.01`
- `minSdk` 33 (AGSL `RuntimeShader`), `targetSdk` 36, JVM 11
- Package: `com.dantech.dreams`
- Principles: YAGNI / KISS / DRY — no editor, no DI, no persistence, no analytics in v1
- Lesson files ≤150 LOC; framework files ≤200 LOC

## Research
- `reports/researcher-260505-0001-agsl-compose-integration.md` — AGSL APIs + 3 Compose paths
- `research/researcher-260505-0011-demo-curriculum.md` — 24-lesson curriculum + 5 showcase combos

## Phases

| # | File | Title | Effort | Status |
|---|------|-------|--------|--------|
| 01 | [phase-01-project-setup.md](phase-01-project-setup.md) | Project setup & deps | 2h | pending |
| 02 | [phase-02-lesson-framework.md](phase-02-lesson-framework.md) | Lesson framework & navigation | 5h | pending |
| 03 | [phase-03-category-a-basics.md](phase-03-category-a-basics.md) | Category A: Basics (6 lessons) | 3h | pending |
| 04 | [phase-04-category-b-sdf.md](phase-04-category-b-sdf.md) | Category B: SDF shapes (6 lessons) | 3h | pending |
| 05 | [phase-05-category-c-noise.md](phase-05-category-c-noise.md) | Category C: Noise & procedural (6 lessons) | 4h | pending |
| 06 | [phase-06-category-d-posteffect.md](phase-06-category-d-posteffect.md) | Category D: RenderEffect post-FX (6 lessons) | 4h | pending |
| 07 | [phase-07-showcase-demos.md](phase-07-showcase-demos.md) | Showcase: liquid glass / aurora / raymarched sphere | 5h | pending |
| 08 | [phase-08-polish-and-share.md](phase-08-polish-and-share.md) | Polish, accessibility, README, share assets | 2h | pending |

## Key Dependencies
- 02 blocks 03–07 (framework first)
- 03–06 independent of each other once 02 ships (parallel-safe)
- 07 depends on 02 (uses `AgslCanvas`); 06 must ship first if any showcase reuses RenderEffect helpers
- 08 last (touches polish across all phases)

## Cross-Cutting Risks
- API 33 driver bugs on some chipsets → real-device smoke required before phase 07
- `RenderEffect` instances must be `remember{}`'d to avoid per-recomposition allocation
- AGSL compile errors are runtime → registry cold-start validator (phase 02) prevents shipping broken shaders

## Definition of Done (project)
App installs on API 33+ device, gallery lists 24 lessons across 4 tabs, every lesson renders without crash, 3 showcase screens record cleanly at 60fps on a mid-range device.

## Validation Summary

**Validated:** 2026-05-05
**Questions asked:** 4

### Confirmed Decisions
- **License:** Apache-2.0 — add `LICENSE` file in phase 08, header note in README
- **Showcase recordings:** Cloud-host (YouTube unlisted / Imgur) + link from README — keeps repo small, ideal for LinkedIn share workflow
- **Perf test devices:** Pixel 6+/7/8 (Adreno/Tensor) + Galaxy S22+ (Mali/Exynos) + mid-range A-series + emulator — broad GPU coverage; phase 07 60fps target verified across vendor stacks
- **Lesson smoke test:** Yes, JVM-only `LessonRegistry` integrity test — non-empty, unique ids, all sources non-blank

### Action Items (apply during implementation, not now)
- [ ] Phase 02: add `app/src/test/.../LessonRegistryTest.kt` (JUnit) asserting registry non-empty + unique ids + non-blank sources
- [ ] Phase 07: update Implementation Step 6 — recordings go to cloud host (default YouTube unlisted), README embeds GIF previews + links; remove "showcase-recordings/" repo subdir reference
- [ ] Phase 07: real-device test matrix = Pixel 6+/7/8, Galaxy S22+, mid-range A-series, emulator (correctness-only)
- [ ] Phase 08: add Apache-2.0 `LICENSE` file at repo root + license badge in README; update LinkedIn post draft with Apache-2.0 mention
- [ ] Phase 08: drop "(MIT or Apache-2 — confirm with user)" from Security Considerations — decision is locked
