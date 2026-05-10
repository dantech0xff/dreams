#!/usr/bin/env python3
"""Parse Figma Tokens JSON (W3C DTCG format) and generate Kotlin Compose token files.

Supports:
- Figma Tokens plugin (Tokens Studio) W3C DTCG format
- Sketch export JSON (--format sketch)
- Generic JSON color maps

Usage:
    python3 parse-figma-tokens.py --input tokens.json --output ./out/ \
        --prefix Qzds --package com.example.app.theme
"""

import argparse
import json
import os
import re
import sys


def hex_to_argb(hex_str: str) -> str:
    """Convert #RRGGBB, #AARRGGBB, or #RGB to 0xFFRRGGBB format."""
    h = hex_str.lstrip("#").upper()
    # Expand 3-char CSS shorthand (#RGB -> #RRGGBB)
    if len(h) == 3 and all(c in "0123456789ABCDEF" for c in h):
        h = h[0] * 2 + h[1] * 2 + h[2] * 2
    # Validate hex characters
    if not all(c in "0123456789ABCDEF" for c in h):
        print(f"Warning: Invalid hex value '{hex_str}', defaulting to black",
              file=sys.stderr)
        return "0xFF000000"
    if len(h) == 6:
        return f"0xFF{h}"
    if len(h) == 8:
        return f"0x{h}"
    print(f"Warning: Unexpected hex length '{hex_str}', defaulting to black",
          file=sys.stderr)
    return "0xFF000000"


def px_to_dp(value: str) -> str:
    """Convert '16px' or '16' to '16' (dp value). Preserves negative sign."""
    return re.sub(r"[^0-9.\-]", "", str(value))


def parse_dtcg_tokens(data: dict, prefix: str = "", result: dict = None) -> dict:
    """Recursively parse W3C DTCG format tokens into flat groups."""
    if result is None:
        result = {"color": {}, "dimension": {}, "duration": {}, "fontWeight": {},
                  "fontSize": {}, "borderRadius": {}, "opacity": {}}

    for key, value in data.items():
        if key.startswith("$"):
            continue

        current_path = f"{prefix}/{key}" if prefix else key

        if isinstance(value, dict):
            if "$value" in value:
                token_type = value.get("$type", "color")
                raw_value = value["$value"]
                group = token_type if token_type in result else "color"
                result.setdefault(group, {})[current_path] = raw_value
            else:
                parse_dtcg_tokens(value, current_path, result)

    return result


def parse_sketch_tokens(data: dict) -> dict:
    """Parse Sketch export JSON format."""
    result = {"color": {}}
    colors = data.get("colors", [])
    for c in colors:
        name = c.get("name", "Unknown")
        hex_val = c.get("hex", c.get("value", "#000000"))
        result["color"][name] = hex_val
    return result


def path_to_kotlin_name(path: str) -> str:
    """Convert 'color/blue/500' to 'Blue500'."""
    parts = path.split("/")
    # Take last two meaningful segments
    if len(parts) >= 2:
        name = parts[-2].capitalize() + parts[-1].capitalize()
    else:
        name = parts[-1].capitalize()
    # Clean non-alphanumeric
    return re.sub(r"[^a-zA-Z0-9]", "", name)


def generate_primitive_colors(colors: dict, prefix: str, package: str) -> str:
    """Generate Kotlin primitive color object."""
    lines = [
        f"package {package}",
        "",
        "import androidx.compose.ui.graphics.Color",
        "",
        f"object {prefix}PrimitiveImported {{",
    ]

    # Group colors by base name
    groups: dict[str, list] = {}
    for path, hex_val in sorted(colors.items()):
        kotlin_name = path_to_kotlin_name(path)
        argb = hex_to_argb(str(hex_val))
        # Extract group name (letters only)
        group = re.match(r"[A-Za-z]+", kotlin_name)
        group_name = group.group(0) if group else "Other"
        groups.setdefault(group_name, []).append((kotlin_name, argb))

    for group_name, tokens in groups.items():
        lines.append(f"    // {group_name} scale")
        for name, argb in tokens:
            lines.append(f"    val {name} = Color({argb})")
        lines.append("")

    lines.append("}")
    return "\n".join(lines)


def generate_primitive_dimensions(dimensions: dict, prefix: str, package: str,
                                  category: str = "Spacing") -> str:
    """Generate Kotlin primitive spacing/dimension object."""
    lines = [
        f"package {package}",
        "",
        "import androidx.compose.ui.unit.dp",
        "",
        f"object {prefix}PrimitiveImported{category} {{",
    ]

    for path, value in sorted(dimensions.items()):
        name = path_to_kotlin_name(path)
        dp_val = px_to_dp(value)
        lines.append(f"    val {name} = {dp_val}.dp")

    lines.append("}")
    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser(description="Parse Figma/Sketch tokens to Kotlin")
    parser.add_argument("--input", required=True, help="Input JSON file path")
    parser.add_argument("--output", default="./generated", help="Output directory")
    parser.add_argument("--prefix", default="Qzds", help="Token prefix (default: Qzds)")
    parser.add_argument("--package", default="com.example.app.theme",
                        help="Kotlin package name")
    parser.add_argument("--format", choices=["dtcg", "sketch"], default="dtcg",
                        help="Input format (default: dtcg)")
    args = parser.parse_args()

    if not os.path.exists(args.input):
        print(f"Error: Input file not found: {args.input}", file=sys.stderr)
        sys.exit(1)

    with open(args.input, "r") as f:
        data = json.load(f)

    # Parse based on format
    if args.format == "sketch":
        tokens = parse_sketch_tokens(data)
    else:
        tokens = parse_dtcg_tokens(data)

    os.makedirs(args.output, exist_ok=True)
    generated_files = []

    # Generate color primitives
    if tokens.get("color"):
        content = generate_primitive_colors(tokens["color"], args.prefix, args.package)
        filepath = os.path.join(args.output, f"{args.prefix}PrimitiveImported.kt")
        with open(filepath, "w") as f:
            f.write(content)
        generated_files.append(filepath)
        print(f"Generated: {filepath} ({len(tokens['color'])} colors)")

    # Generate dimension primitives
    if tokens.get("dimension"):
        content = generate_primitive_dimensions(
            tokens["dimension"], args.prefix, args.package)
        filepath = os.path.join(args.output, f"{args.prefix}PrimitiveImportedSpacing.kt")
        with open(filepath, "w") as f:
            f.write(content)
        generated_files.append(filepath)
        print(f"Generated: {filepath} ({len(tokens['dimension'])} dimensions)")

    # Generate border radius primitives
    if tokens.get("borderRadius"):
        content = generate_primitive_dimensions(
            tokens["borderRadius"], args.prefix, args.package, "Shape")
        filepath = os.path.join(args.output, f"{args.prefix}PrimitiveImportedShape.kt")
        with open(filepath, "w") as f:
            f.write(content)
        generated_files.append(filepath)
        print(f"Generated: {filepath} ({len(tokens['borderRadius'])} shapes)")

    if not generated_files:
        print("Warning: No tokens found in input file", file=sys.stderr)
        sys.exit(1)

    # Summary
    print(f"\nDone! {len(generated_files)} files generated in {args.output}/")
    print("Next steps:")
    print("  1. Review generated files and adjust naming")
    print("  2. Move to theme/src/main/kotlin/.../theme/")
    print(f"  3. Register in {args.prefix}Theme composable + accessor")
    print("  4. Run: ./gradlew :theme:compileDebugKotlin")


if __name__ == "__main__":
    main()
