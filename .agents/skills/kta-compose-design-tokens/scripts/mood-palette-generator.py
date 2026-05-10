#!/usr/bin/env python3
"""Generate design token palette from mood/style keywords.

Maps natural language mood descriptions to curated color palettes,
spacing scales, shape radii, and motion parameters.

Usage:
    # Generate full token config JSON
    python3 mood-palette-generator.py --mood "warm playful gaming" --prefix Qzds --json

    # Specify scope for more/fewer tokens
    python3 mood-palette-generator.py --mood "professional dark fintech" \
        --prefix App --scope standard --json

    # Save to file for piping to generate-kotlin-tokens.py
    python3 mood-palette-generator.py --mood "zen minimal meditation" \
        --prefix Qzds --output palette.json
"""

import argparse
import colorsys
import json
import os
import sys

# --- Mood keyword → design parameter mappings ---

TEMPERATURE_KEYWORDS = {
    "warm": {"hue_center": 25, "hue_range": 30, "sat_bias": 0.65, "gray_warm": True},
    "cozy": {"hue_center": 30, "hue_range": 25, "sat_bias": 0.55, "gray_warm": True},
    "earthy": {"hue_center": 35, "hue_range": 20, "sat_bias": 0.45, "gray_warm": True},
    "cool": {"hue_center": 210, "hue_range": 40, "sat_bias": 0.55, "gray_warm": False},
    "crisp": {"hue_center": 200, "hue_range": 30, "sat_bias": 0.60, "gray_warm": False},
    "professional": {"hue_center": 220, "hue_range": 30, "sat_bias": 0.50, "gray_warm": False},
    "vibrant": {"hue_center": 280, "hue_range": 60, "sat_bias": 0.80, "gray_warm": False},
    "energetic": {"hue_center": 350, "hue_range": 50, "sat_bias": 0.75, "gray_warm": True},
    "muted": {"hue_center": 180, "hue_range": 40, "sat_bias": 0.25, "gray_warm": True},
    "calm": {"hue_center": 170, "hue_range": 30, "sat_bias": 0.30, "gray_warm": False},
    "zen": {"hue_center": 160, "hue_range": 25, "sat_bias": 0.20, "gray_warm": True},
    "neutral": {"hue_center": 220, "hue_range": 20, "sat_bias": 0.10, "gray_warm": False},
}

ENERGY_KEYWORDS = {
    "minimal": {"spacing_base": 16, "radius": 12, "motion_ms": 500, "easing": "EaseInOut"},
    "zen": {"spacing_base": 16, "radius": 16, "motion_ms": 600, "easing": "EaseInOut"},
    "calm": {"spacing_base": 12, "radius": 12, "motion_ms": 400, "easing": "EaseInOut"},
    "balanced": {"spacing_base": 8, "radius": 8, "motion_ms": 250, "easing": "EaseOut"},
    "standard": {"spacing_base": 8, "radius": 8, "motion_ms": 250, "easing": "EaseOut"},
    "compact": {"spacing_base": 4, "radius": 4, "motion_ms": 150, "easing": "EaseOut"},
    "dense": {"spacing_base": 4, "radius": 4, "motion_ms": 150, "easing": "EaseOut"},
    "playful": {"spacing_base": 10, "radius": 20, "motion_ms": 400, "easing": "Spring"},
    "bouncy": {"spacing_base": 10, "radius": 24, "motion_ms": 450, "easing": "Spring"},
    "fun": {"spacing_base": 8, "radius": 16, "motion_ms": 350, "easing": "Spring"},
    "sharp": {"spacing_base": 8, "radius": 2, "motion_ms": 200, "easing": "Linear"},
    "editorial": {"spacing_base": 8, "radius": 0, "motion_ms": 200, "easing": "EaseOut"},
    "corporate": {"spacing_base": 8, "radius": 4, "motion_ms": 200, "easing": "EaseOut"},
}

