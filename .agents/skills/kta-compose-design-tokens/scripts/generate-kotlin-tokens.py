#!/usr/bin/env python3
"""Generate Kotlin Compose token files from intermediate JSON config.

Generates all 3 layers: Primitive object, Semantic data class, Component data class.
Includes CompositionLocal + default/dark/light instances.

Usage:
    # From JSON config
    python3 generate-kotlin-tokens.py --config tokens-config.json --output ./generated/

    # Quick primitive color object
    python3 generate-kotlin-tokens.py --layer primitive --type color \
        --name Brand --values "Red500=#E53E3E,Red600=#C53030" --output ./generated/

    # Quick semantic token
    python3 generate-kotlin-tokens.py --layer semantic --name Spacing \
        --props "componentPadding:Dp,sectionGap:Dp" --output ./generated/
"""

import argparse
import json
import os
import re
import sys
from typing import Any

KOTLIN_RESERVED = {
    "val", "var", "class", "object", "fun", "when", "if", "else", "for",
    "while", "do", "return", "break", "continue", "is", "in", "as", "null",
    "true", "false", "this", "super", "package", "import", "interface",
    "abstract", "enum", "sealed", "data", "companion", "operator", "inline",
    "suspend", "override", "open", "internal", "private", "protected", "public",
}


def sanitize_kotlin_name(name: str, pascal_case: bool = True) -> str:
    """Sanitize a string for use as a Kotlin identifier."""
    # Strip non-alphanumeric (keep underscores)
    cleaned = re.sub(r"[^a-zA-Z0-9_]", "", name)
    if not cleaned:
        cleaned = "Unknown"
    # Ensure starts with letter
    if cleaned[0].isdigit():
        cleaned = "T" + cleaned
    # PascalCase if requested
    if pascal_case and cleaned[0].islower():
        cleaned = cleaned[0].upper() + cleaned[1:]
    # Escape Kotlin reserved words
    if cleaned.lower() in KOTLIN_RESERVED:
        cleaned = cleaned + "_"
    return cleaned


# --- Kotlin type mappings ---
TYPE_IMPORTS = {
    "Color": "import androidx.compose.ui.graphics.Color",
    "Dp": "import androidx.compose.ui.unit.Dp",
    "Shape": "import androidx.compose.ui.graphics.Shape",
    "TextStyle": "import androidx.compose.ui.text.TextStyle",
    "FontWeight": "import androidx.compose.ui.text.font.FontWeight",
    "TextUnit": "import androidx.compose.ui.unit.TextUnit",
    "Int": "",
    "Float": "",
}

ALWAYS_IMPORTS = [
    "import androidx.compose.runtime.Immutable",
    "import androidx.compose.runtime.staticCompositionLocalOf",
]


def collect_imports(props: list[dict], layer: str) -> list[str]:
    """Collect unique imports needed for property types."""
    imports = set()
    if layer != "primitive":
        for imp in ALWAYS_IMPORTS:
            imports.add(imp)

    for prop in props:
        kt_type = prop.get("kotlin_type", "Color")
        imp = TYPE_IMPORTS.get(kt_type, "")
        if imp:
            imports.add(imp)

    return sorted(imports)


def generate_primitive(config: dict) -> str:
    """Generate Kotlin primitive object."""
    prefix = config.get("prefix", "Qzds")
    package = config.get("package", "com.example.app.theme")
    name = config["name"]
    values = config.get("values", [])
    token_type = config.get("type", "color")

    lines = [f"package {package}", ""]

    if token_type == "color":
        lines.append("import androidx.compose.ui.graphics.Color")
    elif token_type in ("spacing", "dimension", "size"):
        lines.append("import androidx.compose.ui.unit.dp")
    elif token_type == "shape":
        lines.append("import androidx.compose.foundation.shape.RoundedCornerShape")
        lines.append("import androidx.compose.ui.unit.dp")

    name = sanitize_kotlin_name(name)
    lines.extend(["", f"object {prefix}Primitive{name} {{"])

    for val in values:
        prop_name = sanitize_kotlin_name(val["name"])
        if token_type == "color":
            hex_val = val["value"].lstrip("#").upper()
            if len(hex_val) == 6:
                hex_val = f"FF{hex_val}"
            lines.append(f"    val {prop_name} = Color(0x{hex_val})")
        elif token_type in ("spacing", "dimension", "size"):
            lines.append(f"    val {prop_name} = {val['value']}.dp")
        elif token_type == "shape":
            lines.append(f"    val {prop_name} = RoundedCornerShape({val['value']}.dp)")
        elif token_type == "opacity":
            lines.append(f"    val {prop_name} = {val['value']}f")
        elif token_type == "duration":
            duration_val = re.sub(r"[^0-9]", "", str(val["value"]))
            lines.append(f"    val {prop_name} = {duration_val}")
        else:
            lines.append(f"    val {prop_name} = {val['value']}")

    lines.append("}")
    return "\n".join(lines)


