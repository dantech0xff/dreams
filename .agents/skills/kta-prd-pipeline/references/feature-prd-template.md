# Feature PRD Template

Use this exact structure when writing each `plans/{slug}/prd/{NN}-{feature-slug}.md`.

## Template

```markdown
---
feature: "{Feature Name}"
slug: "{feature-slug}"
bucket: "{Acquisition | Activation | Core Loop | Retention | Settings}"
screens: {N}
status: draft
---

# {Feature Name} — PRD

## Summary

{1 paragraph, 2–4 sentences. What this feature is and why it matters to the user. Avoid implementation detail.}

## User Stories

- As a {user role}, I want to {action}, so that {outcome}.
- As a {user role}, I want to {action}, so that {outcome}.

## Screens

### {ScreenName1} (PascalCase)

- **Purpose**: {1 sentence}
- **Entry from**: {screen / event / launch / deep-link}
- **Exit to**: {screen / event}
- **Primary actions**:
  - {action 1}
  - {action 2}
  - {action 3}

### {ScreenName2}

- **Purpose**: ...
- **Entry from**: ...
- **Exit to**: ...
- **Primary actions**:
  - ...

## Functional Requirements

- {requirement 1}
- {requirement 2}
- {requirement 3}

## Non-functional Requirements

- **Performance**: {e.g., main screen renders <200ms; lists smooth at 60fps with 1000 items}
- **Offline**: {full offline / read-only offline / online-only}
- **Accessibility**: WCAG AA contrast, 48dp touch targets, screen-reader labels on all actionable elements

## State Machine (optional — include only if 3+ states)

States: `Idle → Loading → Loaded | Error`

| From | Event | To |
|---|---|---|
| Idle | start | Loading |
| Loading | success | Loaded |
| Loading | failure | Error |
| Error | retry | Loading |

## Data Needs

- **Persistent**: {tables, DataStore keys, file paths — generic, not stack-specific}
- **API**: {endpoints if any}
- **Session-only**: {state held in memory only}

If none: `Local-only, no persistence beyond session.`

## Analytics Events

- `{feature}_{action}` — props: {prop1, prop2}
- `{feature}_{action}` — props: {prop1, prop2}

## Out-of-Scope (V1)

- {explicit non-goal 1}
- {explicit non-goal 2}

## Open Questions

- {question 1}
- {question 2}

(or "None.")
```

## Fill Rules

- Every section header must appear, even if empty (write "None." or "N/A" rather than removing the section)
- Screens section: at least 1 screen; for features with no UI (e.g., a background sync feature), justify in Summary
- Frontmatter `screens:` count MUST match number of screens in body
- User Stories: 1 minimum, 5 maximum
- Use real names, not "Screen1 / Screen2" — names should communicate purpose (RunDetail, OnboardingWelcome, etc.)
- No code snippets, no implementation specifics — PRD is the WHAT and WHY, not the HOW

## Pre-fill Defaults

See `phase-2-feature-prd.md` § "Pre-fill Lookup Table" for the lookup. Pre-fills go directly into the appropriate section; student picks from options or overrides after — they never write blank fields from scratch.
