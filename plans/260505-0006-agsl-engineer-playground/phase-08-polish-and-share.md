# Phase 08 — Polish, Accessibility & Share Assets

## Context Links
- `plan.md`
- All prior phases (touches surfaces from 02–07)

## Overview
- **Priority:** P3 (project shipping phase)
- **Status:** pending
- **Effort:** 2h
- Final pass: landing screen with hero shader, About card, accessibility, ProGuard, README enrichment, LinkedIn post draft.

## Key Insights
- Hero on landing = `AuroraRibbons` reused as background → instant visual identity, zero new shader work.
- Reduce-motion: respect `Settings.Global.ANIMATOR_DURATION_SCALE` — pause `time` uniform updates when scale=0 (a11y).
- ProGuard/R8 doesn't typically strip `RuntimeShader`, but verify with release build smoke test.
- LinkedIn post lives outside repo — drafted as markdown in plan dir for user to copy.

## Requirements
- Release build (`./gradlew assembleRelease`) launches and renders all categories.
- TalkBack reads gallery cards meaningfully (title + complexity + category).
- Reduce-motion setting freezes animations.
- README has hero screenshot, build instructions, AGSL primer link, demo recordings link, license.

## Architecture
```
ui/playground/landing/
  LandingScreen.kt           // hero AuroraRibbons + "Enter Playground" CTA + About link
  AboutAgslSheet.kt          // bottom sheet with canonical AGSL doc links
```
Landing becomes new nav root: `landing → gallery → lesson|showcase`.

## Related Code Files
**Create:**
- `ui/playground/landing/LandingScreen.kt`
- `ui/playground/landing/AboutAgslSheet.kt`
- `plans/260505-0006-agsl-engineer-playground/linkedin-post-draft.md`

**Modify:**
- `PlaygroundNavHost` — prepend landing route as start destination
- `MainActivity` — set system bars transparent for full-bleed landing
- `LessonCard` — add `Modifier.semantics { contentDescription = "$title, $category, complexity $complexity of 5" }`
- `AgslCanvas` — gate `time` ticker on `accessibilityManager.isAnimationsEnabled` equivalent
- `app/proguard-rules.pro` — keep rules if release smoke fails (only add if needed)
- `README.md` — full enrichment

## Implementation Steps
1. Build `LandingScreen` with `AuroraRibbons` Brush filling background, centered title "AGSL Playground", subtitle "Learn Android shaders by example", primary CTA "Open Gallery", secondary "About AGSL".
2. `AboutAgslSheet`: list canonical links (developer.android.com AGSL docs, Skia SkSL, Inigo Quilez, Book of Shaders).
3. Wire reduce-motion check into `ShaderTimeUniform` — skip `withFrameNanos` writes when disabled, hold last value.
4. Sweep gallery + showcase for `contentDescription` semantics.
5. `assembleRelease` smoke test on real device — fix any R8 strip issues with `-keep class android.graphics.RuntimeShader { *; }` if needed.
6. README: project intent, "Why AGSL", screenshots (3 from showcase), build & run, lesson list, references, license, contributor note.
7. Draft `linkedin-post-draft.md`: hook line, 3 visuals, what engineers will learn, GitHub link, hashtags (#android #jetpackcompose #agsl #graphics).

## Todo List
- [ ] `LandingScreen` with hero shader
- [ ] `AboutAgslSheet` with canonical links
- [ ] Reduce-motion respect in `ShaderTimeUniform`
- [ ] TalkBack semantics on cards
- [ ] `assembleRelease` smoke pass
- [ ] README enriched (hero + screenshots + lesson list + license)
- [ ] LinkedIn post draft saved to plan dir

## Success Criteria
- Release build installs, landing renders with hero shader, gallery navigates correctly, TalkBack works, README reads as a portfolio piece.
- LinkedIn draft is copy-paste-ready.

## Risk Assessment
| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| R8 strips `RuntimeShader` reflection paths | Low | Med | Add explicit keep rule; test release build |
| Hero shader on landing drains battery on idle | Med | Low | Pause when activity goes background (`LifecycleEventEffect`) |
| Sample image license overlooked | Low | High | Document attribution in README before publishing |

## Security Considerations
- Public repo: ensure no API keys, no proprietary assets, license file (MIT or Apache-2 — confirm with user) at repo root.

## Next Steps
- Optional v2: in-app shader editor, audio-reactive showcase, gradient mesh splash, Compose-Multiplatform port.
- Promote on LinkedIn using draft from this phase.
