# Phase 1 — Style Foundation

Run once per app, before any per-screen spec. Output: `plans/{slug}/design/style-foundation.md`. **No Kotlin, no Compose, no code.**

## Why this gates Phase 2

Per-screen specs reference tokens from this file. Without style-foundation, every screen would re-define colors / type / spacing — breaks consistency, breaks AI design tool output. Phase 2 REFUSES to run if `style-foundation.md` missing.

## Schema (9 sections, all required)

```markdown
---
title: "{App Name} — Style Foundation"
generated: {YYYY-MM-DD}
level: "{beginner | intermediate}"
---

# {App Name} — Style Foundation

## 1. Mood / Voice

3 adjectives + 1 anti-pattern.
> Calm, focused, encouraging — NOT gamified.

## 2. Color Tokens

Either pick a palette OR write "use Material 3 default".

| Token | Light | Dark | Notes |
|---|---|---|---|
| primary | #2E7D5F | #5FBE91 | brand accent |
| on-primary | #FFFFFF | #00200F | text on primary |
| secondary | #6B5C4E | #D4C2B0 | supporting |
| on-secondary | #FFFFFF | #2A1F12 | text on secondary |
| surface | #FAF7F2 | #1A1611 | background |
| on-surface | #1A1611 | #EDE6DA | body text |
| accent | #E07856 | #F0A488 | CTA highlight |
| error | #B3261E | #F2B8B5 | validation, destructive |

OR:

```
Use Material 3 baseline palette (default). No custom tokens this version.
```

## 3. Typography

Either pick families + scale OR "use Material 3 default scale".

- **Display family**: Inter / Roboto / system default
- **Body family**: Inter / Roboto / system default

| Style | Size | Weight | Line height | Use |
|---|---|---|---|---|
| Display L | 57sp | 400 | 64 | hero headlines |
| Headline M | 28sp | 500 | 36 | screen titles |
| Title M | 16sp | 500 | 24 | card titles |
| Body L | 16sp | 400 | 24 | reading text |
| Body M | 14sp | 400 | 20 | secondary text |
| Label L | 14sp | 500 | 20 | button text |

## 4. Spacing (4dp grid)

| Token | Value | Use |
|---|---|---|
| XS | 4dp | tight inline |
| S | 8dp | between related items |
| M | 16dp | default gutter |
| L | 24dp | section spacing |
| XL | 32dp | screen padding (top/bottom) |

## 5. Radii

| Token | Value |
|---|---|
| sm | 4dp |
| md | 12dp |
| lg | 24dp |

## 6. Elevation

| Level | dp | Use |
|---|---|---|
| 0 | 0 | flat surface |
| 1 | 1 | resting card |
| 3 | 3 | raised card / FAB |
| 6 | 6 | menu / dialog |
| 12 | 12 | modal |

## 7. References

3 inspiration apps + 1 anti-reference.

| Reference | Take | Skip |
|---|---|---|
| Strava | clean run cards, achievement timing | gamified leaderboards |
| Things 3 | minimalism, single-tap actions | desktop-first density |
| Forest | warm color, focused single-purpose | streak punishment |
| **ANTI**: MyFitnessPal | — | over-featured, ad-clutter, dark patterns |

## 8. Motion Intent

Pick one: **springy** | **precise** | **minimal**.

> **Springy** — interactive feedback feels physical. Mass / damping defaults appropriate for a calm tracker. Use for list-item taps, button press, screen enters/exits. Avoid spring for content reveals (use fade).

## 9. Accessibility Floor

- WCAG AA contrast — body text 4.5:1, large text 3:1
- Minimum touch target — 48dp × 48dp
- Screen reader labels — every actionable element
- Dynamic type — respect system font size up to 200%
- Color is never the sole information channel

## Notes

- Tokens above are starting defaults — student can adjust per app
- Use Material 3 baseline if no opinion (saves time, accepted by all AI design tools)
```

## Process

1. Ask student about mood/voice (3 adjectives + 1 anti-pattern). Beginner mode offers 3 mood templates: "calm/focused", "playful/bold", "professional/restrained".
2. Ask about color preference. Default to Material 3 baseline. Beginner mode skips custom palette unless student insists.
3. Typography — default to Material 3 scale unless student has brand fonts.
4. Spacing/radii/elevation — defaults above almost always fine; ask once, accept defaults.
5. References — REQUIRE 3 + 1 anti. This is the non-negotiable creative input.
6. Motion intent — single choice; default `springy` for consumer apps.
7. A11y — auto-fill defaults; student opts in to stricter targets.

## Validation Before Write

- All 9 sections present
- Mood section has 3 adjectives + 1 anti-pattern (not just adjectives)
- References section has 3 inspirations + 1 explicit anti
- Motion intent is one of the 3 choices

If validation fails → ask student per missing field, retry.

## Output

Write `plans/{slug}/design/style-foundation.md`. Print:

```
[kta-design-spec] Style foundation written.
✓ plans/{slug}/design/style-foundation.md ({N} sections)
Next: Phase 2 — Per-Screen Spec.
```
