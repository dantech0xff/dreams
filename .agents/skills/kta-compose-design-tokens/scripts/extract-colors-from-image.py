#!/usr/bin/env python3
"""Extract dominant colors from PNG/JPG images and generate Kotlin Compose color primitives.

Uses Pillow to quantize image colors, sorts by luminance, assigns scale names.

Usage:
    python3 extract-colors-from-image.py --input design.png --colors 10 \
        --name Brand --prefix Qzds

    # Output as JSON (for piping to other scripts)
    python3 extract-colors-from-image.py --input logo.png --colors 8 --json
"""

import argparse
import json
import os
import sys

try:
    from PIL import Image
except ImportError:
    print("Error: Pillow not installed. Run: pip install Pillow", file=sys.stderr)
    sys.exit(1)


def extract_dominant_colors(image_path: str, num_colors: int = 10) -> list[tuple]:
    """Extract dominant colors from image using quantization."""
    img = Image.open(image_path).convert("RGB")

    # Resize for performance (preserve aspect ratio)
    img.thumbnail((200, 200))

    # Quantize to reduce to N colors
    quantized = img.quantize(colors=num_colors, method=Image.Quantize.MEDIANCUT)
    palette = quantized.getpalette()

    if palette is None:
        print("Warning: Could not extract palette from image", file=sys.stderr)
        return [(0, 0, 0)]

    # Extract RGB tuples from palette, clamped to actual palette size
    colors = []
    max_colors = min(num_colors, len(palette) // 3)
    for i in range(max_colors):
        idx = i * 3
        r, g, b = palette[idx], palette[idx + 1], palette[idx + 2]
        colors.append((r, g, b))

    return colors if colors else [(0, 0, 0)]


def rgb_to_hex(r: int, g: int, b: int) -> str:
    """Convert RGB to hex string."""
    return f"{r:02X}{g:02X}{b:02X}"


def luminance(r: int, g: int, b: int) -> float:
    """Calculate relative luminance (0=black, 1=white)."""
    return 0.299 * r + 0.587 * g + 0.114 * b


def assign_scale_names(colors: list[tuple], name: str) -> list[dict]:
    """Sort colors by luminance and assign scale names (900=darkest, 50=lightest)."""
    # Sort dark to light
    sorted_colors = sorted(colors, key=lambda c: luminance(*c))

    # Map count to scale values
    n = len(sorted_colors)
    if n <= 5:
        scales = ["900", "700", "500", "300", "50"][:n]
    elif n <= 10:
        scales = ["900", "800", "700", "600", "500", "400", "300", "200", "100", "50"][:n]
    else:
        scales = [str(900 - int(i * 850 / (n - 1))) for i in range(n)]

    result = []
    for i, (r, g, b) in enumerate(sorted_colors):
        result.append({
            "name": f"{name}{scales[i]}",
            "hex": rgb_to_hex(r, g, b),
            "rgb": (r, g, b),
            "luminance": round(luminance(r, g, b), 1),
            "scale": scales[i],
        })

    return result


def generate_kotlin_primitive(colors: list[dict], prefix: str, name: str,
                              package: str) -> str:
    """Generate Kotlin primitive color object from extracted colors."""
    lines = [
        f"package {package}",
        "",
        "import androidx.compose.ui.graphics.Color",
        "",
        f"object {prefix}Primitive{name} {{",
    ]

    for c in colors:
        lines.append(f"    val {c['name']} = Color(0xFF{c['hex']})")

    lines.append("}")
    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser(description="Extract colors from image")
    parser.add_argument("--input", required=True, help="Input image path (PNG/JPG)")
    parser.add_argument("--colors", type=int, default=10, help="Number of colors (default: 10)")
    parser.add_argument("--name", default="Extracted", help="Color group name (default: Extracted)")
    parser.add_argument("--prefix", default="Qzds", help="Token prefix (default: Qzds)")
    parser.add_argument("--package", default="com.example.app.theme",
                        help="Kotlin package name")
    parser.add_argument("--output", help="Output .kt file path (default: stdout)")
    parser.add_argument("--json", action="store_true", help="Output as JSON instead of Kotlin")
    args = parser.parse_args()

    if not os.path.exists(args.input):
        print(f"Error: Image not found: {args.input}", file=sys.stderr)
        sys.exit(1)

    # Extract colors
    raw_colors = extract_dominant_colors(args.input, args.colors)
    palette = assign_scale_names(raw_colors, args.name)

    if args.json:
        # JSON output (for piping)
        output = json.dumps([{
            "name": c["name"],
            "hex": f"#{c['hex']}",
            "luminance": c["luminance"],
            "scale": c["scale"],
        } for c in palette], indent=2)
    else:
        # Kotlin output
        output = generate_kotlin_primitive(palette, args.prefix, args.name, args.package)

    if args.output:
        os.makedirs(os.path.dirname(args.output) or ".", exist_ok=True)
        with open(args.output, "w") as f:
            f.write(output)
        print(f"Generated: {args.output} ({len(palette)} colors)")
        print("\nExtracted palette:")
        for c in palette:
            print(f"  {c['name']}: #{c['hex']} (luminance: {c['luminance']})")
    else:
        print(output)


if __name__ == "__main__":
    main()
