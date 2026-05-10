#!/usr/bin/env python3
"""
Kotlin unit test analysis tool.

Commands:
  units <file.kt>              - Extract testable units from Kotlin class
  coverage <source> <test>     - Analyze coverage gaps between directories

Output: JSON for Claude parsing
"""

import json
import re
import sys
from pathlib import Path
from dataclasses import dataclass, asdict
from typing import Optional


@dataclass
class Dependency:
    name: str
    type: str


@dataclass
class Method:
    name: str
    visibility: str  # public, internal, private
    suspend: bool
    return_type: str
    params: list[str]


@dataclass
class ClassAnalysis:
    class_name: str
    package: str
    file_path: str
    dependencies: list[Dependency]
    methods: list[Method]


def extract_package(content: str) -> str:
    """Extract package name from Kotlin file."""
    match = re.search(r'^package\s+([\w.]+)', content, re.MULTILINE)
    return match.group(1) if match else ""


def extract_class_name(content: str) -> str:
    """Extract primary class name."""
    # Match class or object declaration
    match = re.search(r'(?:class|object)\s+(\w+)', content)
    return match.group(1) if match else ""


def extract_dependencies(content: str) -> list[Dependency]:
    """Extract constructor dependencies."""
    deps = []
    # Match class constructor params: class Foo(private val dep: Type) with optional inheritance
    class_match = re.search(r'class\s+\w+\s*\((.*?)\)\s*(?::|{)', content, re.DOTALL)
    if class_match:
        params_str = class_match.group(1)
        # Normalize whitespace
        params_str = re.sub(r'\s+', ' ', params_str)
        # Match each param: (private|val|var)? name: Type
        for match in re.finditer(r'(?:private\s+)?(?:val|var)\s+(\w+)\s*:\s*(\w+)', params_str):
            deps.append(Dependency(name=match.group(1), type=match.group(2).strip()))
    return deps


def extract_return_type(content: str, start_pos: int) -> str:
    """Extract return type handling nested generics like Flow<Result<List<T>>>."""
    # Find colon after closing paren
    colon_match = re.search(r'^\s*:', content[start_pos:])
    if not colon_match:
        return "Unit"

    type_start = start_pos + colon_match.end()
    # Track angle bracket depth for nested generics
    depth = 0
    end = type_start
    for i, c in enumerate(content[type_start:]):
        if c == '<':
            depth += 1
        elif c == '>':
            depth -= 1
        elif c in '{=\n' and depth == 0:
            end = type_start + i
            break
    else:
        end = type_start + i + 1

    return content[type_start:end].strip() or "Unit"


def extract_methods(content: str) -> list[Method]:
    """Extract public and internal methods."""
    methods = []
    # Match methods: (override)? (suspend)? fun name(params)
    pattern = r'(?:^|\n)\s*(override\s+)?(internal\s+)?(suspend\s+)?fun\s+(\w+)\s*\(([^)]*)\)'

    for match in re.finditer(pattern, content):
        is_override = bool(match.group(1))
        is_internal = bool(match.group(2))
        is_suspend = bool(match.group(3))
        name = match.group(4)
        params_str = match.group(5).strip()
        return_type = extract_return_type(content, match.end())

        # Skip private methods (not matched by pattern) and lifecycle methods
        if name in ['onCreate', 'onDestroy', 'onStart', 'onStop', 'onResume', 'onPause']:
            continue

        # Parse params
        params = []
        if params_str:
            for p in params_str.split(','):
                p = p.strip()
                if ':' in p:
                    param_name = p.split(':')[0].strip().replace('val ', '').replace('var ', '')
                    params.append(param_name)

        visibility = "internal" if is_internal else "public"
        methods.append(Method(
            name=name,
            visibility=visibility,
            suspend=is_suspend,
            return_type=return_type,
            params=params
        ))

    return methods


def analyze_units(file_path: Path) -> dict:
    """Analyze a Kotlin file for testable units."""
    if not file_path.exists():
        return {"error": f"File not found: {file_path}"}

    content = file_path.read_text()

    analysis = ClassAnalysis(
        class_name=extract_class_name(content),
        package=extract_package(content),
        file_path=str(file_path),
        dependencies=extract_dependencies(content),
        methods=extract_methods(content)
    )

    # Convert to dict with nested dataclasses
    result = asdict(analysis)
    return result


