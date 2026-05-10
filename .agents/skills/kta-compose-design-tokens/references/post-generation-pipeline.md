# Post-Generation Pipeline

MANDATORY 4-phase pipeline after token file generation. Driven by the tracking file at `plans/{date-slug}/screens-todo.md`.

## Phase 1: Create Token-Backed Components

Load `references/component-implementation-requirements.md` for full specs.

**P0 components (always created):**
- `{Prefix}Surface` — token-backed Surface wrapper
- `{Prefix}Background` — fullscreen background with token color
- `{Prefix}Text` — Text with token typography + color defaults
- `{Prefix}Label` — small text variant for labels/captions

**P1 components (created when matching component tokens exist):**
- `{Prefix}FilledButton`, `{Prefix}OutlinedButton`, `{Prefix}TextButton`
- `{Prefix}Card`, `{Prefix}OutlinedCard`
- `{Prefix}TextField`
- `{Prefix}Dialog`

**P2 components (comprehensive scope only):**
- `{Prefix}BottomSheet`, `{Prefix}Snackbar`, `{Prefix}TopBar`
- `{Prefix}Badge`, `{Prefix}Chip`, `{Prefix}Divider`, `{Prefix}SkeletonRow`

**Rules:**
- Every component reads tokens via `{Prefix}Theme.*` — zero hardcoded values
- Every component file includes at least one `@Preview`
- Components go in UI module, NOT theme module
- Component creation is delegated per-component to `kta-compose-developer` (or batched as a single delegation when count ≤ 4)

## Phase 2: Recreate Source Screens (loop driven by tracking file)

When tokens originate from a Figma / Stitch / Claude Design source. Load `references/source-screen-recreation.md` for full protocol and `references/screen-selection-workflow.md` for the tracking-file format.

| Source | What to Recreate |
|--------|-----------------|
| **Figma** | Fetch frames/pages via MCP → recreate each selected as Compose screen |
| **Stitch** | Fetch screen code/image via MCP → translate to Compose |
| **Claude Design** | Parse kta-design-spec / Claude artifact / pasted markdown → recreate each screen |

**Loop (strict numerical order):**
1. Read `plans/{date-slug}/screens-todo.md`.
2. Pick the lowest `pending` row.
3. Set status to `in_progress` → save tracking file.
4. Spawn `kta-compose-developer` agent for that single row (see Phase 4 prompt template).
5. Agent updates the row to `done` (with output path) or `failed` (with reason) before returning.
6. If `failed` → STOP and surface to user. Do not skip ahead.
7. If `done` → repeat from step 2 until no `pending` rows remain.

**Recreation rules (enforced by agent):**
- Each screen = standalone `@Composable` (no ViewModel — pure UI)
- Use ONLY token-backed components + `{Prefix}Theme.*` accessors
- Match source layout: column/row arrangement, spacing, hierarchy
- Include `@Preview` with light + dark theme variants
- Animations on every state change (no snap-cuts)
- Advanced layout mapping: `references/figma-advanced-layout-mapping.md` / `references/stitch-advanced-css-mapping.md`
- Image/icon handling: `references/asset-icon-pipeline.md`

## Phase 3: Token Showcase Screen

Run only after all rows in the tracking file are `done`. Load `references/token-showcase-screen.md` for full template. Displays ALL created components + recreated screens:
- Color palette swatches (all semantic colors)
- Typography scale (representative styles)
- Spacing & shape samples
- All component variants (enabled + disabled states)
- Thumbnails / entry points to each recreated screen
- Light/dark theme `@Preview` functions

Delegated to `kta-compose-developer` as a single final invocation.

## Phase 4: Delegate to `kta-compose-developer`

Spawn via `Agent` tool with `subagent_type: "kta-compose-developer"`. The skill is the orchestrator — agent only handles ONE row (or one component / showcase) at a time.

**Per-row prompt template (Phase 2 loop):**
```
Task: Implement screen row #{N} from tracking file.

Tracking file: {project_root}/plans/{date-slug}/screens-todo.md
Row to implement:
  N: {number}
  Name: {ScreenName}
  Source: {figma | stitch | claude-design}
  Source ID: {frameId | screenId | spec path}
  Output File: {target .kt path}

Theme path: {project_root}/{ui-module}/.../theme/
Prefix: {Prefix}
Generated tokens: {list .kt paths}
Available components: {list from Phase 1}

Behavior:
- Read tracking file, set this row to `in_progress` before starting.
- Use ONLY {Prefix}Theme.* and token-backed components — zero hardcoded values.
- Light + dark @Preview required.
- Animate every state change.
- Compile via gradle module before reporting done.
- On success: set row to `done` with output path. On failure: set `failed` + reason.
- Do NOT advance to the next row.

Work context: {project_root}
Reports: {project_root}/plans/{date-slug}/reports/
```

**Phase 1 component-batch prompt template:**
```
Task: Implement P0 + P1 token-backed components.
Components: {list}
Theme path: {...}
Prefix: {...}
Output module: {ui-module}
Compile after each component file.
```

**Phase 3 showcase prompt template:**
```
Task: Build {Prefix}TokenShowcaseScreen.
Aggregate: all components from Phase 1 + recreated screens listed in `done` rows of {tracking_file}.
Layout sections: colors, typography, spacing, shapes, components, screens.
Compile + light/dark @Preview required.
```

**Skip delegation when:** user said `"none"` / `"tokens only"`, or audit `--fix-suggestions` only.

The agent will: read tracking file → discover conventions → scout → implement → compile-check → update tracking file → return.
