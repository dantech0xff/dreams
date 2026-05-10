# Phase 1 — Decompose

Read `plans/{slug}/idea-brief.md` → propose 4–8 features grouped by user journey → student edits → hand off to Phase 2.

## Pre-flight Validation

Before proposing anything, verify:
1. `idea-brief.md` exists at `plans/{slug}/idea-brief.md`
2. `feature_seeds:` section present with 5–8 entries

If any required field missing → STOP, print:

```
[kta-prd-pipeline] Cannot proceed:
- Missing: {field}
- Run kta-idea-pipeline first, or edit idea-brief.md to add the missing field.
```

## Journey Buckets (FIXED — 5 buckets, NO Monetization)

| Bucket | Purpose | Typical Features |
|---|---|---|
| **Acquisition** | First-impression, install, landing | App listing copy, share screen, deep-links |
| **Activation** | First-run, onboarding, get-to-aha | Welcome / signup / permissions / tutorial / first-action |
| **Core Loop** | The thing the app does, repeated | Main UI, the daily-use feature(s) |
| **Retention** | Bring user back | Notifications, history, streaks, reminders |
| **Settings** | Account, preferences, support | Profile, notifications, theme, about, contact |

NOT all apps need all buckets. Acquisition is often outside the app (Play Store), so usually 0 features. Settings usually 1 feature. Core Loop is where most features land.

**Monetization is OUT OF SCOPE in this phase.** This pipeline is feature-build only — deliver the core app first; monetization (paywall, IAP, subscriptions, billing, ads) belongs to a later phase. If `feature_seeds` contain a paywall/upgrade/IAP/subscription/billing/ads item, drop it from the proposed list and tell the student:

```
[kta-prd-pipeline] Skipping "{seed}" — monetization is out of scope in this phase.
Current objective: deliver core features. Defer monetization to a later phase.
```

## Proposal Process

1. Read `feature_seeds` from `idea-brief.md`.
2. **Strip monetization seeds first** — drop any seed describing paywall, IAP, in-app purchase, subscription, billing, premium upgrade, restore purchase, or ads. Note each dropped seed in the student-facing summary as "Deferred — out of scope this phase."
3. Map remaining seeds → most-fitting bucket (Acquisition / Activation / Core Loop / Retention / Settings ONLY). Combine seeds that overlap (e.g., "log run" + "edit run" → 1 feature "RunLogging").
4. Add OBVIOUS missing core features per bucket (e.g., a Core Loop app with no Settings → add a basic Settings feature). DO NOT add monetization features even if they appear missing.
5. Cap at 8. If proposed list >8, consolidate by:
   - Merging adjacent features in same bucket
   - Deferring nice-to-haves to a labeled `[V2]` list outside the cap
5. Present numbered list to student:

```markdown
## Proposed Feature Breakdown

| # | Feature | Bucket | Summary | Est. screens |
|---|---|---|---|---|
| 1 | Onboarding | Activation | Welcome → permission → first run | 3 |
| 2 | RunLogging | Core Loop | Log distance, time, route | 2 |
| 3 | TrainingCalendar | Core Loop | Plan + view runs by week | 2 |
| 4 | ProgressDashboard | Retention | Charts + streak | 1 |
| 5 | Settings | Settings | Account, theme, about | 1 |

**Total**: 5 features, ~9 screens.

**[V2 — deferred]**: Social sharing, route map, training plans library
**[Out of scope this phase]**: Monetization (paywall / IAP / subscriptions / ads) — handled later
```

## Student Edit Loop

Ask: "Edit / approve / consolidate?" Accept:
- Rename a feature
- Move feature to different bucket
- Merge two features
- Drop a feature
- Defer to V2
- **Add a feature** — only if total stays ≤8; else skill responds:

```
[kta-prd-pipeline] Hard cap 8 features. To add "{name}":
- Drop one of the existing features, OR
- Consolidate two adjacent features, OR
- Defer "{name}" to [V2].

Which?
```

Loop until student approves.

## Output (memory only — passed to Phase 2)

Approved feature list as a structured object:

```yaml
features:
  - slug: onboarding
    name: Onboarding
    bucket: Activation
    summary: Welcome → permission → first run
    estimated_screens: 3
  - slug: run-logging
    name: RunLogging
    bucket: Core Loop
    ...
```

Phase 2 consumes this. No file written at this phase — Phase 3 writes `INDEX.md`.

## Heuristics

- 4–6 features is the sweet spot for a 4-week solo MVP
- Onboarding/Activation usually 1 feature, not 3 (don't fragment)
- "Core Loop" usually 1–3 features — the thing users return for
- Settings is always 1 feature (catch-all) unless app is settings-heavy
- Monetization NEVER appears — out of scope this phase. Defer to a later phase. Finish > Perfect
