#!/usr/bin/env python3
"""Audit Kotlin Compose design tokens for consistency, coverage, and naming.

Checks:
- Naming convention compliance (prefix, casing)
- Orphan primitives (not referenced by any semantic token)
- Missing dark/light variants for color tokens
- CompositionLocal + {Prefix}Theme registration
- Token inventory summary

Usage:
    python3 audit-compose-tokens.py --dir theme/src/main/kotlin/com/example/app/theme/
    python3 audit-compose-tokens.py --dir theme/src/main/kotlin/com/example/app/theme/ --prefix Qzds
"""

import argparse
import os
import re
import sys
from collections import defaultdict


def find_kotlin_files(directory: str) -> list[str]:
    """Find all .kt files in directory."""
    kt_files = []
    for root, _, files in os.walk(directory):
        for f in files:
            if f.endswith(".kt"):
                kt_files.append(os.path.join(root, f))
    return sorted(kt_files)


def extract_primitives(content: str, prefix: str) -> list[str]:
    """Extract primitive token names from object declarations."""
    primitives = []
    # Match: val Name = Color/dp/etc
    pattern = rf"object\s+{prefix}Primitive\w+\s*\{{(.*?)\}}"
    for match in re.finditer(pattern, content, re.DOTALL):
        body = match.group(1)
        for val_match in re.finditer(r"val\s+(\w+)\s*=", body):
            primitives.append(val_match.group(1))
    return primitives


def extract_data_classes(content: str, prefix: str) -> list[dict]:
    """Extract semantic/component token data classes."""
    classes = []
    pattern = rf"data\s+class\s+({prefix}\w+Tokens)\s*\((.*?)\)"
    for match in re.finditer(pattern, content, re.DOTALL):
        class_name = match.group(1)
        body = match.group(2)
        props = re.findall(r"val\s+(\w+)\s*:", body)
        classes.append({"name": class_name, "properties": props})
    return classes


def check_dark_light_variants(content: str, prefix: str) -> dict:
    """Check for dark/light variant instances."""
    variants = {}
    dark_pattern = rf"val\s+(dark{prefix}\w+)\s*="
    light_pattern = rf"val\s+(light{prefix}\w+)\s*="
    default_pattern = rf"val\s+(default{prefix}\w+)\s*="

    for m in re.finditer(dark_pattern, content):
        base = m.group(1).replace("dark", "")
        variants.setdefault(base, set()).add("dark")

    for m in re.finditer(light_pattern, content):
        base = m.group(1).replace("light", "")
        variants.setdefault(base, set()).add("light")

    for m in re.finditer(default_pattern, content):
        base = m.group(1).replace("default", "")
        variants.setdefault(base, set()).add("default")

    return variants


def check_composition_locals(content: str, prefix: str) -> list[str]:
    """Find registered CompositionLocals."""
    pattern = rf"val\s+(Local{prefix}\w+)\s*="
    return re.findall(pattern, content)


def check_theme_accessors(content: str, prefix: str) -> list[str]:
    """Find {prefix}Theme object accessors."""
    pattern = rf"val\s+(\w+)\s*:\s*{prefix}\w+Tokens"
    return re.findall(pattern, content)


def find_primitive_references(content: str, prefix: str) -> set[str]:
    """Find all references to primitive token values."""
    pattern = rf"{prefix}Primitive\w+\.(\w+)"
    return set(re.findall(pattern, content))


