# Phase 1 — Generate

Produce **exactly 2 candidate ideas**. Keep output tight — student doesn't want to wade through 4-5 options.

**Scope of Phase 1: feature value only.** Do NOT ask about or include monetization, pricing, IAP, ads, or subscription. We're picking the right thing to BUILD; how to charge for it is a separate later pass once value is validated.

## Per-Idea Schema

```markdown
### Idea N: {Working Title}

- **Problem**: One sentence. Sharper than "people want X" — name the pain.
- **Target user**: Specific. "Solo Android devs shipping side projects" beats "developers".
- **Core value**: What changes for the user. One sentence — "Now I can ___ without ___".
- **MVP scope (≤4 weeks solo)**: 3-5 features max. List them. Star (⭐) the ONE feature that delivers the irreplaceable core value.
- **Differentiation**: vs the 2-3 closest existing apps. If you can't name them, the idea is weaker.
- **Killer feature**: the one thing that makes a user tell a friend.
```

## Generation Heuristics

Bias toward ideas where:
1. **Personal pain** — solo dev has the problem themselves (proxy for taste)
2. **Narrow niche** — "habit tracker for marathon runners" beats "habit tracker"
3. **Daily-use shape** — apps used weekly+ retain better than annual
4. **Offline-first feasibility** — fewer backend dependencies for indie scale
5. **One sharp core value** — the idea collapses to a single sentence and a single starred feature

The 2 ideas should be **meaningfully different** — different niche, different user behavior, or different core value. Don't ship two near-duplicates.

## Anti-Patterns (avoid)

- "Uber for X", "Airbnb for Y" — marketplace = network effect = no indie advantage
- Social apps from scratch — chicken-and-egg, requires marketing budget
- "AI {anything}" without a sharp wedge — table stakes, not differentiation
- Ideas needing real-time multiplayer — operational cost crushes indie margin
- B2B SaaS — long sales cycles incompatible with mobile indie
- Ideas where you can't name the ONE core-value feature

## Output Format

Present **2 candidates** as a numbered list. Then use `AskUserQuestion` (NOT raw prompt) to pick which to advance:

```
header: "Pick Idea"
question: "Which candidate advances to Phase 2 refine?"
multiSelect: false
options:
  - <option per candidate, label = working title (≤17 chars), description = problem + target user>
  - label: "Both", description: "Refine both candidates"
  - label: "Regenerate", description: "Neither lands, try again"
```

Only the selected candidate(s) proceed to Phase 2. If user picks "Regenerate", produce 2 new ideas with adjusted heuristics (note what to change based on their feedback).

## If User Volunteers Monetization Preferences

If the student mentions pricing/IAP/ads/subscription unprompted, do NOT branch into a monetization decision tree. Capture it as a one-line `monetization_note:` to carry into the brief frontmatter, then continue with feature scoping. Acknowledge with: "Noted — we'll revisit pricing in a later pass once the core value is validated."
