# Screen Spec Template

Use this exact structure when writing each `plans/{slug}/design/{NN-feature-slug}/{NN-screen-slug}.md`.

**REMINDER**: NO Kotlin, NO Compose, NO `@Preview`. Markdown spec only.

## Template

```markdown
---
feature: "{Feature Name}"
screen: "{ScreenName}"
purpose: "{1-line summary}"
status: draft
level: "{beginner | intermediate}"
---

# {ScreenName} — Design Spec

## 1. Purpose [required]
{1–2 sentences. The user goal this screen serves and where it sits in the journey.}

## 2. Entry / Exit [required]
- **Entry from**: {screen name | event | deep-link | launch}
- **Exit to**: {screen name | event}

## 3. Layout [required]

Tree describing visual zones (header / body / footer / floating).

```
Frame (360 × 800 portrait)
├── StatusBar zone (system, 24dp)
├── Header (64dp)
│   ├── BackArrow (left, optional)
│   └── Title ("{Title text}")
├── Body (scrollable, 16dp horizontal padding)
│   ├── HeroBlock (full-width, 240dp tall)
│   │   ├── Image (full-bleed, top)
│   │   └── HeadlineStack (centered, padded 32dp)
│   ├── Card (radius md, elevation 1) × N
│   └── ...
└── Footer (88dp, pinned)
    ├── PrimaryButton (full-width minus 32dp)
    └── SkipLink (centered, 16dp below button)
```

## 4. Components (with states) [required]

Every component MUST list all 6 states. Use `N/A` for inapplicable states; do not omit.

- **PrimaryButton** ("{button copy}") — states:
  - default: {visual description}
  - pressed: {visual description}
  - disabled: {visual description}
  - loading: {visual description}
  - empty: {visual description or N/A}
  - error: {visual description or N/A}

- **{ComponentName}** ("{copy}") — states:
  - default: ...
  - pressed: ...
  - disabled: ...
  - loading: ...
  - empty: ...
  - error: ...

(Repeat for every component on the screen. Lists, cards, text fields, buttons, dialogs all qualify.)

## 5. Copy [required]

Every visible string. Real text. No lorem ipsum. No `{placeholder}`.

- **Headline**: "..."
- **Subheadline**: "..."
- **Primary CTA**: "..."
- **Secondary CTA**: "..." (or N/A)
- **Empty state**: "..." (or N/A)
- **Error message**: "..." (or N/A)
- **Loading message** (if visible): "..." (or N/A)
- **Helper / hint text**: "..." (or N/A)

## 6. Tokens used [required]

Reference style-foundation.md token names. Do not redefine.

- Colors: `primary`, `on-primary`, `accent`, `surface`, `on-surface-variant`
- Type: `Display M` (headline), `Body L` (subheadline), `Label L` (button)
- Spacing: `M` (gutter), `XL` (top/bottom screen padding)
- Radii: `md` (card, button)
- Elevation: `1` (resting card), `0` (button)

## 7. Accessibility [required]

- **Contrast**: all text meets WCAG AA (4.5:1 body, 3:1 large)
- **Touch targets**: all interactive elements ≥ 48dp × 48dp
- **Screen reader labels**:
  - PrimaryButton: announces "Continue, button"
  - SkipLink: announces "Skip onboarding, link"
  - {Component}: ...
- **Dynamic type**: layout reflows at 200% font scale (no clipped text)
- **Focus order**: top-to-bottom, left-to-right, primary CTA last

## 8. Motion [required]

Intent only, no durations. Defer specific timings to implementation.

- Screen enter: {fade | slide | crossfade | none}
- Screen exit: {fade | slide | crossfade | none}
- PrimaryButton press: springy feedback (scale + shadow)
- {Other element}: {motion intent}

## 9. Reference [required]

"Looks like {app} but with {wedge}." 1 sentence.

> Looks like Strava's first-run, but with calmer color palette and no gamification.

## 10. AI-tool render block [required]

4–8 sentences, self-contained, paste-ready into Stitch / Figma AI / Claude artifacts. Combines screen identity + layout + components + tokens + tone + motion.

```
{Self-contained paragraph here.}
```

## 11. Open Questions [optional]

- {question 1}
- {question 2}

(or "None.")
```

## Fill Rules

- All 11 sections required (10 body + frontmatter)
- Use `N/A` instead of omitting an inapplicable state or copy field — the structure must remain consistent
- Component states list 6 items each — skill validates count
- Copy strings are FINAL strings — no `{placeholder}`, no `$variable`, no lorem ipsum
- Tokens reference style-foundation.md by name; do not duplicate values
- AI-tool render block must stand alone (paste it into a fresh tool with no other context — should yield recognizable output)

## Beginner Mode Defaults

When `level: beginner`:
- Pre-fill component states with sensible defaults from a base library (e.g., Button states use M3 patterns)
- Offer 3 copy options per visible string, student picks
- Default screen-enter motion = `crossfade` (safe choice)
- Default reference = "Material 3 baseline patterns, polished"
- Student can override anywhere

## Intermediate Mode

Emit blank template. Student fills.

## Anti-Examples (do NOT do)

- ❌ `@Composable fun WelcomeScreen() { ... }` — NO Compose code
- ❌ `Button(onClick = {}) { Text("Continue") }` — NO DSL
- ❌ `// TODO: implement state` — NO TODOs in spec
- ❌ `Headline: "{title}"` — placeholder; write real copy
- ❌ `Lorem ipsum dolor sit amet...` — never
- ❌ Missing pressed state on a button — refuse to emit