CATEGORY_DEFAULTS = {
    "fintech": {"temp": "cool", "energy": "compact", "theme": "dark"},
    "banking": {"temp": "cool", "energy": "compact", "theme": "adaptive"},
    "health": {"temp": "warm", "energy": "minimal", "theme": "light"},
    "wellness": {"temp": "muted", "energy": "zen", "theme": "light"},
    "gaming": {"temp": "vibrant", "energy": "playful", "theme": "dark"},
    "quiz": {"temp": "vibrant", "energy": "fun", "theme": "dark"},
    "productivity": {"temp": "neutral", "energy": "balanced", "theme": "adaptive"},
    "social": {"temp": "warm", "energy": "balanced", "theme": "adaptive"},
    "ecommerce": {"temp": "neutral", "energy": "standard", "theme": "light"},
    "kids": {"temp": "vibrant", "energy": "playful", "theme": "light"},
    "education": {"temp": "warm", "energy": "balanced", "theme": "light"},
    "meditation": {"temp": "muted", "energy": "zen", "theme": "dark"},
    "news": {"temp": "neutral", "energy": "compact", "theme": "adaptive"},
    "editorial": {"temp": "neutral", "energy": "editorial", "theme": "adaptive"},
}


def parse_mood_keywords(mood_str: str) -> dict:
    """Parse mood string into temperature, energy, and category signals."""
    words = mood_str.lower().replace(",", " ").split()
    result = {"temp": None, "energy": None, "category": None, "theme": None, "raw": words}

    for w in words:
        if w in TEMPERATURE_KEYWORDS and not result["temp"]:
            result["temp"] = w
        if w in ENERGY_KEYWORDS and not result["energy"]:
            result["energy"] = w
        if w in CATEGORY_DEFAULTS and not result["category"]:
            result["category"] = w
        if w in ("dark", "light", "adaptive"):
            result["theme"] = w

    # Fill from category defaults if missing
    if result["category"] and result["category"] in CATEGORY_DEFAULTS:
        defaults = CATEGORY_DEFAULTS[result["category"]]
        if not result["temp"]:
            result["temp"] = defaults["temp"]
        if not result["energy"]:
            result["energy"] = defaults["energy"]
        if not result["theme"]:
            result["theme"] = defaults["theme"]

    # Final fallbacks
    result["temp"] = result["temp"] or "neutral"
    result["energy"] = result["energy"] or "balanced"
    result["theme"] = result["theme"] or "adaptive"

    return result


def hsl_to_hex(h: float, s: float, l: float) -> str:
    """Convert HSL (h=0-360, s=0-1, l=0-1) to hex string."""
    h_norm = h / 360.0
    r, g, b = colorsys.hls_to_rgb(h_norm, l, s)
    return f"{int(r*255):02X}{int(g*255):02X}{int(b*255):02X}"


def generate_color_scale(hue: float, sat: float, name: str) -> list[dict]:
    """Generate a 10-step color scale from dark (900) to light (50)."""
    steps = [
        ("900", 0.12), ("800", 0.20), ("700", 0.30), ("600", 0.40), ("500", 0.50),
        ("400", 0.60), ("300", 0.72), ("200", 0.82), ("100", 0.90), ("50", 0.96),
    ]
    return [
        {"name": f"{name}{scale}", "value": f"#{hsl_to_hex(hue, sat, lightness)}"}
        for scale, lightness in steps
    ]


def generate_neutral_scale(warm: bool) -> list[dict]:
    """Generate neutral gray scale (warm or cool tinted)."""
    hue = 30 if warm else 220
    sat = 0.08 if warm else 0.12
    base = [
        ("Black", 0.0, 0.0), ("Gray950", hue, 0.05), ("Gray900", hue, 0.10),
        ("Gray800", hue, 0.18), ("Gray700", hue, 0.30), ("Gray600", hue, 0.42),
        ("Gray500", hue, 0.55), ("Gray400", hue, 0.68), ("Gray300", hue, 0.78),
        ("Gray200", hue, 0.87), ("Gray100", hue, 0.93), ("Gray50", hue, 0.97),
        ("White", 0, 1.0),
    ]
    return [
        {"name": n, "value": f"#{hsl_to_hex(h, sat if h > 0 else 0, l)}"}
        for n, h, l in base
    ]


def generate_semantic_colors() -> list[dict]:
    """Generate fixed semantic colors (success, error, warning, info)."""
    return [
        {"name": "Success500", "value": "#22C55E"},
        {"name": "Success700", "value": "#15803D"},
        {"name": "Error500", "value": "#EF4444"},
        {"name": "Error700", "value": "#B91C1C"},
        {"name": "Warning500", "value": "#F59E0B"},
        {"name": "Warning700", "value": "#B45309"},
        {"name": "Info500", "value": "#3B82F6"},
        {"name": "Info700", "value": "#1D4ED8"},
    ]


