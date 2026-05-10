# Phase 2 — Per-Screen Spec (loop)

Read all PRDs from `plans/{slug}/prd/{NN}-*.md`. For each screen across all features, emit one spec at `plans/{slug}/design/{NN-feature-slug}/{NN-screen-slug}.md`.

**ABSOLUTE RULES:**
- **NO Kotlin / Compose / `@Preview` / DSL snippets in any output**
- **Component-states list MANDATORY — refuse to write incomplete spec**
- **Real copy only — no lorem ipsum, no `{placeholder}` text**

## Pre-flight

1. Verify `plans/{slug}/design/style-foundation.md` exists. If missing → STOP:

```
[kta-design-spec] Phase 2 cannot run:
- Missing: plans/{slug}/design/style-foundation.md
- Run Phase 1 first.
```

2. Read all `plans/{slug}/prd/{NN}-*.md`. Validate each PRD has `## Screens` section non-empty.

3. Build a flat list:

```yaml
screens:
  - feature_slug: onboarding
    feature_index: 1
    screen_name: Welcome
    screen_index: 1
    purpose: First impression, value prop, CTA to continue
    entry_from: app launch (first run only)
    exit_to: PermissionPrompt
    primary_actions: [Continue, Skip onboarding]
  - feature_slug: onboarding
    feature_index: 1
    screen_name: PermissionPrompt
    screen_index: 2
    ...
```

## Loop

For each screen, emit `plans/{slug}/design/{NN_feature}-{feature_slug}/{NN_screen}-{screen-slug-kebab}.md` (zero-padded indices).

Use `references/screen-spec-template.md` as exact template.

### Required Sections (9 — all mandatory)

1. **Frontmatter** — feature, screen, purpose, status: draft
2. **Purpose** (1–2 sentences)
3. **Entry / Exit** (from PRD)
4. **Layout** — tree describing header / body / footer / floating
5. **Components (with states)** — MANDATORY EVERY ITEM HAS 6 STATES
6. **Copy** — every visible string written in full
7. **Tokens used** — reference style-foundation.md token names
8. **Accessibility** — contrast, touch targets, screen reader labels
9. **Motion** — which transitions need animation; intent only, no durations
10. **Reference** — "looks like {app} but with {wedge}"
11. **AI-tool render block** — self-contained paragraph paste-ready into any text-to-UI tool

(Total 11 sections — frontmatter + 10 body sections.)

### Component States Format (inline)

Each component listed once with states inline (NOT 6 separate sections per component):

```markdown
- **PrimaryButton** ("Continue") — states:
  - default: `accent` background, `on-accent` text, radius `md`, elevation 0
  - pressed: 95% scale, shadow lift
  - disabled: 38% opacity, no shadow, no press feedback
  - loading: spinner replaces label, disabled press
  - empty: N/A (button not contextual)
  - error: red outline, error message below
- **SkipLink** ("Skip onboarding") — states:
  - default: `on-surface-variant` text, underlined
  - pressed: 80% opacity
  - disabled: hidden (no skip allowed)
  - loading: N/A
  - empty: N/A
  - error: N/A
```

States that don't apply → write `N/A` explicitly. Don't omit. Skill validation counts 6 states per component; fewer = refuse.

### Copy Section

```markdown
## Copy

- **Headline**: "Track your runs, unlock your best."
- **Subheadline**: "Built for marathon runners chasing sub-3:30."
- **Primary CTA**: "Continue"
- **Skip link**: "Skip onboarding"
- **Empty state** (if applicable): N/A — first-run screen, no empty state
- **Error message** (if applicable): N/A
```

EVERY copy field is real text. NO `{placeholder}`, NO lorem ipsum.

Beginner mode: offer 3 copy options per visible string, student picks. Intermediate: write blank, student fills.

### AI-Tool Render Block

Self-contained paragraph that any text-to-UI tool can consume cold. Includes:
- Screen identity (1 sentence)
- Layout summary (1 sentence)
- Key components (list)
- Style tokens to use (from style-foundation.md)
- Tone / motion hint (1 phrase)

Example:

```markdown
## AI-Tool Render Block

Welcome screen for a running tracker app aimed at marathon runners. Layout: full-bleed background image (subtle running shoe motif), centered content stack with headline + subheadline, primary CTA pinned 32dp from bottom, small skip link below. Components: large headline (Display M, on-surface), supporting subheadline (Body L, on-surface-variant), one filled button (PrimaryButton, accent color, full-width minus 32dp horizontal padding), text link below ("Skip onboarding"). Use Material 3 baseline tokens overridden by warm earth palette from style-foundation.md. Tone: calm, encouraging, not pushy. Motion: button presses use springy feedback; screen enters with 250ms fade.
```

This block alone — pasted into Stitch / Figma AI / Claude — should yield a recognizable mockup.

## Validation Before Write

For EACH screen spec, before writing file:

```
checks:
  - frontmatter complete (feature, screen, purpose, status)
  - all 11 sections present (don't skip even if empty — write "N/A")
  - components list non-empty
  - EVERY component has all 6 states listed (default, pressed, disabled, loading, empty, error)
  - copy fields contain real strings (no lorem, no {placeholder}, no $variable)
  - tokens reference style-foundation.md
  - AI-tool render block is 4–8 sentences, self-contained
```

Any check fails → STOP, print:

```
[kta-design-spec] Cannot write {feature}/{screen}.md:
- Missing/invalid: {field}
- Fix and retry.
```

DO NOT silently emit incomplete spec.

## After Loop — Write INDEX

After all per-screen specs written, generate `plans/{slug}/design/INDEX.md`:

```markdown
---
title: "{App Name} — Design Spec Index"
generated: {YYYY-MM-DD}
features: {N}
screens: {M}
---

# Design Spec Index — {App Name}

Style foundation: [`./style-foundation.md`](./style-foundation.md)

## Features → Screens

### 1. Onboarding
- [01-welcome.md](./01-onboarding/01-welcome.md) — First impression, value prop, CTA
- [02-permission-prompt.md](./01-onboarding/02-permission-prompt.md) — Request runtime permissions
- [03-first-action.md](./01-onboarding/03-first-action.md) — Funnel into core loop

### 2. RunLogging
- [01-run-list.md](./02-run-logging/01-run-list.md) — All runs, sorted by date
- [02-run-create.md](./02-run-logging/02-run-create.md) — Log a new run
- [03-run-detail.md](./02-run-logging/03-run-detail.md) — Single run view + edit

(...)

## Next Steps

→ Paste any per-screen spec into Stitch / Figma AI / Claude artifacts to generate mockups.
→ Or run Phase 3 (tool-specific render) for tool-tuned reformatting.
```

## Print Final Summary

```
[kta-design-spec] Phase 2 complete:
✓ {M} screen specs written across {N} features
✓ design/INDEX.md written

Validation passed:
✓ All component-state lists complete (6 states × {C} components)
✓ All copy strings real (no lorem ipsum)
✓ All tokens reference style-foundation.md
✓ Zero Kotlin / Compose / @Preview emitted

Next: Paste any spec into your AI design tool. Optional: Phase 3 for tool-specific render.
```
