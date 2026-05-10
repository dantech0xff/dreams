# Google Stitch MCP Integration

Extract Design DNA (colors, typography, spacing, layouts) from Google Stitch screens and convert to Kotlin Compose tokens.

## What is Stitch?

Google Stitch (stitch.withgoogle.com) generates UI designs + HTML/CSS from text prompts, powered by Gemini. Stitch MCP exposes these designs to AI coding agents.

## Detection

Check for Stitch MCP tools matching `mcp__stitch__*` pattern.

**If available**: Detect mode:
- `mcp__stitch__extract_design_context` exists? → **Extended mode** (oogleyskr/stitch-mcp-server, 25 tools)
- Only `mcp__stitch__get_screen_code`? → **Basic mode** (davideast/stitch-mcp, 11 tools)

**If not**: Guide user to setup (see Setup section).

## Setup

### Quick Setup (Recommended)
```bash
npx @_davideast/stitch-mcp init
```
Wizard handles gcloud auth, OAuth, and MCP client config.

### Manual MCP Config
Add to `.mcp.json`: `{"mcpServers":{"stitch":{"command":"npx","args":["@_davideast/stitch-mcp","proxy"]}}}`
For system gcloud, add `"env":{"STITCH_USE_SYSTEM_GCLOUD":"1"}`.

**Auth**: OAuth via init (default) | `STITCH_API_KEY=<key>` env | `gcloud auth application-default login`

## Available MCP Tools

### Core Tools (davideast — both modes)

| Tool | Purpose |
|------|---------|
| `list_projects` | List all Stitch projects |
| `get_project` | Get project details |
| `list_screens` | List screens in a project |
| `get_screen` | Get screen metadata + download URLs |
| `generate_screen_from_text` | Generate new screen from text prompt |
| `edit_screens` | Modify existing screens via prompt |
| `generate_variants` | Generate design variants |

### Proxy Tools (davideast virtual layer)

| Tool | Purpose |
|------|---------|
| `get_screen_code` | Fetch screen + download HTML content |
| `get_screen_image` | Fetch screen + download screenshot (base64 PNG) |
| `build_site` | Map screens to routes, return HTML per page |
| `list_tools` | List all available tools + schemas |

### Extended Tools (oogleyskr only)

| Tool | Purpose |
|------|---------|
| `extract_design_context` | Extract Design DNA: fonts, colors, layouts |
| `apply_design_context` | Generate screens matching existing design system |
| `generate_design_tokens` | Export tokens (JSON, CSS, etc.) |
| `extract_components` | Isolate reusable elements |
| `analyze_accessibility` | WCAG 2.1 compliance checking |
| `compare_designs` | Side-by-side comparison |

## Workflow: Stitch → Kotlin Compose Tokens

### Step 1: Find the Screen

```
mcp__stitch__list_projects → pick project
mcp__stitch__list_screens(projectId) → pick screen(s)
```

For multi-screen projects: pass list to `references/screen-selection-workflow.md`.

### Step 2: Extract Design DNA

**Extended mode:**
```
mcp__stitch__extract_design_context(screenId)
```
Returns structured design data: colors, typography, spacing/layout, shapes.

**Basic mode (no extract_design_context):**
```
mcp__stitch__get_screen_code(screenId) → parse HTML/CSS manually
mcp__stitch__get_screen_image(screenId) → visual reference for analysis
```
Parse CSS to extract colors, fonts, spacing. Load `references/stitch-advanced-css-mapping.md` for mapping rules.

### Step 3: Get Visual Reference (Optional)

```
mcp__stitch__get_screen_image(screenId) → base64 PNG
```
Use for additional visual analysis (see `references/screenshot-analysis-workflow.md`).

### Step 4: Map Design DNA → Token Architecture

| Stitch Design DNA | Token Layer | Kotlin Pattern |
|-------------------|-------------|----------------|
| Colors (raw hex values) | Primitive | `object {Prefix}PrimitiveColors` |
| Font sizes, weights | Primitive | `object {Prefix}PrimitiveTypography` |
| Spacing values | Primitive | `object {Prefix}PrimitiveSpacing` |
| Corner radii | Primitive | `object {Prefix}PrimitiveShape` |
| Color roles (primary, bg, surface) | Semantic | `data class {Prefix}ColorTokens` |
| Type roles (headline, body, label) | Semantic | `data class {Prefix}TypographyTokens` |
| Layout roles (screenPadding, gap) | Semantic | `data class {Prefix}SpacingTokens` |
| Component-specific styles | Component | `data class {Prefix}{Component}Tokens` |

### Step 5: Build Token Config & Generate

```json
{
  "prefix": "Qzds",
  "package": "com.example.app.theme",
  "source": "stitch",
  "tokens": [...]
}
```

```bash
python3 scripts/generate-kotlin-tokens.py --config stitch-tokens.json --output ./generated/
```

## Advanced Patterns

**Multi-Screen Consistency**: Extract DNA from each selected screen, merge shared values into primitives, screen-specific overrides into component tokens.

**Generate + Extract Loop**: `generate_screen_from_text(prompt)` → `extract_design_context(newScreenId)` → map to tokens. Uses Stitch as visual design tool feeding token system.

**Screen Recreation**: Load `references/stitch-advanced-css-mapping.md` for CSS→Compose, `references/asset-icon-pipeline.md` for icons/images, `references/source-screen-recreation.md` for protocol.

**Combining Sources**: Stitch + Screenshot (supplement with `extract-colors-from-image.py`), Stitch + Mood (creative → generate → extract), Stitch + Figma (merge into unified token set).
