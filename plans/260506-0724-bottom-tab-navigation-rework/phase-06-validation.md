# Phase 06 — Validation

## Context Links

- Reports:
  - [/plans/reports/researcher-260506-0724-navigation3-multi-stack.md](../reports/researcher-260506-0724-navigation3-multi-stack.md)
  - [/plans/reports/researcher-260506-0724-bottom-bar-ux.md](../reports/researcher-260506-0724-bottom-bar-ux.md)
- All preceding phases.

## Overview

- Priority: P1
- Status: completed-with-followup
- Brief: Manual smoke test + automated build/lint. Confirm acceptance of all behaviors specified in the plan. No new tests written (project has none currently).

## Key Insights

- The most subtle behaviors to verify are: per-tab back-stack persistence across config change AND across tab switches; bottom bar slide animation respects reduced motion; shared-element animation Lesson list → Detail still smooth.
- Process-death persistence depends on phase-01's saver implementation; if the spike fell back to config-change-only, document this gap in the project's known limitations.

## Requirements

### Functional (smoke test checklist)

#### Build & Static

- [ ] `./gradlew :app:assembleDebug` succeeds (no warnings escalated to errors).
- [ ] `./gradlew test` passes — `KoinModulesCheckTest` green; no orphaned VM tests.
- [ ] `./gradlew :app:lintDebug` produces no NEW errors compared to baseline (informational).
- [ ] `./gradlew :app:installDebug` deploys to device.

#### Cold Launch

- [ ] App launches directly into Lesson tab. No Landing splash visible at any point.
- [ ] Bottom bar visible with three tabs: Lesson (selected), Showcase, Settings.
- [ ] Tab labels visible under icons.

#### Lesson Tab — Drill-Down

- [ ] Lesson root shows exactly 4 category cards: Basics, SDF, Noise, Post-FX. NO Showcase card.
- [ ] Tap "Basics" → list of 6 lessons (matches `data/lesson/source/basics/`).
- [ ] Bottom bar still visible on list screen.
- [ ] Tap a lesson card → detail screen. Shared-element animation morphs card → header smoothly.
- [ ] Bottom bar HIDDEN on detail screen (slides down).
- [ ] System back from detail → list. Bar slides back up.
- [ ] System back from list → categories.
- [ ] System back from categories → app exits (or backgrounds, depending on launcher).

#### Showcase Tab — Drill-Down

- [ ] Tap Showcase tab → 3 cards: Liquid Glass, Aurora Ribbons, Raymarched Sphere.
- [ ] Tap a card → fullscreen showcase. Bottom bar HIDDEN. Shader renders.
- [ ] Tap on screen toggles UI (existing showcase behavior preserved).
- [ ] Back arrow returns to showcase list. Bar reappears.

#### Settings Tab — Single Page

