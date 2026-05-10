# Phase 0 — Discover (conditional)

Run ONLY when student says "I don't have an idea", "help me find one", or skill detects free-text input lacks a concrete concept. Otherwise jump to Phase 1.

## Output

5–10 seed concepts (one-line each) → feeds Phase 1 as candidate ideas. Each seed:

```
{Working title} — {target user} — {pain it addresses}
```

## Trigger Paths

Use `AskUserQuestion` to pick ONE trigger path. Do NOT emit raw markdown choices.

```
header: "Trigger Path"
question: "Which discovery angle resonates?"
multiSelect: false
options:
  - label: "Personal Pain", description: "Surface annoyances from your past 7 days"
  - label: "Hobby-Driven", description: "Existing skill/passion as moat"
  - label: "Niche-of-Niche", description: "Drill broad category into 1-person-wins subniche"
  - label: "Reskin+Wedge", description: "Saturated category + sharp differentiator"
  - label: "Trending Store", description: "Currently-rising Play Store categories"
  - label: "Daily-Tool Gap", description: "Recurring task done badly today"
  - label: "Sunset Revival", description: "Refresh formerly-popular dead category"
```

Run ONLY the selected path; don't run all.

### 1. Personal Pain
Surface annoyances of the past 7 days.

Prompts:
- "What did you complain about this week (out loud or in your head)?"
- "What task did you redo because the existing app didn't quite work?"
- "What did you wish your phone could do at 11pm last night?"

For each pain → frame as 1-line seed.

### 2. Hobby-Driven
Existing skill/passion as moat — domain knowledge competitors lack.

Prompts:
- "What do you spend weekend hours on that isn't coding?"
- "Where do friends ask you for advice?"
- "Which subreddit / forum / Discord do you read daily?"

Output: 5–10 seeds where domain expertise is the wedge.

### 3. Niche-of-Niche
Pick broad category → narrow to subniche where 1 person can win.

Process:
- Student names broad category (fitness / finance / productivity / language)
- Drill down 2 levels (fitness → running → marathon prep → sub-3:30 marathoners)
- Generate seeds at deepest level

### 4. Reskin-with-Wedge
Saturated category + sharp differentiator. Not "another habit tracker" — "habit tracker that ONLY does X".

Process:
- Pick saturated category (todo / habit / meditation / journaling)
- Identify 1 axis competitors don't optimize (offline-only / privacy-first / no streaks / single-tap / paid-only)
- Combine — "X for Y, but with Z"

### 5. Trending Play Store (WebFetch)
Discover currently-rising categories. Use sources from `discovery-sources.md`.

Process:
- WebFetch Play Store top-charts page (1 retry on fail)
- Extract category labels appearing 3+ times in top 100
- For each, ask "what's missing in the current top 5?"
- Frame seeds as the gap

**Fallback** — if WebFetch fails twice, prompt: "Which app categories did you notice growing on Play Store recently?" Use student answers as substitute.

### 6. Daily-Tool Gap
Recurring task done badly with current apps.

Prompts:
- "What do you open 5+ times a day that's slow / clunky / over-featured?"
- "What do you do in a browser that should be a native app?"
- "What 'pro' app could be replaced with a simple, focused tool?"

Output: seeds attacking specific pain in established tool.

### 7. Sunset-Tool Revival
Formerly popular category, now dead or stagnant. Refresh with modern tech.

Process:
- Brainstorm 5 app categories that peaked 3-5 years ago (RSS readers, podcast managers, voice memos, simple drawing pads, mood trackers, dictionary apps, weather widgets)
- For each, identify why interest faded — and whether the underlying need still exists
- Frame seeds as "modern minimal {category}"

## Output Format

Present seed concepts as a numbered list:

```markdown
## Seed Concepts ({path-name})

1. {Title} — {target user} — {pain}
2. ...
```

Then use `AskUserQuestion` to flag favorites (advance to Phase 1):

```
header: "Pick Seeds"
question: "Which seed concepts advance to Phase 1?"
multiSelect: true
options: <one option per seed, label = working title (≤17 chars), description = target user + pain>
```

Selected seeds become candidate-idea anchors for Phase 1.

## Heuristics

- A good seed names a SPECIFIC user, not "people"
- A good seed names a SPECIFIC pain, not "make X easier"
- 5 strong seeds beat 10 weak ones — quality over quota
- If student rejects all seeds, re-run with a different trigger path
