#!/usr/bin/env python3
"""Scan Kotlin Compose files for hardcoded design values that should use tokens.

Usage:
    python validate-hardcoded-values.py --dir <src-path>
    python validate-hardcoded-values.py --dir src/ --severity error
    python validate-hardcoded-values.py --dir src/ --fix-suggestions

Exit code 1 if violations found (CI-friendly).
"""

import argparse
import os
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path

# --- Patterns to detect ---

PATTERNS = [
    {
        "name": "Color hex",
        "regex": r"Color\(0x[0-9A-Fa-f]+\)",
        "severity": "error",
        "suggestion": "Use {Prefix}Theme.colors.xxx",
    },
    {
        "name": "Color named",
        "regex": r"(?<!\w)Color\.(Black|White|Red|Green|Blue|Yellow|Cyan|Magenta|Gray|DarkGray|LightGray|Transparent|Unspecified)",
        "severity": "warning",
        "suggestion": "Use {Prefix}PrimitiveColors.xxx or {Prefix}Theme.colors.xxx",
    },
    {
        "name": "Dp hardcode",
        "regex": r"(?<!\w)\d+\.dp\b",
        "severity": "warning",
        "suggestion": "Use {Prefix}Theme.spacing.xxx or {Prefix}PrimitiveSpacing.xxx",
    },
    {
        "name": "Sp hardcode",
        "regex": r"(?<!\w)\d+\.sp\b",
        "severity": "warning",
        "suggestion": "Use {Prefix}PrimitiveTypography.FontSizeXxx",
    },
    {
        "name": "Shape hardcode",
        "regex": r"RoundedCornerShape\(\d+",
        "severity": "warning",
        "suggestion": "Use {Prefix}Theme.shapes.xxx or {Prefix}PrimitiveShape.xxx",
    },
    {
        "name": "FontWeight hardcode",
        "regex": r"FontWeight\.(Thin|ExtraLight|Light|Normal|Medium|SemiBold|Bold|ExtraBold|Black|W\d+)",
        "severity": "info",
        "suggestion": "Define in typography tokens",
    },
    {
        "name": "Tween duration hardcode",
        "regex": r"tween\(\s*\d+",
        "severity": "info",
        "suggestion": "Use {Prefix}Theme.motion.durationXxx",
    },
]

# --- Exclusion rules ---

EXCLUDE_FILE_PATTERNS = [
    r"Primitive.*\.kt$",
    r"Token.*\.kt$",
    r"Theme.*\.kt$",
    r"Test\.kt$",
    r"\.test\.kt$",
]

# Directories excluded by default (templates, skill scripts, build outputs)
DEFAULT_EXCLUDE_DIRS = [
    ".claude",
    "templates",
    "build",
    ".gradle",
    ".git",
]

SUPPRESS_LINE = "// noinspection DesignToken"
SUPPRESS_FILE = "// design-token-ignore-file"


@dataclass
class Violation:
    file: str
    line_num: int
    line_text: str
    pattern_name: str
    severity: str
    suggestion: str


@dataclass
class Report:
    violations: list = field(default_factory=list)
    files_scanned: int = 0
    files_with_violations: int = 0
    counts: dict = field(default_factory=lambda: {"error": 0, "warning": 0, "info": 0})


def should_exclude_file(filepath: str) -> bool:
    basename = os.path.basename(filepath)
    return any(re.search(p, basename) for p in EXCLUDE_FILE_PATTERNS)


def is_in_preview(line: str, in_preview_block: bool) -> bool:
    """Simple heuristic: skip lines inside @Preview functions."""
    return in_preview_block


def scan_file(filepath: str, severity_filter: str | None) -> list[Violation]:
    violations = []
    try:
        with open(filepath, "r", encoding="utf-8") as f:
            lines = f.readlines()
    except (OSError, UnicodeDecodeError):
        return violations

    # Check file-level suppression
    if lines and SUPPRESS_FILE in lines[0]:
        return violations

    in_preview = False
    for i, line in enumerate(lines, 1):
        stripped = line.strip()

        # Track @Preview blocks (simple: from @Preview to next blank line or fun)
        if "@Preview" in stripped:
            in_preview = True
            continue
        if in_preview and (stripped == "" or stripped.startswith("fun ") or stripped.startswith("@")):
            if not stripped.startswith("@Preview"):
                in_preview = False

        if in_preview:
            continue

        # Check line-level suppression
        if SUPPRESS_LINE in line:
            continue

        for pattern in PATTERNS:
            if severity_filter and pattern["severity"] != severity_filter:
                continue
            if re.search(pattern["regex"], line):
                violations.append(
                    Violation(
                        file=filepath,
                        line_num=i,
                        line_text=stripped[:100],
                        pattern_name=pattern["name"],
                        severity=pattern["severity"],
                        suggestion=pattern["suggestion"],
                    )
                )
    return violations


