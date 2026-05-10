# Input Sources

## Source 1: Figma Tokens JSON

### Supported Formats
- **Figma Tokens plugin** (Tokens Studio): W3C DTCG format
- **Figma Variables API** export: JSON with `resolvedValue` fields

### W3C DTCG Format Example
```json
{
  "color": {
    "blue": {
      "500": { "$value": "#3B82F6", "$type": "color" },
      "600": { "$value": "#2563EB", "$type": "color" }
    }
  },
  "spacing": {
    "sm": { "$value": "8px", "$type": "dimension" },
    "md": { "$value": "16px", "$type": "dimension" }
  }
}
```

### Mapping Rules
| JSON `$type` | Kotlin Type | Target Layer |
|-------------|-------------|-------------|
| `color` | `Color(0xFF...)` | Primitive object |
| `dimension` | `X.dp` | Primitive object |
| `duration` | `Int` (ms) | Primitive object |
| `fontWeight` | `FontWeight.X` | Typography tokens |
| `fontSize` | `X.sp` | Typography tokens |
| `borderRadius` | `RoundedCornerShape(X.dp)` | Primitive shape |

### Script Usage
```bash
python3 parse-figma-tokens.py --input tokens.json --output ./out/ \
  --prefix Qzds --package com.example.app.theme
```

Outputs: one `.kt` file per token group (colors, spacing, shapes, etc.)

## Source 2: PNG/JPG Image

Extract dominant colors from design mockups, screenshots, or brand assets.

### How It Works
1. Image resized to 200x200 (performance)
2. Pillow `quantize()` extracts top N colors
3. Colors sorted by luminance (dark → light)
4. Scale names assigned: `{Name}900` (darkest) → `{Name}50` (lightest)

### Script Usage
```bash
python3 extract-colors-from-image.py --input mockup.png --colors 10 \
  --name "Brand" --prefix Qzds
```

### Output Example
```kotlin
object QzdsPrimitiveBrand {
    val Brand900 = Color(0xFF1A1A2E)
    val Brand800 = Color(0xFF16213E)
    val Brand700 = Color(0xFF0F3460)
    // ...
    val Brand50 = Color(0xFFE8F0FE)
}
```

### Tips
- Use screenshots of key screens for app-specific palettes
- Use brand logos/assets for brand color extraction
- Adjust `--colors` count: 5 for minimal, 10 for standard, 20 for detailed

## Source 3: Manual Definition

User describes desired tokens in natural language. Claude generates code using templates.

### Workflow
1. User specifies: colors (hex/names), spacing scale, typography choices
2. Claude loads `references/code-templates.md`
3. Claude generates complete Primitive → Semantic → Component chain
4. User reviews, adjusts naming/values

### Example Prompts
- "Create a warm earth-tone palette: terracotta #E07A5F, sage #81B29A, cream #F4F1DE"
- "Add a new spacing scale based on 8dp grid with 6 sizes"
- "Create component tokens for a new QzdsSnackbar component"

## Source 4: Sketch Export

Sketch color palettes export as JSON via plugins (Sketch2JSON, Design Tokens).

### Format
```json
{
  "colors": [
    { "name": "Primary/500", "hex": "#6200EE" },
    { "name": "Primary/700", "hex": "#3700B3" }
  ]
}
```

Same pipeline as Figma: parse JSON → generate Kotlin via `parse-figma-tokens.py` with `--format sketch` flag.
