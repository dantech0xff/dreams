# Screen Selection & Tracking Workflow

MANDATORY for any Figma / Stitch / Claude Design source that contains screens. After tokens are extracted, enumerate every screen, ask the user which to build, persist the choice to a tracking file in the plan dir, then implement screens **in strict numerical order** until the file shows all `done`.

## Step 1: List Available Screens

### From Figma
```
mcp__figma__get_metadata(fileKey) → parse top-level frames
```
Each top-level frame = one screen candidate. Extract: frame name, frame ID, dimensions.

### From Stitch
```
mcp__stitch__list_screens(projectId) → screen list with IDs and names
```

### From Claude Design
- `kta-design-spec` output: each `screen-*.md` file under the spec dir = one screen.
- Multi-screen markdown blob: each `## Screen: <Name>` heading = one screen.
- Single HTML/SVG artifact: typically one screen (skip selection if just 1).

If the source produces a single screen, skip Step 2's prompt — auto-select it but still write the tracking file.

## Step 2: Present Screen List

Numbered, source-ordered list. Numbering is the **build order** — locked once tracking file is written.

```
Found {N} screens in "{ProjectName}":

  1. [Login] — 390×844
  2. [Dashboard] — 390×844
  3. [Transaction History] — 390×844
  4. [Profile Settings] — 390×844
  5. [Payment Flow] — 390×844

Which screens to build?
  "all"       → build every screen
  "1,3,5"     → specific screens by number
  "1-5"       → range of screens
  "1-3,7,10"  → mixed range + specific
  "none"      → tokens only, no screens
```

Use `AskUserQuestion` to collect the selection.

## Step 3: Parse Selection

| Input | Interpretation |
|-------|---------------|
| `all` | Every listed screen |
| `none` | Skip implementation entirely |
| `1,3,5` | Positions 1, 3, 5 |
| `1-5` | Positions 1 through 5 inclusive |
| `1-3,7,10-12` | Mixed |
| Single number `3` | Just screen 3 |

Validate: numbers within range, no duplicates. Re-ask if invalid.

## Step 4: Write Tracking File (MANDATORY)

Persist the selection **before** implementing anything. Path:

```
{project-root}/plans/{date-slug}/screens-todo.md
```

Where `{date-slug}` matches the active plan dir injected by hooks (e.g. `260501-1902-poststamp-camera`). If no active plan exists, create one named after the source (e.g. `260501-1902-figma-{fileName}` / `260501-1902-stitch-{projectId}` / `260501-1902-claude-design-{slug}`).

**Tracking file format** (markdown table — must round-trip parseable):

```markdown
# Screens TODO — {Source} ({ProjectName})

Source: figma | stitch | claude-design
Source ref: {fileKey | projectId | spec dir path}
Generated tokens: {path/to/theme}
Order: STRICT NUMERICAL — do not reorder.

| # | Screen | Source ID | Status | Output File | Notes |
|---|--------|-----------|--------|-------------|-------|
| 1 | Login | frame:123 | pending | — | — |
| 2 | Dashboard | frame:124 | pending | — | — |
| 3 | Profile | frame:125 | pending | — | — |
```

Statuses: `pending` → `in_progress` → `done` | `failed` (with reason in Notes).

Only screens the user picked appear in the table. Skipped screens are omitted entirely (don't carry them as `skipped` rows — keeps the table tight).

## Step 5: Implement One-by-One (Strict Numerical Order)

Loop:
1. Read the tracking file.
2. Find the lowest-numbered row with `status = pending`.
3. Set its row to `in_progress`, save the file.
4. Generate the Compose screen file using the workflow in `references/source-screen-recreation.md`.
5. On success: set status to `done`, fill `Output File` column with the relative path, save.
6. On failure: set status to `failed`, fill `Notes` with the error summary, save, then STOP and surface to the user — do not skip ahead.
7. Repeat until no `pending` rows remain.

Never implement a higher-numbered screen while a lower-numbered one is `pending` or `in_progress`.

## Step 6: Resume Safety

If the session is interrupted, the next invocation must:
1. Locate the tracking file in the active plan dir.
2. If found: read it, resume from the lowest `pending` row. Do NOT re-prompt for selection.
3. If a row is stuck in `in_progress`, treat it as `failed` and surface to the user.
4. If file missing: re-run Steps 1–4.

## Step 7: Completion

When every selected row is `done`:
1. Append a `## Summary` block to the tracking file with totals + timestamps.
2. Run Post-Generation phase 3 (Token Showcase) including all built screens.
3. Delegate final implementation polish to kta-compose-developer if not already done per-screen.

## Step 8: Incremental Addition

User wants to add more screens later:
1. Read existing tracking file in plan dir.
2. Re-list source screens, marking already-`done` ones as `BUILT — skip`.
3. User picks from remaining only.
4. Append new rows to the same tracking file (numbering continues from existing max + 1).
5. Resume Step 5 loop.

## Constraints

- One tracking file per plan dir. Never create parallel files.
- Build order is fixed at write time. Do not allow user reordering — if they want a different order, they must regenerate the file.
- Implementation is sequential — never parallelize across rows. (kta-compose-developer delegation per screen is allowed; cross-screen batching is not.)
- Keep the tracking file under 200 lines. If a project has more than ~50 screens, ask the user to scope before writing.
