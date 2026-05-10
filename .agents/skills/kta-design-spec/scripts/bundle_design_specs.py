#!/usr/bin/env python3
"""
bundle_design_specs.py — Concatenate all design spec files in a plan's design/
directory into a single markdown bundle ready to upload to Design AI Tools
(Stitch, Claude, Figma AI, etc.).

Order:
  1. Header (title + generated timestamp + table of contents)
  2. style-foundation.md
  3. INDEX.md (if exists)
  4. Per-feature directories (sorted by NN prefix)
       → per-screen spec files (sorted by NN prefix) inside each

Usage:
  python3 bundle_design_specs.py <design-dir> [--output <path>]

  <design-dir>  — absolute or relative path to plans/{slug}/design/
  --output      — output file path (default: <design-dir>/design-bundle.md)

Exit codes:
  0  — success
  1  — design-dir missing or invalid
  2  — required files missing (no style-foundation.md or no screen specs)
"""

from __future__ import annotations

import argparse
import re
import sys
from datetime import datetime
from pathlib import Path

NN_PREFIX = re.compile(r"^(\d+)[-_]")


def sort_key(p: Path) -> tuple[int, str]:
    """Sort by leading NN- prefix, fall back to name."""
    m = NN_PREFIX.match(p.name)
    return (int(m.group(1)) if m else 9999, p.name.lower())


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8").rstrip() + "\n"


def collect_feature_dirs(design_dir: Path) -> list[Path]:
    return sorted(
        [d for d in design_dir.iterdir() if d.is_dir() and NN_PREFIX.match(d.name)],
        key=sort_key,
    )


def collect_screen_files(feature_dir: Path) -> list[Path]:
    return sorted(
        [f for f in feature_dir.glob("*.md") if NN_PREFIX.match(f.name)],
        key=sort_key,
    )


def section_separator(label: str) -> str:
    bar = "═" * 78
    return f"\n\n<!-- {bar} -->\n<!-- {label} -->\n<!-- {bar} -->\n\n"


def build_bundle(design_dir: Path) -> tuple[str, dict[str, int]]:
    style_file = design_dir / "style-foundation.md"
    index_file = design_dir / "INDEX.md"

    if not style_file.exists():
        print(f"[bundle] ERROR: missing {style_file}", file=sys.stderr)
        sys.exit(2)

    feature_dirs = collect_feature_dirs(design_dir)
    if not feature_dirs:
        print(f"[bundle] ERROR: no feature directories found in {design_dir}", file=sys.stderr)
        sys.exit(2)

    parts: list[str] = []
    counts = {"features": 0, "screens": 0}

    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M")
    parts.append(
        f"# Design Spec Bundle\n\n"
        f"_Generated {timestamp} from `{design_dir}`._\n\n"
        f"**Use this file as input for Design AI Tools (Stitch, Claude artifacts, Figma AI).**\n\n"
        f"Contents — style foundation → screen index → per-screen specs grouped by feature.\n"
    )

    parts.append(section_separator("STYLE FOUNDATION"))
    parts.append(read_text(style_file))

    if index_file.exists():
        parts.append(section_separator("INDEX"))
        parts.append(read_text(index_file))

    for fdir in feature_dirs:
        screens = collect_screen_files(fdir)
        if not screens:
            continue
        counts["features"] += 1
        parts.append(section_separator(f"FEATURE — {fdir.name}"))
        parts.append(f"# Feature: {fdir.name}\n")
        for sfile in screens:
            counts["screens"] += 1
            parts.append(f"\n---\n\n## Screen file: `{fdir.name}/{sfile.name}`\n\n")
            parts.append(read_text(sfile))

    return "".join(parts), counts


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("design_dir", help="Path to plans/{slug}/design/ directory")
    parser.add_argument("--output", help="Output file path (default: <design-dir>/design-bundle.md)")
    args = parser.parse_args()

    design_dir = Path(args.design_dir).resolve()
    if not design_dir.is_dir():
        print(f"[bundle] ERROR: not a directory: {design_dir}", file=sys.stderr)
        return 1

    output_path = Path(args.output).resolve() if args.output else design_dir / "design-bundle.md"

    bundle_text, counts = build_bundle(design_dir)
    output_path.write_text(bundle_text, encoding="utf-8")

    size_kb = output_path.stat().st_size / 1024
    print(
        f"[bundle] OK\n"
        f"  features: {counts['features']}\n"
        f"  screens : {counts['screens']}\n"
        f"  output  : {output_path}\n"
        f"  size    : {size_kb:.1f} KB\n"
        f"\n"
        f"Upload {output_path.name} to Stitch / Claude / Figma AI."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
