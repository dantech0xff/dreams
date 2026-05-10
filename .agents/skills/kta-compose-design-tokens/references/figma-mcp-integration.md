# Figma MCP Integration

Connect to Figma via MCP for live token extraction, or fall back to JSON import.

## Detection

Check if Figma MCP is available by looking for tools matching `mcp__figma__*` pattern.

**If available**: Use live mode (Step 1-4 below).
**If not available**: Guide user to either:
- Add Figma MCP server to `.mcp.json` (see Setup below)
- Export tokens as JSON and use `scripts/parse-figma-tokens.py` instead

## Setup (If Not Configured)

### Option A: Figma Official MCP (Recommended — Remote Server)
```json
{
  "mcpServers": {
    "figma": {
      "type": "url",
      "url": "https://mcp.figma.com/mcp"
    }
  }
}
```
Handles auth via OAuth2 — browser prompt on first use.

### Option B: Framelink Community MCP (Local)
```json
{
  "mcpServers": {
    "figma": {
      "command": "npx",
      "args": ["-y", "figma-developer-mcp", "--figma-api-key=<key>"]
    }
  }
}
```
Personal access token: Figma → Settings → Personal access tokens → Generate.

## Available Tools (Official Figma MCP)

| Tool | Purpose |
|------|---------|
| `get_design_context` | Extract design info (colors, typography, layout) for code generation |
| `get_variable_defs` | Retrieve design tokens: colors, typography, spacing variables |
| `get_metadata` | Layer tree with IDs, names, positions, sizes (XML format) |
| `get_screenshot` | Capture visual snapshot of a selection or frame |
| `search_design_system` | Search connected design libraries for components & tokens |
| `get_code_connect_map` | Map Figma nodes to code components |
| `get_code_connect_suggestions` | Auto-detect component-to-code relationships |
| `create_design_system_rules` | Generate rule files for design system context |

Remote-only tools (require `mcp.figma.com`):
- `generate_figma_design` — Convert web pages to Figma designs
- `use_figma` — General-purpose create/edit Figma objects
- `create_new_file` — Create blank Design or FigJam file

## Live Mode Workflow

### Step 1: Get File Reference

Ask user for Figma file URL. Extract file key from URL:
`https://www.figma.com/design/<FILE_KEY>/...`

### Step 2: Fetch Design Data

Use Figma MCP tools to extract token data:
1. **`get_variable_defs`** — design tokens (colors, spacing, typography) if using Figma Variables
2. **`get_design_context`** — layout info, component structure, styles
3. **`get_metadata`** — layer tree for frame structure and screen discovery
4. **`get_screenshot`** — visual reference for analysis

### Step 3: Map to Token Architecture

| Figma Concept | Token Layer | Kotlin Pattern |
|--------------|-------------|----------------|
| Color variables | Primitive | `object {Prefix}PrimitiveColors` |
| Spacing variables | Primitive | `object {Prefix}PrimitiveSpacing` |
| Border radius variables | Primitive | `object {Prefix}PrimitiveShape` |
| Color aliases (semantic names) | Semantic | `data class {Prefix}ColorTokens` |
| Typography styles | Semantic | `data class {Prefix}TypographyTokens` |
| Component-specific styles | Component | `data class {Prefix}{Component}Tokens` |

### Step 4: Generate Kotlin Files

Build config JSON from mapped data:
```json
{
  "prefix": "Qzds",
  "package": "com.example.app.theme",
  "tokens": [...]
}
```
Feed to: `scripts/generate-kotlin-tokens.py --config <json> --output ./generated/`

## Figma Variable Mapping

### Color Variables
```
Figma: primitives/blue/500 = #3B82F6
  → Kotlin: QzdsPrimitiveColors.Blue500 = Color(0xFF3B82F6)

Figma: semantic/primary = {primitives.blue.500}
  → Kotlin: QzdsColorTokens.primary = QzdsPrimitiveColors.Blue500
```

### Spacing Variables
```
Figma: spacing/sm = 8
  → Kotlin: QzdsPrimitiveSpacing.Sm = 8.dp
```

### Mode Mapping (Light/Dark)
```
Figma mode "Light": semantic/background = {primitives.white}
Figma mode "Dark": semantic/background = {primitives.gray.900}
  → Kotlin: lightQzdsColors.background / darkQzdsColors.background
```

## JSON Fallback

If no MCP, user exports via:
1. **Tokens Studio plugin**: Export → JSON (W3C DTCG format)
2. **Figma Dev Mode**: Copy variables as JSON
3. **Manual**: Copy hex values from Figma inspect panel

Then use:
```bash
python3 scripts/parse-figma-tokens.py --input exported.json \
  --output ./generated/ --prefix Qzds --format dtcg
```

See `references/input-sources.md` for format details.

## Screen Discovery via Figma

To list screens for selection (Mode 9):
1. `get_metadata(fileKey)` → parse top-level frames
2. Each top-level frame = one screen candidate
3. Pass to `references/screen-selection-workflow.md` for user selection
4. For advanced layout translation: load `references/figma-advanced-layout-mapping.md`
5. For icon/image handling: load `references/asset-icon-pipeline.md`