def find_test_file(source_file: Path, source_dir: Path, test_dir: Path) -> Optional[Path]:
    """Find corresponding test file for a source file."""
    # Calculate relative path from source dir
    try:
        rel_path = source_file.relative_to(source_dir)
    except ValueError:
        return None

    # Construct test file path: SomeClass.kt -> SomeClassTest.kt
    test_name = source_file.stem + "Test.kt"
    test_path = test_dir / rel_path.parent / test_name

    return test_path if test_path.exists() else None


def extract_test_methods(content: str) -> list[str]:
    """Extract method names being tested from test file."""
    tested = set()
    # Match backtick names: fun `methodName should...`()
    for match in re.finditer(r'fun\s+`([^`]+)`\s*\(', content):
        test_name = match.group(1)
        # Extract method name from "methodName should X when Y"
        words = test_name.split()
        if words:
            tested.add(words[0])

    # Match testMethodName pattern (camelCase)
    for match in re.finditer(r'fun\s+test(\w+)\s*\(', content):
        method_name = match.group(1)
        if method_name:
            tested.add(method_name[0].lower() + method_name[1:] if len(method_name) > 1 else method_name.lower())

    # Match test_method_name pattern (snake_case)
    for match in re.finditer(r'fun\s+test_(\w+)\s*\(', content):
        # Convert snake_case to camelCase: test_sync_user -> syncUser
        parts = match.group(1).split('_')
        if parts:
            method_name = parts[0] + ''.join(p.capitalize() for p in parts[1:])
            tested.add(method_name)

    # Match verify_method_name pattern
    for match in re.finditer(r'fun\s+verify_?(\w+)\s*\(', content):
        method_name = match.group(1)
        if method_name:
            tested.add(method_name[0].lower() + method_name[1:] if len(method_name) > 1 else method_name.lower())

    return list(tested)


def analyze_coverage(source_dir: Path, test_dir: Path) -> dict:
    """Analyze coverage gaps between source and test directories."""
    if not source_dir.exists():
        return {"error": f"Source directory not found: {source_dir}"}
    if not test_dir.exists():
        return {"error": f"Test directory not found: {test_dir}"}

    results = {
        "source_dir": str(source_dir),
        "test_dir": str(test_dir),
        "files": [],
        "summary": {"total_methods": 0, "tested_methods": 0, "coverage_percent": 0}
    }

    # Find all Kotlin source files
    for source_file in source_dir.rglob("*.kt"):
        # Skip interfaces and test files
        if source_file.name.endswith("Test.kt") or "Interface" in source_file.name:
            continue

        content = source_file.read_text()
        methods = extract_methods(content)

        if not methods:
            continue

        # Find test file
        test_file = find_test_file(source_file, source_dir, test_dir)
        tested_methods = []

        if test_file:
            test_content = test_file.read_text()
            tested_methods = extract_test_methods(test_content)

        # Compare
        method_names = [m.name for m in methods]
        tested = [m for m in method_names if m in tested_methods]
        missing = [m for m in method_names if m not in tested_methods]

        file_result = {
            "file": source_file.name,
            "path": str(source_file),
            "test_file": str(test_file) if test_file else None,
            "methods": method_names,
            "tested": tested,
            "missing": missing,
            "coverage_percent": round(len(tested) / len(method_names) * 100) if method_names else 0
        }

        results["files"].append(file_result)
        results["summary"]["total_methods"] += len(method_names)
        results["summary"]["tested_methods"] += len(tested)

    # Calculate overall coverage
    total = results["summary"]["total_methods"]
    tested = results["summary"]["tested_methods"]
    results["summary"]["coverage_percent"] = round(tested / total * 100) if total > 0 else 0

    return results


def main():
    if len(sys.argv) < 2:
        print(json.dumps({"error": "Usage: analyze_kotlin.py <units|coverage> [args...]"}))
        sys.exit(1)

    command = sys.argv[1]

    if command == "units":
        if len(sys.argv) < 3:
            print(json.dumps({"error": "Usage: analyze_kotlin.py units <file.kt>"}))
            sys.exit(1)
        result = analyze_units(Path(sys.argv[2]))

    elif command == "coverage":
        if len(sys.argv) < 4:
            print(json.dumps({"error": "Usage: analyze_kotlin.py coverage <source_dir> <test_dir>"}))
            sys.exit(1)
        result = analyze_coverage(Path(sys.argv[2]), Path(sys.argv[3]))

    else:
        result = {"error": f"Unknown command: {command}. Use 'units' or 'coverage'"}

    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