def generate_semantic(config: dict) -> str:
    """Generate Kotlin semantic @Immutable data class with defaults + CompositionLocal."""
    prefix = config.get("prefix", "Qzds")
    package = config.get("package", "com.example.app.theme")
    name = config["name"]
    props = config.get("properties", [])
    has_variants = config.get("has_dark_light", False)

    imports = collect_imports(props, "semantic")
    lines = [f"package {package}", ""]
    lines.extend(imports)
    lines.append("")

    # Data class
    lines.append("@Immutable")
    lines.append(f"data class {prefix}{name}Tokens(")
    for prop in props:
        lines.append(f"    val {prop['name']}: {prop['kotlin_type']},")
    lines.append(")")

    # Instances
    if has_variants:
        for variant in ["dark", "light"]:
            lines.append("")
            lines.append(f"val {variant}{prefix}{name} = {prefix}{name}Tokens(")
            for prop in props:
                val = prop.get(f"{variant}_value", prop.get("default_value", ""))
                val = val or 'TODO("set value")'
                lines.append(f"    {prop['name']} = {val},")
            lines.append(")")
    else:
        lines.append("")
        lines.append(f"val default{prefix}{name} = {prefix}{name}Tokens(")
        for prop in props:
            val = prop.get("default_value", "") or 'TODO("set value")'
            lines.append(f"    {prop['name']} = {val},")
        lines.append(")")

    # CompositionLocal
    lines.append("")
    default_ref = f"dark{prefix}{name}" if has_variants else f"default{prefix}{name}"
    lines.append(
        f"val Local{prefix}{name} = staticCompositionLocalOf {{ {default_ref} }}"
    )

    return "\n".join(lines)


def generate_component(config: dict) -> str:
    """Generate Kotlin component @Immutable data class with defaults + CompositionLocal."""
    prefix = config.get("prefix", "Qzds")
    package = config.get("package", "com.example.app.theme")
    name = config["name"]
    props = config.get("properties", [])

    imports = collect_imports(props, "component")
    lines = [f"package {package}", ""]
    lines.extend(imports)
    lines.append("")

    # Data class
    lines.append("@Immutable")
    lines.append(f"data class {prefix}{name}Tokens(")
    for prop in props:
        lines.append(f"    val {prop['name']}: {prop['kotlin_type']},")
    lines.append(")")

    # Default instance
    lines.append("")
    lines.append(f"val default{prefix}{name} = {prefix}{name}Tokens(")
    for prop in props:
        val = prop.get("default_value", "") or 'TODO("set value")'
        lines.append(f"    {prop['name']} = {val},")
    lines.append(")")

    # CompositionLocal
    lines.append("")
    lines.append(
        f"val Local{prefix}{name} = staticCompositionLocalOf {{ default{prefix}{name} }}"
    )

    return "\n".join(lines)


def generate_theme_snippet(tokens: list[dict], prefix: str) -> str:
    """Generate QzdsTheme registration snippet (for manual insertion)."""
    # Filter out primitives — they don't need CompositionLocal/Theme registration
    registrable = [t for t in tokens if t.get("layer") != "primitive"]

    if not registrable:
        return "// No semantic/component tokens to register."

    lines = [
        f"// Add to {prefix}Theme composable's CompositionLocalProvider:",
    ]

    for token in registrable:
        name = token["name"]
        has_variants = token.get("has_dark_light", False)

        if has_variants:
            lines.append(
                f"Local{prefix}{name} provides if (darkTheme) dark{prefix}{name} "
                f"else light{prefix}{name},"
            )
        else:
            lines.append(f"Local{prefix}{name} provides default{prefix}{name},")

    lines.extend([
        "",
        f"// Add to {prefix}Theme object:",
    ])

    for token in registrable:
        name = token["name"]
        accessor = name[0].lower() + name[1:]
        lines.append(f"val {accessor}: {prefix}{name}Tokens")
        lines.append(f"    @Composable get() = Local{prefix}{name}.current")

    return "\n".join(lines)