def scan_directory(dir_path: str, severity_filter: str | None, follow_symlinks: bool, exclude_dirs: list[str] | None = None) -> Report:
    report = Report()
    real_root = os.path.realpath(dir_path)
    excluded = set(exclude_dirs or DEFAULT_EXCLUDE_DIRS)

    for root, dirs, files in os.walk(real_root, followlinks=follow_symlinks):
        # Security: ensure we stay within the target directory
        if not os.path.realpath(root).startswith(real_root + os.sep) and os.path.realpath(root) != real_root:
            continue

        # Skip excluded directories (prune in-place for efficiency)
        dirs[:] = [d for d in dirs if d not in excluded]

        for fname in files:
            if not fname.endswith(".kt"):
                continue

            filepath = os.path.join(root, fname)
            if should_exclude_file(filepath):
                continue

            report.files_scanned += 1
            violations = scan_file(filepath, severity_filter)
            if violations:
                report.files_with_violations += 1
                report.violations.extend(violations)
                for v in violations:
                    report.counts[v.severity] += 1

    return report


def print_report(report: Report, fix_suggestions: bool) -> None:
    if not report.violations:
        print("No hardcoded values found.")
        print(f"Scanned {report.files_scanned} files.")
        return

    print("=== Hardcoded Values Report ===\n")

    # Group by file
    by_file: dict[str, list[Violation]] = {}
    for v in report.violations:
        by_file.setdefault(v.file, []).append(v)

    for filepath, violations in sorted(by_file.items()):
        print(f"{filepath}:")
        for v in sorted(violations, key=lambda x: x.line_num):
            marker = {"error": "E", "warning": "W", "info": "I"}[v.severity]
            line = f"  L{v.line_num}: [{marker}] {v.pattern_name} → {v.line_text}"
            if fix_suggestions:
                line += f"\n         Fix: {v.suggestion}"
            print(line)
        print()

    total = sum(report.counts.values())
    print(f"Summary: {total} hardcoded values in {report.files_with_violations} files")
    print(f"  Errors: {report.counts['error']} | Warnings: {report.counts['warning']} | Info: {report.counts['info']}")
    print(f"  Files scanned: {report.files_scanned}")


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Scan Kotlin Compose files for hardcoded design values."
    )
    parser.add_argument("--dir", required=True, help="Directory to scan")
    parser.add_argument(
        "--severity",
        choices=["error", "warning", "info"],
        default=None,
        help="Filter by severity level",
    )
    parser.add_argument(
        "--fix-suggestions",
        action="store_true",
        help="Show replacement suggestions (read-only, never mutates files)",
    )
    parser.add_argument(
        "--follow-symlinks",
        action="store_true",
        help="Follow symlinks (disabled by default for security)",
    )
    parser.add_argument(
        "--exclude",
        nargs="*",
        default=None,
        help="Additional directory names to exclude (added to defaults: .claude, templates, build, .gradle, .git)",
    )
    parser.add_argument(
        "--no-default-excludes",
        action="store_true",
        help="Disable default directory exclusions",
    )
    parser.add_argument("--version", action="version", version="1.1.0")
    args = parser.parse_args()

    # Validate directory
    target = os.path.realpath(args.dir)
    if not os.path.isdir(target):
        print(f"Error: '{args.dir}' is not a valid directory.", file=sys.stderr)
        sys.exit(2)

    # Build exclusion list
    exclude_dirs = [] if args.no_default_excludes else list(DEFAULT_EXCLUDE_DIRS)
    if args.exclude:
        exclude_dirs.extend(args.exclude)

    report = scan_directory(target, args.severity, args.follow_symlinks, exclude_dirs)
    print_report(report, args.fix_suggestions)

    if report.violations:
        sys.exit(1)


if __name__ == "__main__":
    main()
