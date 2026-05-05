# Mood to Tokens

Map natural language mood/style descriptions to concrete design token decisions.

## How to Use

1. Extract mood keywords from user input
2. Match keywords to dimensions below
3. Combine dimensions into a coherent token config
4. Run `scripts/mood-palette-generator.py` for palette generation
5. Use output as input to `scripts/generate-kotlin-tokens.py`

## Mood Dimensions

### Temperature (Color Warmth)

| Keyword | Primary Hue Range | Accent Strategy |
|---------|-------------------|-----------------|
| warm, cozy, earthy | Orange 20°-50°, Amber, Terracotta | Complementary cool accent (teal, sage) |
| cool, crisp, professional | Blue 200°-240°, Slate, Steel | Warm accent (amber, coral) |
| neutral, balanced | Gray-based, Taupe, Stone | Either warm or cool accent |
| vibrant, energetic | High saturation any hue | Split-complementary |
| muted, calm, zen | Desaturated pastels, Earth tones | Analogous, low contrast |

### Energy (Spacing & Motion)

| Keyword | Spacing Base | Corner Radius | Motion Duration | Easing |
|---------|-------------|---------------|-----------------|--------|
| minimal, zen, calm | 16dp (spacious) | 8-16dp (soft) | 400-600ms (slow) | EaseInOut |
| balanced, standard | 8dp (standard) | 8-12dp (medium) | 200-300ms (normal) | EaseOut |
| compact, dense, data-heavy | 4dp (tight) | 4-8dp (subtle) | 150-200ms (snappy) | EaseOut |
| playful, bouncy, fun | 8-12dp (generous) | 16-24dp (very round) | 300-500ms + overshoot | Spring |
| sharp, editorial, corporate | 8dp | 0-4dp (angular) | 200-250ms (precise) | Linear/EaseOut |

### Personality (Typography & Elevation)

| Keyword | Type Character | Weight Range | Elevation Strategy |
|---------|---------------|-------------|-------------------|
| professional, corporate | Geometric sans (Inter, Roboto) | 400-700 | Subtle (1-3dp) |
| playful, fun, kids | Rounded sans (Nunito, Quicksand) | 500-800 | Pronounced (2-8dp) |
| luxury, premium, elegant | Serif or thin sans (Playfair, Lato Light) | 300-600 | Minimal (0-2dp) |
| technical, developer | Monospace hybrid (JetBrains Mono, Fira Code) | 400-500 | Flat (0-1dp) |
| editorial, magazine | High contrast serif (Merriweather) | 300-900 | Medium (1-4dp) |
| friendly, approachable | Humanist sans (Open Sans, Source Sans) | 400-700 | Moderate (2-4dp) |

### Theme (Dark/Light Preference)

| Keyword | Theme Strategy | Surface Colors |
|---------|---------------|----------------|
| dark, night, moody | Dark primary, light accent surfaces | Gray900-800 bases |
| light, airy, bright | Light primary, subtle shadows | White/Gray50-100 bases |
| high-contrast | Either dark or light with strong foreground/background contrast | Max contrast pairs |
| adaptive | Both dark and light variants | Full dual-theme |

## App Category Defaults

Starting points — user keywords override these.

| Category | Temperature | Energy | Personality | Theme |
|----------|-------------|--------|-------------|-------|
| Fintech/Banking | cool | compact | professional | dark or adaptive |
| Health/Wellness | warm/muted | minimal | friendly | light |
| Gaming/Quiz | vibrant | playful | fun | dark |
| Productivity | neutral | balanced | professional | adaptive |
| Social | warm | balanced | friendly | adaptive |
| E-commerce | neutral | standard | approachable | light |
| Kids/Education | vibrant | playful | fun/playful | light |
| Meditation/Zen | muted | minimal | elegant | dark |
| News/Editorial | neutral | compact | editorial | adaptive |

## Color Psychology Quick Reference

| Color Family | Associations | Good For |
|-------------|-------------|----------|
| Blue | Trust, stability, calm | Finance, health, corporate |
| Green | Growth, nature, success | Health, finance (profit), eco |
| Red/Coral | Energy, urgency, passion | Gaming, food, sales CTAs |
| Orange/Amber | Warmth, creativity, fun | Social, kids, creative tools |
| Purple | Luxury, creativity, wisdom | Premium, creative, wellness |
| Teal/Cyan | Modern, fresh, tech | SaaS, tech, productivity |
| Pink | Playful, caring, youthful | Social, kids, lifestyle |
| Earth tones | Organic, reliable, calm | Wellness, eco, artisanal |

## Palette Generation Rules

1. **Primary**: Derived from temperature + category defaults
2. **Secondary**: Complementary or analogous to primary (based on energy)
3. **Tertiary**: Accent for CTAs and highlights
4. **Neutral**: Gray scale matching temperature (warm gray vs cool gray)
5. **Semantic**: Success=green, Error=red, Warning=amber, Info=blue (adjust saturation to match mood)
6. **Surface**: Derived from theme preference + temperature

### Scale Generation

For each color family, generate a 10-step scale (50-900):
- **50**: Tinted almost-white (backgrounds)
- **100-200**: Light tints (hover states, subtle fills)
- **300-400**: Mid tones (disabled states, borders)
- **500**: Base value (primary usage)
- **600-700**: Darker (pressed states, emphasis)
- **800-900**: Deepest (text on light, dark surfaces)

## Output Format

Script outputs JSON compatible with `generate-kotlin-tokens.py --config`:

```json
{
  "prefix": "Qzds",
  "package": "com.example.app.theme",
  "mood_summary": "warm playful gaming",
  "tokens": [
    {
      "layer": "primitive",
      "name": "Colors",
      "type": "color",
      "values": [
        {"name": "Primary500", "value": "#E07A5F"},
        {"name": "Primary600", "value": "#C4694F"}
      ]
    },
    {
      "layer": "semantic",
      "name": "Color",
      "has_dark_light": true,
      "properties": [
        {"name": "primary", "kotlin_type": "Color", "dark_value": "...", "light_value": "..."}
      ]
    }
  ]
}
```