def build_palette(mood: dict, prefix: str, package: str, scope: str) -> dict:
    """Build complete token config from parsed mood."""
    temp_cfg = TEMPERATURE_KEYWORDS.get(mood["temp"], TEMPERATURE_KEYWORDS["neutral"])
    energy_cfg = ENERGY_KEYWORDS.get(mood["energy"], ENERGY_KEYWORDS["balanced"])

    hue = temp_cfg["hue_center"]
    sat = temp_cfg["sat_bias"]

    # Generate primary color scale
    primary_colors = generate_color_scale(hue, sat, "Primary")

    # Secondary: complementary offset
    secondary_hue = (hue + 150) % 360
    secondary_colors = generate_color_scale(secondary_hue, sat * 0.8, "Secondary")

    # Tertiary: accent
    tertiary_hue = (hue + 60) % 360
    tertiary_colors = generate_color_scale(tertiary_hue, min(sat * 1.2, 1.0), "Tertiary")

    # Neutrals
    neutral_colors = generate_neutral_scale(temp_cfg["gray_warm"])

    # Semantic status colors
    semantic_status = generate_semantic_colors()

    # Combine all into primitive color values
    all_colors = primary_colors + secondary_colors + tertiary_colors + neutral_colors + semantic_status

    tokens = []

    # --- Primitive Colors ---
    tokens.append({
        "layer": "primitive",
        "name": "Colors",
        "type": "color",
        "values": all_colors,
    })

    # --- Primitive Spacing ---
    base = energy_cfg["spacing_base"]
    spacing_values = [
        {"name": "Xxxs", "value": str(max(1, base // 4))},
        {"name": "Xxs", "value": str(max(2, base // 2))},
        {"name": "Xs", "value": str(base)},
        {"name": "Sm", "value": str(int(base * 1.5))},
        {"name": "Md", "value": str(base * 2)},
        {"name": "Lg", "value": str(base * 3)},
        {"name": "Xl", "value": str(base * 4)},
        {"name": "Xxl", "value": str(base * 6)},
        {"name": "Xxxl", "value": str(base * 8)},
    ]
    tokens.append({
        "layer": "primitive",
        "name": "Spacing",
        "type": "spacing",
        "values": spacing_values,
    })

    # --- Primitive Shape ---
    r = energy_cfg["radius"]
    shape_values = [
        {"name": "None", "value": "0"},
        {"name": "Xs", "value": str(max(1, r // 4))},
        {"name": "Sm", "value": str(max(2, r // 2))},
        {"name": "Md", "value": str(r)},
        {"name": "Lg", "value": str(int(r * 1.5))},
        {"name": "Xl", "value": str(r * 2)},
        {"name": "Full", "value": "50"},
    ]
    tokens.append({
        "layer": "primitive",
        "name": "Shape",
        "type": "shape",
        "values": shape_values,
    })

    # --- Semantic Color Tokens ---
    is_dark = mood["theme"] in ("dark", "adaptive")
    has_variants = mood["theme"] == "adaptive"

    semantic_color_props = [
        {"name": "primary", "kotlin_type": "Color",
         "dark_value": f"{prefix}PrimitiveColors.Primary400",
         "light_value": f"{prefix}PrimitiveColors.Primary600",
         "default_value": f"{prefix}PrimitiveColors.Primary500"},
        {"name": "onPrimary", "kotlin_type": "Color",
         "dark_value": f"{prefix}PrimitiveColors.Black",
         "light_value": f"{prefix}PrimitiveColors.White",
         "default_value": f"{prefix}PrimitiveColors.White"},
        {"name": "secondary", "kotlin_type": "Color",
         "dark_value": f"{prefix}PrimitiveColors.Secondary400",
         "light_value": f"{prefix}PrimitiveColors.Secondary600",
         "default_value": f"{prefix}PrimitiveColors.Secondary500"},
        {"name": "background", "kotlin_type": "Color",
         "dark_value": f"{prefix}PrimitiveColors.Gray950",
         "light_value": f"{prefix}PrimitiveColors.Gray50",
         "default_value": f"{prefix}PrimitiveColors.Gray950" if is_dark else f"{prefix}PrimitiveColors.Gray50"},
        {"name": "surface", "kotlin_type": "Color",
         "dark_value": f"{prefix}PrimitiveColors.Gray900",
         "light_value": f"{prefix}PrimitiveColors.White",
         "default_value": f"{prefix}PrimitiveColors.Gray900" if is_dark else f"{prefix}PrimitiveColors.White"},
        {"name": "onSurface", "kotlin_type": "Color",
         "dark_value": f"{prefix}PrimitiveColors.Gray100",
         "light_value": f"{prefix}PrimitiveColors.Gray900",
         "default_value": f"{prefix}PrimitiveColors.Gray100" if is_dark else f"{prefix}PrimitiveColors.Gray900"},
        {"name": "error", "kotlin_type": "Color",
         "dark_value": f"{prefix}PrimitiveColors.Error500",
         "light_value": f"{prefix}PrimitiveColors.Error700",
         "default_value": f"{prefix}PrimitiveColors.Error500"},
    ]

    tokens.append({
        "layer": "semantic",
        "name": "Color",
        "has_dark_light": has_variants,
        "properties": semantic_color_props,
    })

    # --- Semantic Spacing Tokens ---
    tokens.append({
        "layer": "semantic",
        "name": "Spacing",
        "has_dark_light": False,
        "properties": [
            {"name": "screenPadding", "kotlin_type": "Dp",
             "default_value": f"{prefix}PrimitiveSpacing.Md"},
            {"name": "sectionGap", "kotlin_type": "Dp",
             "default_value": f"{prefix}PrimitiveSpacing.Lg"},
            {"name": "itemSpacing", "kotlin_type": "Dp",
             "default_value": f"{prefix}PrimitiveSpacing.Sm"},
            {"name": "componentPadding", "kotlin_type": "Dp",
             "default_value": f"{prefix}PrimitiveSpacing.Md"},
        ],
    })

    if scope in ("standard", "comprehensive"):
        # --- Primitive Motion ---
        ms = energy_cfg["motion_ms"]
        tokens.append({
            "layer": "primitive",
            "name": "Motion",
            "type": "duration",
            "values": [
                {"name": "Quick", "value": str(max(50, ms // 3))},
                {"name": "Normal", "value": str(ms)},
                {"name": "Slow", "value": str(int(ms * 1.5))},
                {"name": "Emphasis", "value": str(ms * 2)},
            ],
        })

    config = {
        "prefix": prefix,
        "package": package,
        "mood_summary": " ".join(mood["raw"]),
        "mood_parsed": {
            "temperature": mood["temp"],
            "energy": mood["energy"],
            "category": mood.get("category", "general"),
            "theme": mood["theme"],
        },
        "tokens": tokens,
    }

    return config


def main():
    parser = argparse.ArgumentParser(description="Generate token palette from mood keywords")
    parser.add_argument("--mood", required=True, help="Mood keywords (e.g., 'warm playful gaming')")
    parser.add_argument("--prefix", default="Qzds", help="Token prefix (default: Qzds)")
    parser.add_argument("--package", default="com.example.app.theme", help="Kotlin package")
    parser.add_argument("--scope", default="standard", choices=["minimal", "standard", "comprehensive"],
                        help="Token scope (default: standard)")
    parser.add_argument("--output", help="Output JSON file path (default: stdout)")
    parser.add_argument("--json", action="store_true", help="Pretty-print JSON output")

    args = parser.parse_args()

    # Parse mood
    mood = parse_mood_keywords(args.mood)

    # Generate palette
    config = build_palette(mood, args.prefix, args.package, args.scope)

    # Output
    output = json.dumps(config, indent=2) if args.json else json.dumps(config)

    if args.output:
        os.makedirs(os.path.dirname(args.output) or ".", exist_ok=True)
        with open(args.output, "w") as f:
            f.write(output)

        token_count = sum(
            len(t.get("values", [])) + len(t.get("properties", []))
            for t in config["tokens"]
        )
        print(f"Generated: {args.output}", file=sys.stderr)
        print(f"  Mood: {config['mood_summary']}", file=sys.stderr)
        print(f"  Parsed: temp={mood['temp']}, energy={mood['energy']}, theme={mood['theme']}", file=sys.stderr)
        print(f"  Tokens: {len(config['tokens'])} groups, ~{token_count} values", file=sys.stderr)
        print(f"\nNext: python3 generate-kotlin-tokens.py --config {args.output} --output ./generated/",
              file=sys.stderr)
    else:
        print(output)


if __name__ == "__main__":
    main()