def main():
    parser = argparse.ArgumentParser(description="Audit Compose design tokens")
    parser.add_argument("--dir", required=True, help="Theme source directory")
    parser.add_argument("--prefix", default="Qzds", help="Token prefix (default: Qzds)")
    args = parser.parse_args()

    if not os.path.isdir(args.dir):
        print(f"Error: Directory not found: {args.dir}", file=sys.stderr)
        sys.exit(1)

    kt_files = find_kotlin_files(args.dir)
    if not kt_files:
        print(f"Error: No .kt files found in {args.dir}", file=sys.stderr)
        sys.exit(1)

    # Read all content
    all_content = ""
    file_contents = {}
    for f in kt_files:
        with open(f, "r") as fh:
            content = fh.read()
            all_content += content + "\n"
            file_contents[os.path.basename(f)] = content

    prefix = args.prefix
    issues = []
    warnings = []

    # 1. Extract primitives
    all_primitives = extract_primitives(all_content, prefix)
    print(f"## Token Audit Report\n")
    print(f"**Prefix:** {prefix}")
    print(f"**Files scanned:** {len(kt_files)}")
    print(f"**Primitive tokens:** {len(all_primitives)}")

    # 2. Extract data classes
    all_classes = extract_data_classes(all_content, prefix)
    print(f"**Token classes:** {len(all_classes)}")
    for cls in all_classes:
        print(f"  - {cls['name']}: {len(cls['properties'])} properties")

    # 3. Check dark/light variants
    variants = check_dark_light_variants(all_content, prefix)
    print(f"\n### Variant Coverage")
    for base, modes in sorted(variants.items()):
        if "Color" in base and "dark" in modes and "light" not in modes:
            issues.append(f"Missing light variant for {base}")
            print(f"  {base}: {', '.join(modes)} (MISSING light)")
        elif "Color" in base and "light" in modes and "dark" not in modes:
            issues.append(f"Missing dark variant for {base}")
            print(f"  {base}: {', '.join(modes)} (MISSING dark)")
        else:
            print(f"  {base}: {', '.join(modes)}")

    # 4. Check CompositionLocals
    locals_found = check_composition_locals(all_content, prefix)
    print(f"\n### CompositionLocals: {len(locals_found)}")
    for local in sorted(locals_found):
        print(f"  - {local}")

    # Check each data class has a corresponding Local
    for cls in all_classes:
        expected_local = f"Local{cls['name'].replace('Tokens', '')}"
        # Normalize: Local{Prefix}Color vs Local{Prefix}Colors
        if not any(expected_local in l for l in locals_found):
            issues.append(f"No CompositionLocal found for {cls['name']}")

    # 5. Check {prefix}Theme accessors
    accessors = check_theme_accessors(all_content, prefix)
    print(f"\n### Theme Accessors: {len(accessors)}")
    for acc in sorted(accessors):
        print(f"  - {prefix}Theme.{acc}")

    # 6. Orphan detection
    referenced = find_primitive_references(all_content, prefix)
    orphans = [p for p in all_primitives if p not in referenced]
    # Filter out primitives that reference themselves (object self-reference)
    # and common false positives
    real_orphans = [o for o in orphans if not o.startswith("__")]

    if real_orphans:
        print(f"\n### Potentially Unused Primitives: {len(real_orphans)}")
        for o in sorted(real_orphans)[:20]:  # Cap at 20
            warnings.append(f"Primitive '{o}' may be unused")
            print(f"  - {o}")
        if len(real_orphans) > 20:
            print(f"  ... and {len(real_orphans) - 20} more")

    # 7. Naming consistency check
    print(f"\n### Naming Check")
    naming_issues = 0
    for fname, content in file_contents.items():
        if fname.startswith(prefix):
            # Check file matches expected pattern
            if "Primitive" in fname and "object" not in content:
                issues.append(f"{fname}: Expected 'object' declaration for Primitive")
                naming_issues += 1
            if "Tokens" in fname and "data class" not in content:
                issues.append(f"{fname}: Expected 'data class' declaration for Tokens")
                naming_issues += 1
    if naming_issues == 0:
        print("  All files follow naming conventions")
    else:
        print(f"  {naming_issues} naming issues found")

    # Summary
    print(f"\n### Summary")
    print(f"  Issues: {len(issues)}")
    print(f"  Warnings: {len(warnings)}")

    if issues:
        print(f"\n### Issues (must fix)")
        for i, issue in enumerate(issues, 1):
            print(f"  {i}. {issue}")

    if warnings:
        print(f"\n### Warnings (review)")
        for i, warn in enumerate(warnings, 1):
            print(f"  {i}. {warn}")

    if not issues and not warnings:
        print("\n  All checks passed!")

    sys.exit(1 if issues else 0)


if __name__ == "__main__":
    main()