def process_config(config: dict, output_dir: str) -> list[str]:
    """Process a full config JSON and generate all token files."""
    prefix = config.get("prefix", "Qzds")
    package = config.get("package", "com.example.app.theme")
    tokens = config.get("tokens", [])
    generated = []

    for token in tokens:
        token.setdefault("prefix", prefix)
        token.setdefault("package", package)
        layer = token.get("layer", "semantic")

        if layer == "primitive":
            content = generate_primitive(token)
            filename = f"{prefix}Primitive{token['name']}.kt"
        elif layer == "semantic":
            content = generate_semantic(token)
            filename = f"{prefix}{token['name']}Tokens.kt"
        elif layer == "component":
            content = generate_component(token)
            filename = f"{prefix}{token['name']}Tokens.kt"
        else:
            print(f"Warning: Unknown layer '{layer}' for token '{token['name']}'",
                  file=sys.stderr)
            continue

        filepath = os.path.join(output_dir, filename)
        with open(filepath, "w") as f:
            f.write(content)
        generated.append(filepath)
        print(f"Generated: {filepath}")

    # Generate theme registration snippet
    if tokens:
        snippet = generate_theme_snippet(tokens, prefix)
        snippet_path = os.path.join(output_dir, "_theme-registration-snippet.txt")
        with open(snippet_path, "w") as f:
            f.write(snippet)
        generated.append(snippet_path)
        print(f"Generated: {snippet_path}")

    return generated


def parse_inline_values(values_str: str) -> list[dict]:
    """Parse 'Name1=#HEX1,Name2=#HEX2' into list of dicts."""
    result = []
    for pair in values_str.split(","):
        pair = pair.strip()
        if "=" in pair:
            name, value = pair.split("=", 1)
            result.append({"name": name.strip(), "value": value.strip()})
    return result


def parse_inline_props(props_str: str) -> list[dict]:
    """Parse 'propName:Type,propName2:Type' into list of dicts."""
    result = []
    for pair in props_str.split(","):
        pair = pair.strip()
        if ":" in pair:
            name, kt_type = pair.split(":", 1)
            result.append({
                "name": name.strip(),
                "kotlin_type": kt_type.strip(),
                "default_value": "",
            })
    return result


def main():
    parser = argparse.ArgumentParser(
        description="Generate Kotlin Compose token files"
    )
    parser.add_argument("--config", help="JSON config file with token definitions")
    parser.add_argument("--output", default="./generated", help="Output directory")
    parser.add_argument("--prefix", default="Qzds", help="Token prefix")
    parser.add_argument("--package", default="com.example.app.theme",
                        help="Kotlin package")

    # Inline mode (quick single-token generation)
    parser.add_argument("--layer", choices=["primitive", "semantic", "component"],
                        help="Token layer (inline mode)")
    parser.add_argument("--type",
                        choices=["color", "spacing", "dimension", "shape",
                                 "opacity", "duration", "size"],
                        help="Primitive type (inline mode)")
    parser.add_argument("--name", help="Token name (inline mode)")
    parser.add_argument("--values", help="Comma-separated Name=Value pairs (primitive)")
    parser.add_argument("--props",
                        help="Comma-separated name:Type pairs (semantic/component)")
    args = parser.parse_args()

    os.makedirs(args.output, exist_ok=True)

    if args.config:
        # Config file mode
        if not os.path.exists(args.config):
            print(f"Error: Config not found: {args.config}", file=sys.stderr)
            sys.exit(1)

        with open(args.config, "r") as f:
            config = json.load(f)

        config.setdefault("prefix", args.prefix)
        config.setdefault("package", args.package)
        generated = process_config(config, args.output)

    elif args.layer and args.name:
        # Inline mode
        token = {
            "prefix": args.prefix,
            "package": args.package,
            "name": args.name,
            "layer": args.layer,
        }

        if args.layer == "primitive":
            token["type"] = args.type or "color"
            token["values"] = parse_inline_values(args.values or "")
            content = generate_primitive(token)
            filename = f"{args.prefix}Primitive{args.name}.kt"
        elif args.layer == "semantic":
            token["properties"] = parse_inline_props(args.props or "")
            content = generate_semantic(token)
            filename = f"{args.prefix}{args.name}Tokens.kt"
        elif args.layer == "component":
            token["properties"] = parse_inline_props(args.props or "")
            content = generate_component(token)
            filename = f"{args.prefix}{args.name}Tokens.kt"
        else:
            print("Error: Specify --config or --layer + --name", file=sys.stderr)
            sys.exit(1)

        filepath = os.path.join(args.output, filename)
        with open(filepath, "w") as f:
            f.write(content)
        print(f"Generated: {filepath}")
        generated = [filepath]

    else:
        parser.print_help()
        sys.exit(1)

    print(f"\nDone! {len(generated)} files generated in {args.output}/")
    print("Next steps:")
    print("  1. Review generated files and adjust values")
    print("  2. Move to theme/src/main/kotlin/.../theme/")
    print(f"  3. Apply _theme-registration-snippet.txt to {args.prefix}Theme")
    print("  4. Run: ./gradlew :theme:compileDebugKotlin")


if __name__ == "__main__":
    main()