- [ ] Tap Settings tab → fullscreen Settings page.
- [ ] Bottom bar visible (it's a tab root).
- [ ] Display section: Reduced motion toggle works; toggle state persists across tab switch and app restart.
- [ ] About section: app version visible; tap "About AGSL" opens existing bottom sheet content; tap GitHub launches browser; tap License opens inline sheet.

#### Per-Tab Back Stack

- [ ] In Lesson tab, drill to detail. Switch to Showcase tab. Switch back to Lesson tab. Detail still on top of stack — must NOT be reset to root.
- [ ] In Showcase tab, drill into showcase. Switch to Lesson and back. Showcase still on top of stack.
- [ ] Settings tab has no drill-down; switching to and from preserves nothing notable.

#### Tap-Current-Tab-To-Pop-To-Root

- [ ] In Lesson tab, drill to a list, then to a detail. Tap Lesson tab in bottom bar (already selected). Stack pops all the way back to LessonRoot (categories).
- [ ] Same test for Showcase tab: showcase → tap Showcase tab → ShowcaseRoot list.
- [ ] In Settings tab (already root): tap-current is a no-op.

#### Config Change (Rotation)

- [ ] In Lesson detail, rotate device. Same lesson still shown, sliders preserve values.
- [ ] In Lesson list scrolled mid-way, rotate. Scroll position preserved.
- [ ] In Showcase tab during showcase, rotate. Same showcase resumes (shader may flicker — acceptable).
- [ ] Selected tab persists across rotation.

#### Process Death (best-effort)

- [ ] In Lesson detail, send app to background, run `adb shell am kill com.dantech.dreams`, reopen. EXPECT: same lesson detail OR (if saver was config-only fallback) Lesson root. Document actual behavior.

#### Reduced Motion

- [ ] In Settings, enable Reduced motion.
- [ ] Drill-down transitions become snap (no fade duration).
- [ ] Bottom bar hide/show becomes snap (no slide).
- [ ] Shared-element morph still functional but instant.

#### Edge-to-Edge / Insets

- [ ] On Showcase fullscreen, back arrow visible (not under status bar). If clipped, file follow-up.
- [ ] Bottom bar respects navigation gesture insets (Material3 default).

#### LessonCard Reuse

- [ ] In Lesson list, cards visually identical to old Gallery cards.
- [ ] Favorite heart toggles persist (DataStore).
- [ ] Card complexity stars (★/☆) render correctly.

### Non-Functional

- [ ] Cold-start time anecdotally similar to or better than pre-rework (Landing was an extra screen with its own shader cost).
- [ ] No logcat warnings about shared-transition scope mismatches.
- [ ] No memory leak on repeated tab switching (LeakCanary not in scope; visual smoke only).

## Implementation Steps

1. **Build check** (automated):
   ```bash
   cd /Users/dan/Desktop/Development/Android-Kotlin/dreams
   ./gradlew :app:assembleDebug
   ./gradlew test
   ./gradlew :app:lintDebug
   ./gradlew :app:installDebug
   ```

2. **Manual smoke** on a physical Android 13+ device or emulator, working through the checklist above section by section. Tick boxes as completed.

3. **Logcat watch** during smoke test:
   ```bash
   adb logcat -s "Compose:E" "Compose:W" "Choreographer:W" "AndroidRuntime:E"
   ```
   Note any unexpected warnings/errors.

4. **Document gaps** in this file's "Findings" section below at end of phase. Specifically:
   - Which behaviors PASSED.
   - Which FAILED — with steps to reproduce.
   - Process-death restoration depth (full / config-change-only / unsupported).
   - Any small UX issues to file as follow-up tickets.

5. **If any FAIL is critical (crash, missing feature)** — rollback to phase-04 commit and re-engage planner. Otherwise, file as follow-ups and ship.

## Todo List

- [ ] `assembleDebug` passes
- [ ] `test` passes
- [ ] `lintDebug` no new errors
- [ ] `installDebug` deploys
- [ ] Cold launch checklist complete
- [ ] Lesson tab drill-down checklist complete
- [ ] Showcase tab drill-down checklist complete
- [ ] Settings tab checklist complete
- [ ] Per-tab back stack preservation verified
- [ ] Tap-current-tab-pop-to-root verified
- [ ] Rotation persistence verified
- [ ] Process death persistence verified or limitation documented
- [ ] Reduced motion verified
- [ ] Edge-to-edge insets verified
- [ ] LessonCard reuse verified
- [ ] Findings section filled in below

## Success Criteria

- All Build & Static checklist items pass.
- All Functional checklist items pass OR documented as known limitations with severity.
- Zero new logcat ERRORs introduced by the rework.

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Process-death restoration partially broken (TopLevelBackStack saver) | Medium | Low (known limitation) | Acceptable for MVP; document in plan + project changelog. |
| Shared-element animation regresses subtly under single-NavDisplay structure | Low | Medium | Visual A/B against pre-rework recording (no recording exists; rely on memory/screenshots). If regressed, file follow-up; revert is heavy. |
| `KoinModulesCheckTest` fails because phase-05 missed a test reference | Low | Low | `./gradlew test` catches; fix in implementation. |
| Edge-to-edge ShowcaseScreen back arrow obscured | Medium | Low | One-line fix to add `Modifier.statusBarsPadding()` to that Box. File as follow-up if observed. |
| Bottom bar slide animation jitters on first transition | Low | Low | Tune `AnimatedVisibility` enter/exit specs (e.g. add `tween(durationMs)`). |

## Security Considerations

n/a — validation phase, no code change unless rollback triggered.

## Next Steps

- If all green: update `docs/system-architecture.md` Navigation section to reflect new shell + remove Landing/Gallery references. Update `docs/codebase-summary.md` package tree. Update `docs/project-changelog.md` with feature entry.
- If gaps: file follow-up tickets for non-blocking items.

## Findings (fill in during execution)

> Replace this block during phase execution.

- Build: ☐ pass / ☐ fail — notes:
- Smoke (Cold launch): ☐ pass / ☐ fail — notes:
- Smoke (Lesson drill-down): ☐ pass / ☐ fail — notes:
- Smoke (Showcase drill-down): ☐ pass / ☐ fail — notes:
- Smoke (Settings): ☐ pass / ☐ fail — notes:
- Per-tab back stack: ☐ pass / ☐ fail — notes:
- Tap-current pop-to-root: ☐ pass / ☐ fail — notes:
- Rotation: ☐ pass / ☐ fail — notes:
- Process death: ☐ pass / ☐ partial / ☐ fail — notes:
- Reduced motion: ☐ pass / ☐ fail — notes:
- Edge-to-edge: ☐ pass / ☐ fail — notes:

## Unresolved Questions

- Process-death persistence depth depends on phase-01 saver outcome. To be resolved during phase-01 spike and confirmed here.
