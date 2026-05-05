# Screenshot Analysis Workflow

Extract a complete design language from a screenshot — not just colors.

## How to Use

1. User provides screenshot/mockup image path
2. Run color extraction script for precise hex values
3. Use Claude's vision (Read tool on image) for everything else
4. Combine into a Discovery Context
5. Generate tokens from combined analysis

## Step 1: Color Extraction (Script)

```bash
python3 scripts/extract-colors-from-image.py --input <image> --colors 10 --json
```

This gives precise hex values sorted by luminance. Use as the color foundation.

## Step 2: Visual Analysis (Claude Vision)

After reading the image with the `Read` tool, analyze these aspects systematically:

### 2a: Typography Hierarchy

Describe what you see:
- **Headline**: Approximate size (large/medium/small), weight (bold/medium/light), serif or sans-serif?
- **Body**: Size relative to headline, weight, line spacing (tight/normal/relaxed)
- **Caption/Label**: Smallest text size, weight
- **Count distinct type sizes**: How many levels in the hierarchy? (3=minimal, 5=standard, 7+=comprehensive)

Map to tokens:
```
headlineLarge → largest text observed
titleMedium → section headers
bodyLarge → primary content text
labelSmall → captions, metadata
```

### 2b: Spacing Rhythm

Look for:
- **Screen edge padding**: Distance from content to screen edges (estimate: 8/12/16/20/24dp)
- **Section gaps**: Space between major content blocks
- **Item spacing**: Space between list items or cards
- **Internal padding**: Padding within cards/containers
- **Base unit**: What multiple do spacings follow? (4dp grid? 8dp grid?)

Map to tokens:
```
screenPadding → edge padding observed
sectionGap → between major blocks
itemSpacing → between repeated elements
componentPadding → inside containers
```

### 2c: Shape Language

Look for:
- **Corner radius**: Sharp (0-2dp), subtle (4-6dp), medium (8-12dp), rounded (16-24dp), pill (50%)?
- **Consistency**: Same radius everywhere or varied by component?
- **Card shapes**: Specific radius on card-like elements
- **Button shapes**: Rounded pill vs subtle radius vs sharp

Map to tokens:
```
None → 0dp (sharp edges)
Xs → 4dp (subtle)
Sm → 8dp (medium)
Md → 12dp (standard)
Lg → 16dp (rounded)
Xl → 24dp (very rounded)
Full → 50% (pill/circle)
```

### 2d: Elevation & Depth

Look for:
- **Shadow presence**: No shadows (flat), subtle shadows, pronounced shadows?
- **Layering**: How many depth levels visible? (1=flat, 2-3=standard, 4+=layered)
- **Shadow style**: Soft/diffuse vs sharp/defined

Map to tokens:
```
none → 0dp (flat elements)
low → 1-2dp (subtle lift)
medium → 4-6dp (cards, dialogs)
high → 8-12dp (floating elements)
```

### 2e: Overall Mood Assessment

Based on the visual, classify:
- **Temperature**: warm / cool / neutral
- **Energy**: calm / balanced / energetic
- **Personality**: professional / friendly / playful / elegant
- **Density**: spacious / balanced / compact

## Step 3: Synthesize Discovery Context

Combine script output + visual analysis into:

```
## Screenshot Analysis Context
- colors: [from script JSON output]
- typography_levels: 3|5|7
- type_character: geometric-sans|rounded-sans|serif|monospace
- spacing_base: 4dp|8dp|12dp|16dp
- corner_radius_style: sharp|subtle|medium|rounded|pill
- elevation_style: flat|subtle|pronounced
- mood: [temperature] [energy] [personality]
- theme: dark|light
```

## Step 4: Generate Tokens

Use the synthesized context to:
1. Load matching references from `defaults/token-scope-presets.md`
2. Create token config JSON matching `generate-kotlin-tokens.py` format
3. Generate Primitive → Semantic → Component chain
4. Present to user for review before writing to project

## Tips

- **Multiple screenshots**: Analyze 2-3 screens for consistency; note any variations
- **Design mockups vs live apps**: Mockups have cleaner spacing; live screenshots may have system UI noise — crop to app content area
- **Low resolution**: If text is hard to read, focus on relative sizes rather than absolute values
- **Dark mode screenshots**: Note if this is a dark theme — generate dark-first tokens
