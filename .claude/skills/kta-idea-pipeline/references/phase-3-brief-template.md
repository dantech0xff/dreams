# Phase 3 — Idea Brief Template

Output file — `plans/{slug}/idea-brief.md`. Filled by skill, consumed by `kta-prd-pipeline`.

**Scope: feature build only.** No monetization section. If user volunteered a pricing/billing/ads preference earlier, capture it as a single `monetization_note:` line in frontmatter — do NOT expand into a model/price/justification section.

## Required Schema

```markdown
---
title: "{Idea Title}"
status: refined
created: {YYYY-MM-DD}
surface: {lean-mvp | standard-mvp | full-app}
connectivity: {offline-only | offline-first | online-required}
sharing: {none | share-out | light-social | full-social}
accounts: {anonymous | optional | required}
monetization_note: "{optional one-line capture if user volunteered, else omit}"
---

# {Idea Title} — Idea Brief

## 1. Problem
{One paragraph. The pain, sharply.}

## 2. Target User
- **Primary**: {role + context, specific}
- **Secondary** (optional): {if applicable}
- **NOT for**: {explicit exclusions to keep MVP focused}

## 3. Value Proposition
{One sentence the user would say after first session: "Now I can ___ without ___"}

## 4. Core-Value Feature (⭐)
The ONE feature that, if removed, kills the value prop. Name it explicitly here. Everything else in MVP justifies its slot relative to this.

## 5. MVP Scope (≤4 weeks solo)
1. ⭐ {Core-value feature — from section 4}
2. {Feature 2}
3. {Feature 3}
4. {Feature 4 — optional}
5. {Feature 5 — optional}

**Out of scope (V1)**: {explicit list — these are tradeoffs already accepted in Phase 2}

## 6. Differentiation
- **Closest competitor 1**: {app name} — we differ by {axis}
- **Closest competitor 2**: {app name} — we differ by {axis}
- **Killer feature**: {the thing users tell friends about}

## 7. Key Risks
1. **Riskiest assumption** (from Phase 2 refine): {one sentence, plus how to test cheap}
2. {Other risks}

## 7.1 Refined Spec (from Phase 2)
- **Surface**: {Lean MVP / Standard MVP / Full app} — {1-line rationale}
- **Connectivity**: {Offline-only / Offline-first / Online-required}
- **Sharing**: {None / Share-out / Light social / Full social}
- **Accounts**: {Anonymous / Optional / Required}
- **Wedge**: {first-100-users channel}
- **MVP features (locked)**: {bullet list — these become PRD scope}
- **v2 features (deferred)**: {bullet list — explicitly out of MVP}

## 7.2 Scope Tradeoffs Accepted (from Phase 2 Risk/Reward/Tradeoff blocks)
Carry forward the consolidated tradeoff bullets from Phase 2. Each entry:
- **Cut/Narrowed**: {what we said no to}
  - Risk: {what could bite us}
  - Reward: {speed/clarity gained}
  - Tradeoff: {what user need is deferred}

This section is the contract — `kta-prd-pipeline` MUST honor it (no PRD for deferred features).

## 8. Success Metrics (V1)
- **D7 retention target**: X%
- **First user milestone**: {e.g., 1K downloads in 60 days}
- **Activation signal**: {what proves a user got the core value, e.g., "completed first session of ⭐ feature"}

## 9. Tech Stack Fit Notes
- Screen list (rough) — {list candidate features}
- Backend — {needed? what tables / services?}

## 10. Feature Seeds (for kta-prd-pipeline)
5–8 candidate features this MVP needs. One line each. Group hint optional (Acquisition / Activation / Core Loop / Retention / Settings) — `kta-prd-pipeline` will refine groupings. **No Monetization bucket** in this phase.

- ⭐ {core-value feature} — {1-line summary} [core-loop]
- {feature 2} — {1-line summary} [bucket-hint]
- {feature 3} — {1-line summary} [bucket-hint]
- {feature 4} — {1-line summary} [bucket-hint]
- {feature 5} — {1-line summary} [bucket-hint]
- ... (up to 8)

## 11. Next Steps
→ Run `kta-prd-pipeline` to decompose into per-feature PRDs with screens nested. Skill will offer to auto-chain (Phase 4 handoff prompt).
→ Monetization strategy is a separate later pass once feature value is validated.
```

## After Writing the Brief

Once this file is written, the skill MUST run **Phase 4 Handoff** (defined in `SKILL.md`):
1. Confirm the absolute brief path back to the user.
2. Use `AskUserQuestion` to offer two options — "Run PRD next" (auto-invoke `kta-prd-pipeline` with brief path) or "Exit to read".
3. If "Run PRD next" → invoke `Skill` tool with `skill: "kta-prd-pipeline"` and `args` containing the brief path.
4. If "Exit to read" → print path once more and stop.

Do NOT skip the handoff prompt.

## Fill Rules

- Every field MUST be populated (no "TBD" — if unknown, write the assumption explicitly)
- "NOT for" section is mandatory — forces narrowing
- Section 4 (Core-Value Feature) is mandatory — exactly ONE starred feature, named in plain English
- Killer feature must be testable in MVP (not "AI assistant" handwave)
- Risks must be ranked, not listed at random
- **Section 7.2 (Scope Tradeoffs) MANDATORY** — if Phase 2 produced no tradeoff blocks, the brief is incomplete and the skill must loop back
- **Feature Seeds (section 10) MANDATORY — 5 minimum, 8 maximum**, with ⭐ marker on the core-value feature. Downstream skill refuses to run without this section populated.
- **Do NOT** include a Monetization section, pricing field, or revenue model. `monetization_note:` frontmatter ONLY (if at all).

## Sensible Defaults (always pre-fill when student didn't choose)

- Section 8 — D7 retention target → `15%` (typical indie baseline)
- Section 9 — Backend → `none (offline-first)` unless features demand sync
- Section 10 — Feature Seeds → ensure Auth, Settings always included if relevant; ⭐ marker on whichever single feature delivers the core value
