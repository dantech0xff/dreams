# Claude Design Integration

Use this when the seed source is **Claude Design** — i.e. design specs, mockups, or artifacts produced by Claude (not Stitch, not Figma).

## Accepted Forms

| Form | How to detect |
|------|---------------|
| `kta-design-spec` output | File path matches `plans/**/design/screen-*.md` or contains a `Design Tokens` section |
| Claude HTML/SVG artifact | File `.html` / `.svg` produced by Claude, or pasted artifact block in conversation |
| Pasted design spec markdown | Markdown block in chat with sections like `## Layout`, `## Components`, `## Tokens`, `## Color`, `## Typography` |

If none of the above is present in the user's input, the **input gate** in `SKILL.md` rejects the request — do not start work.

## Extraction Workflow

### A. From `kta-design-spec` output

1. **Read** the spec file with `Read` tool.
2. Locate sections (canonical headings emitted by `kta-design-spec`):
   - `## Design Tokens` — color/typography/spacing/shape values
   - `## Screen Layout` — top bar, sections, bottom nav presence
   - `## Components` — list of components per screen with state notes
   - `## Interactions` — motion/animation hints
3. Map directly:
   - `Design Tokens` → primitive + semantic token JSON for `generate-kotlin-tokens.py`
   - `Screen Layout` → screen recreation tree (Scaffold/Column/LazyColumn)
   - `Components` → component spec list passed to Post-Generation phase 1
4. Honor the spec's prefix / scope hints if present; otherwise use `detect-project-config.py` defaults.

### B. From Claude HTML/SVG artifact

1. **Read** the artifact source.
2. Parse inline styles (CSS in `<style>` blocks, `style=""` attributes, SVG `fill` / `stroke` / `font-size`).
3. Cluster colors by usage role:
   - Most-frequent background → `surface` / `background`
   - Text colors paired with backgrounds → `onSurface`, `onBackground`
   - Accent / CTA fill → `primary`
   - Error / warning if explicitly used
4. Capture spacing rhythm — most-common `padding` / `margin` / `gap` values become primitive spacing scale (`xs`, `sm`, `md`, `lg`, `xl`).
5. Capture typography scale — group by `font-size` + `font-weight` pairs into roles (`display`, `headline`, `title`, `body`, `label`).
6. Capture corner radii — distinct `border-radius` values become primitive shape scale.

### C. From pasted markdown spec

1. Treat the pasted block as virtual `kta-design-spec` output.
2. If sections are missing, infer from prose. Where ambiguous, choose conservative defaults from `defaults/token-scope-presets.md`.
3. Never invent unspecified values without flagging — emit a comment in the generated token file: `// inferred default — not in source spec`.

## Output Mapping (all forms)

After extraction, build a Discovery Context object:

```json
{
  "source": "claude-design",
  "sourceFile": "<absolute path or 'pasted'>",
  "prefix": "Qzds",
  "scope": "standard",
  "tokens": {
    "color": { "primitive": {...}, "semantic": {...} },
    "typography": { ... },
    "spacing": { ... },
    "shape": { ... },
    "elevation": { ... },
    "motion": { ... }
  },
  "screens": [
    { "name": "Login", "components": [...], "layout": "..." }
  ]
}
```

Pass this to `scripts/generate-kotlin-tokens.py --config <json>` and to the Post-Generation Pipeline.

## Recreation

Each screen identified in the source becomes a `*Recreation.kt` file. See `references/source-screen-recreation.md` → "From Claude Design".

## Rules

- **Never** silently invent tokens — if a value is missing from the source, mark it as inferred in a comment and pick a defensible default.
- **Never** mix this mode with Stitch or Figma in the same run — input gate requires exactly one source.
- Treat `kta-design-spec` files as authoritative when they conflict with prose elsewhere in the conversation.
- Do not execute or render arbitrary HTML / SVG — parse-only.
