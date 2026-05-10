# Phase 2 — Per-Feature PRD

Loop through the approved feature list from Phase 1. For EACH feature, emit one PRD file at `plans/{slug}/prd/{NN}-{feature-slug}.md` (NN = ordinal, zero-padded).

## Loop

```
for index, feature in approved_features (1-indexed):
    NN = zero-pad(index, width=2)
    file = plans/{slug}/prd/{NN}-{feature.slug}.md
    fill template (see feature-prd-template.md) using:
        - feature info from Phase 1
        - sensible defaults pre-filled per the lookup table below
    write file
```

## Required Schema (every PRD must contain)

1. **Frontmatter** — feature, slug, bucket, screens count, status (draft)
2. **Summary** — 1 paragraph (2–4 sentences) — what it is + why it matters
3. **User Stories** — minimum 1, max 5. Format: `As {user}, I want {action}, so that {outcome}.`
4. **Screens** — list, each with:
   - Name (PascalCase)
   - Purpose (1 sentence)
   - Entry points (which screens / events lead here)
   - Exit points (which screens / events leave here)
   - Primary actions (3–5 bullet points — what user can DO on this screen)
5. **Functional Requirements** — bulleted list of what the feature does
6. **Non-functional Requirements** — performance, offline support, accessibility floor (WCAG AA, 48dp targets)
7. **State Machine** — only if non-trivial (3+ states). List states + valid transitions. Skip section otherwise.
8. **Data Needs** — tables / DataStore keys / API endpoints. If none, write "Local-only, no persistence beyond session."
9. **Analytics Events** — bulleted list. Each: event_name + 2-3 properties. Use `feature_action` naming.
10. **Out-of-Scope (V1)** — explicit list of what this PRD does NOT cover
11. **Open Questions** — bulleted list of decisions still pending. If none, write "None."

## Pre-fill Lookup Table

Auto-populate these fields with sensible defaults — never emit blank fields, never ask the student to write from scratch. The student picks from the options offered or accepts the default:

| Feature pattern | Pre-fill |
|---|---|
| Auth feature | Screens: Welcome, Signup, Login, ForgotPassword. Methods: email + 1 social. Data: user table with id/email/created_at |
| Onboarding | Screens: Welcome, PermissionPrompt, FirstAction. State: 3-step wizard. Analytics: onboarding_started, onboarding_step_completed, onboarding_finished |
| Settings | Screens: Settings (single). Items: Account, Notifications, Theme, About. Out-of-scope V1: import/export |
| Core Loop default | Screens: List, Detail, Create. Out-of-scope V1: bulk operations, search filters beyond keyword |

**No Paywall pre-fill** — monetization is out of scope this phase. If a Paywall feature somehow reaches Phase 2, drop it and surface the [Out of scope this phase] note from Phase 1.

Pre-fills are a starting point, not a lock-in — student can override any value after.

## Validation Before Write

Before writing each PRD file:
- Screens list non-empty (else: ask student to define screens, don't emit feature with 0 screens)
- Every screen has all 5 sub-fields (name, purpose, entry, exit, actions)
- User Stories ≥ 1
- Functional Requirements ≥ 1

If validation fails → ask student per missing field, then retry.

## Output Format Per File

See `references/feature-prd-template.md` — use as exact template. Frontmatter first, then sections in order above.

## After Loop

Print summary:

```
[kta-prd-pipeline] Phase 2 complete:
✓ 01-onboarding.md
✓ 02-run-logging.md
✓ 03-training-calendar.md
✓ 04-progress-dashboard.md
✓ 05-settings.md

Total: 5 features, 11 screens.
Next: Phase 3 — Cross-Feature Review.
```

Then proceed to Phase 3.
