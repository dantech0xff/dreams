# Source Screen Recreation

MANDATORY when tokens originate from one of the three supported sources. Recreate the actual screen(s) the user provided as production-ready Compose code using generated tokens.

## When to Apply

| Source | Apply? | What to Recreate |
|--------|--------|-----------------|
| Figma frames | YES | Each Figma frame/page as a separate screen |
| Stitch screens | YES | Each Stitch screen as a separate screen |
| Claude Design (kta-design-spec / artifact / markdown) | YES | Each spec'd screen as a separate Compose screen |
| Audit/Validate (no seed) | NO | No source screen exists |

> Sources outside this table are rejected by the input gate in `SKILL.md` — they never reach this stage.

## Protocol by Source Type

### From Claude Design

1. **Locate** the design source — one of:
   - `kta-design-spec` output: `plans/.../design/screen-*.md` (structured per-screen markdown)
   - Claude HTML/SVG artifact pasted or saved to disk
   - Pasted design spec markdown block in the conversation
2. **Parse** the source — extract:
   - Screen name (from spec heading or artifact title)
   - Layout structure (sections, top bar, bottom bar, FAB notes)
   - Component list (CTA buttons, cards, inputs, lists)
   - Inline style values (colors, font sizes, spacing, radii) — map straight to generated tokens
3. **Decompose** into composable tree (Scaffold / Column / LazyColumn etc.) using parsed structure
4. **Implement** as a standalone `@Composable` function — pure UI, no ViewModel
5. **Name** the file: `{Prefix}{ScreenName}Recreation.kt` (PascalCase from spec heading)

### From Figma

1. **Fetch frames** via MCP: `mcp__figma__get_file` or page-level fetch
2. **For each frame/page**, extract:
   - Frame name → screen name
   - Layer hierarchy → composable tree
   - Auto layout → Column/Row with spacing
   - Component instances → map to token-backed components
   - Constraints → Modifier alignment/sizing
3. **Map Figma concepts to Compose**:

   | Figma | Compose |
   |-------|---------|
   | Frame with auto layout (vertical) | `Column(verticalArrangement = ...)` |
   | Frame with auto layout (horizontal) | `Row(horizontalArrangement = ...)` |
   | Fixed size frame | `Box(modifier = Modifier.size(w, h))` |
   | Fill container | `Modifier.fillMaxWidth()` / `fillMaxHeight()` |
   | Hug contents | `Modifier.wrapContentSize()` |
   | Padding | `Modifier.padding(...)` using spacing tokens |
   | Corner radius | Shape tokens |
   | Fill color | Color tokens |
   | Text layer | `{Prefix}Text(style = ..., color = ...)` |
   | Rectangle | `Box(modifier = Modifier.background(...))` |
   | Component instance | Matching token-backed component |

4. **Name** each file: `{Prefix}{FigmaFrameName}Recreation.kt`

### From Stitch

1. **Fetch screen code**: `mcp__stitch__get_screen_code(screenId)`
2. **Fetch screen image**: `mcp__stitch__get_screen_image(screenId)` for visual reference
3. **Translate HTML/CSS → Compose**:

   | HTML/CSS | Compose |
   |----------|---------|
   | `<div style="display: flex; flex-direction: column">` | `Column` |
   | `<div style="display: flex; flex-direction: row">` | `Row` |
   | `<div style="display: grid">` | `LazyVerticalGrid` or nested Row/Column |
   | `padding: 16px` | `Modifier.padding(spacing tokens)` |
   | `border-radius: 12px` | Shape tokens |
   | `background-color: #xxx` | Color tokens |
   | `font-size: 16px; font-weight: 600` | Typography tokens |
   | `<button>` | `{Prefix}FilledButton` |
   | `<input>` | `{Prefix}TextField` |
   | `<img>` | `Image` / `AsyncImage` with placeholder |
   | `box-shadow` | Elevation tokens |

4. **Name** each file: `{Prefix}{StitchScreenName}Recreation.kt`

## Multi-Screen Handling

When source has multiple screens:
- Create one `.kt` file per screen
- Each is a standalone `@Composable` (no navigation between them)
- All placed in `{ui-module}/.../component/showcase/` package
- Each has its own `@Preview` functions (light + dark)

## Code Pattern

```kotlin
/**
 * Recreation of {source description} using {Prefix} design tokens.
 * Source: {Figma frame name | Stitch screen ID | Claude Design spec path}
 */
@Composable
fun {Prefix}{ScreenName}Recreation(
    modifier: Modifier = Modifier,
) {
    // Use Scaffold if source has top bar / bottom bar
    // Use Column/LazyColumn for scrollable content
    // Use ONLY token-backed components + {Prefix}Theme.* accessors
    // Zero hardcoded colors, spacing, typography, shapes
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun {ScreenName}RecreationLight() {
    {Prefix}Theme(darkTheme = false) {
        {Prefix}{ScreenName}Recreation()
    }
}

@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun {ScreenName}RecreationDark() {
    {Prefix}Theme(darkTheme = true) {
        {Prefix}{ScreenName}Recreation()
    }
}
```

## Rules

- **Pure UI only** — no ViewModel, no NavController, no side effects
- **Placeholder data** — use realistic sample text/numbers, not "Lorem ipsum"
- **Token-only** — every visual property from `{Prefix}Theme.*`, zero hardcoded
- **Use created components** — prefer `{Prefix}FilledButton` over raw `Button`
- **Images** — use `painterResource` with a placeholder drawable, or `Box` with background color
- **One file per screen** — don't combine multiple screens into one file

## Checklist

1. [ ] Each source screen has a corresponding `*Recreation.kt` file
2. [ ] Layout matches source (column/row structure, spacing, alignment)
3. [ ] All visual props from tokens — zero hardcoded values
4. [ ] Uses token-backed components where applicable
5. [ ] Realistic placeholder data
6. [ ] Light + dark `@Preview` functions
7. [ ] Compiles: `./gradlew :{ui-module}:compileDebugKotlin`
