# Phase 2 — Refine & Adjust

**Purpose**: take the selected candidate and decide the *right shape* before writing PRD/designs. NOT a kill phase. The idea is moving forward — we're tuning scope and approach.

**Scope of Phase 2: feature surface only.** No monetization, pricing, IAP, ads, or subscription decisions. We're locking what to BUILD; charging strategy is a later separate pass once the feature value is proven.

Goal: walk out of Phase 2 with clear answers to scope questions (offline vs online, sharing, MVP vs full app, accounts, core-value feature set) so `kta-prd-pipeline` has unambiguous input.

## Mantras (Surface These to the User)

**Ship finished > ship perfect. Perfect never exists. Ship and improve is the key.**

- **YAGNI** — You Aren't Gonna Need It. Default-cut every feature that's not directly serving the core value loop. Anything that *might* matter goes to v2.
- Bias every scope decision toward the smallest version that proves the riskiest assumption.
- A shipped Lean MVP beats a polished concept that never reaches the Play Store.
- When the user is tempted to add scope, surface this reminder before accepting the addition.
- **Core-value-first**: the MVP collapses to ONE starred feature that delivers the irreplaceable user value. Everything else justifies its slot or is cut.

## Approach Analysis

For the selected idea, produce a tight analysis covering:

```markdown
### {Idea Title} — Approach Analysis

- **Core loop**: one sentence describing what user does in a typical session.
- **Core-value feature (⭐)**: the ONE feature that, if removed, kills the value prop. Name it.
- **Riskiest assumption**: the one belief that, if wrong, kills the idea. How could we test it cheap?
- **Wedge**: where do the first 100 users come from? (one channel, named)
- **Tech-stack fit**: how this maps to DRE-KT + Compose + Supabase. Any fights with the stack?
- **Build estimate (solo, honest)**: weeks to MVP. Multiply naive estimate by 2.5x.
- **Substitute behavior**: what does the target user do today instead?
```

Keep this under ~20 lines. Do NOT score, do NOT verdict. This is context for the scope choices below.

## Risk / Reward / Tradeoff Framing (NON-NEGOTIABLE)

**Every scope decision below has consequences.** When asking the user to scope down (cut features, narrow surface, defer flows), each `AskUserQuestion` option's `description` MUST hint at the tradeoff in one short clause.

For the **post-decision Risk/Reward/Tradeoff block**, after each scope decision is made, emit a 3-bullet recap:

```markdown
**Decision: {what was picked}**
- Risk: {what could go wrong by cutting / narrowing}
- Reward: {what speed / clarity / focus the cut buys}
- Tradeoff: {what specific user need is deferred to v2}
```

This block is NOT optional — it's the contract that keeps scope-down honest. If the student can't articulate the tradeoff, surface this and let them reconsider.

## Scope Decision Points (use AskUserQuestion for EACH)

Walk the student through these decisions one at a time. Each decision narrows the PRD scope. Each option description must hint at the consequence (e.g., "ship in 2-3 weeks but no settings UI").

### 1. App Surface

```
header: "Surface"
question: "MVP shape for {title}?"
multiSelect: false
options:
  - label: "Lean MVP", description: "Single core flow only — fastest to ship, no polish layers"
  - label: "Standard MVP", description: "Core flow + onboarding + settings — balanced, 4-6 weeks"
  - label: "Full app", description: "Multiple flows + accounts + sync — slowest, highest scope risk"
```

After selection, emit Risk/Reward/Tradeoff block.

### 2. Connectivity

```
header: "Connectivity"
question: "Offline-first or online-required?"
multiSelect: false
options:
  - label: "Offline-only", description: "All data local — zero backend cost, no sync UX"
  - label: "Offline-first", description: "Local primary + opt-in sync — middle ground, more code"
  - label: "Online-required", description: "Always-on backend — accounts mandatory, infra cost"
```

After selection, emit Risk/Reward/Tradeoff block.

### 3. Social / Sharing

```
header: "Sharing"
question: "Any social or sharing features?"
multiSelect: false
options:
  - label: "None", description: "Solo-use app — no social loops, harder organic growth"
  - label: "Share-out", description: "System share sheet only — cheap virality hook"
  - label: "Light social", description: "Friends list + leaderboard — backend + moderation light"
  - label: "Full social", description: "Profiles + feed + comments — heavy backend + abuse risk"
```

After selection, emit Risk/Reward/Tradeoff block.

### 4. Account Model

```
header: "Accounts"
question: "Account requirement?"
multiSelect: false
options:
  - label: "Anonymous", description: "No account — fastest first-run, no sync, no recovery"
  - label: "Optional", description: "Anonymous default + optional sign-in — best of both, more code"
  - label: "Required", description: "Sign-in gate at first launch — friction risk, easier multi-device"
```

After selection, emit Risk/Reward/Tradeoff block.

### 5. Core-Value Feature Cut/Add Pass

After the four decisions above, present a refined feature list (5–8 items) with the ⭐ core-value feature clearly marked, and use `AskUserQuestion` `multiSelect: true` to flag what stays in MVP vs cuts to v2.

**Each option description should hint at what the user loses if cut.**

```
header: "MVP Scope"
question: "Which features ship in MVP? (others go to v2)"
multiSelect: true
options: <one per candidate feature, label ≤17 chars, description = user value lost if cut, in 1 line>
```

The ⭐ core-value feature is non-optional — flag it pre-checked and warn if user tries to deselect it. If the student wants to **add** a feature not listed, use a follow-up free-text capture.

After the cut pass, emit a single consolidated Risk/Reward/Tradeoff block summarizing the final MVP feature set:

```markdown
**Final MVP feature set: {N items locked, M deferred}**
- Risk: {what user behavior or value gap might bite us by cutting these M items}
- Reward: {weeks saved, clarity gained by narrowing to N}
- Tradeoff: {explicit list of what v2 owes the user}
```

## Output of Phase 2

A short refined-spec block to feed Phase 3 brief:

```markdown
### {Title} — Refined Spec

- Surface: {Lean/Standard/Full}
- Connectivity: {Offline-only/Offline-first/Online-required}
- Sharing: {None/Share-out/Light social/Full social}
- Accounts: {Anonymous/Optional/Required}
- Core-value feature (⭐): {one feature, the irreplaceable one}
- MVP features (locked): [...]
- v2 features (deferred): [...]
- Riskiest assumption: {one sentence}
- Wedge: {one sentence}
- Scope tradeoffs accepted: {1-3 bullets summarizing what was cut and why}
```

This block goes straight into `idea-brief.md` so PRD/design phases have unambiguous scope.

## What NOT to do here

- Do NOT score the idea on a 1–5 scale.
- Do NOT issue "kill / refine / advance" verdicts.
- Do NOT challenge whether the idea should exist — assume it ships, decide its shape.
- Do NOT skip `AskUserQuestion` for the scope decisions — these MUST be explicit choices.
- Do NOT ask about monetization, pricing, IAP, ads, or subscription. If user volunteers, capture as `monetization_note:` and move on.
- Do NOT skip the Risk/Reward/Tradeoff block after each decision — that's the contract.
