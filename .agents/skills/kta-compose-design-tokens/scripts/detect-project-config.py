#!/usr/bin/env python3
"""Auto-detect Kotlin Compose project config. JSON to stdout, warnings to stderr.
Usage: python3 detect-project-config.py --dir /path/to/project [--quiet]
Exit 0 = success (null = detection failed), Exit 1 = script error. Stdlib only.
"""
import argparse, json, os, re, sys
from pathlib import Path

NS_RE = re.compile(r'^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)*$')
PFX_RE = re.compile(r'^[A-Z][a-zA-Z0-9]*$')
BOM_RE = re.compile(r'^\d{4}\.\d{2}\.\d{2}$')
_warnings, _quiet = [], False

def warn(msg):
    _warnings.append(msg)
    if not _quiet: print(f"WARNING: {msg}", file=sys.stderr)

def val(v, pat, field):
    if v and pat.match(v): return v
    if v: warn(f"{field} '{v}' failed validation, set null")
    return None

def rf(p):
    try: return p.read_text(encoding="utf-8")
    except (OSError, UnicodeDecodeError): return None

def skip(p): return "/build/" in str(p) or "/test/" in str(p)

def detect_namespace(root):
    for g in [root/"app"/"build.gradle.kts", root/"build.gradle.kts"]:
        c = rf(g)
        if c and (m := re.search(r'namespace\s*=\s*"([^"]+)"', c)):
            return val(m.group(1), NS_RE, "namespace")
    c = rf(root/"app"/"src"/"main"/"AndroidManifest.xml")
    if c and (m := re.search(r'package="([^"]+)"', c)):
        return val(m.group(1), NS_RE, "namespace")
    for kt in sorted(root.rglob("*.kt"))[:20]:
        c = rf(kt)
        if c and (m := re.search(r'^package\s+([\w.]+)', c, re.MULTILINE)):
            pkg = m.group(1)
            for s in [".ui",".theme",".data",".domain",".core"]:
                if pkg.endswith(s): pkg = pkg[:-len(s)]; break
            return val(pkg, NS_RE, "namespace")
    return None

def detect_prefix(root):
    prefixes = {}
    for kt in root.rglob("*Theme.kt"):
        if skip(kt): continue
        c = rf(kt)
        if not c: continue
        for m in re.finditer(r'(?:object|class)\s+(\w+)Theme\b', c):
            n = m.group(1)
            if n in ("Material","MaterialTheme"): continue
            prefixes.setdefault(n, []).append(str(kt.relative_to(root)))
    if not prefixes:
        for kt in root.rglob("*Tokens.kt"):
            if skip(kt): continue
            c = rf(kt)
            if not c: continue
            for m in re.finditer(r'(?:object|class)\s+(\w+?)(?:Color|Spacing|Shape)?Tokens\b', c):
                prefixes.setdefault(m.group(1), []).append(str(kt.relative_to(root)))
    if not prefixes: return None, [], False
    multi = len(prefixes) > 1
    if multi: warn(f"Multiple prefixes: {list(prefixes.keys())}. Using tiebreaker.")
    files = sorted(set(f for fs in prefixes.values() for f in fs))
    best = sorted(prefixes, key=lambda n: (0 if any("theme/" in f for f in prefixes[n])
        else 1 if any("app/" in f for f in prefixes[n]) else 2, n))[0]
    return val(best, PFX_RE, "prefix"), files, multi

def detect_modules(root):
    for name in ["settings.gradle.kts", "settings.gradle"]:
        c = rf(root / name)
        if not c: continue
        # KTS: include("...") or Groovy: include ':...'
        incs = re.findall(r'include\s*\(?["\':]+([^"\')\s]+)', c)
        has_feat = any("feature" in i.lower() for i in incs)
        n = len(incs)
        if n <= 1: return "single"
        if has_feat: return "feature-modules"
        return "composed" if n <= 5 else "multi-module"
    return None

def detect_target(root):
    for name, p in [("theme", root/"theme"), ("ui-components", root/"ui-components"), ("core/ui", root/"core"/"ui")]:
        if p.is_dir(): return name
    return "app"

def detect_m3(root):
    c = rf(root/"gradle"/"libs.versions.toml")
    if c and "material3" in c.lower(): return True
    for g in root.rglob("build.gradle*"):
        if skip(g): continue
        c = rf(g)
        if c and "material3" in c: return True
    return False

def detect_bom(root):
    c = rf(root/"gradle"/"libs.versions.toml")
    if c and (m := re.search(r'composeBom\s*=\s*"([^"]+)"', c)):
        return val(m.group(1), BOM_RE, "composeBomVersion")
    for g in root.rglob("build.gradle*"):
        if skip(g): continue
        c = rf(g)
        if c and (m := re.search(r'compose-bom:(\d{4}\.\d{2}\.\d{2})', c)):
            return val(m.group(1), BOM_RE, "composeBomVersion")
    return None

def main():
    global _quiet
    p = argparse.ArgumentParser(description="Detect Kotlin Compose project config.")
    p.add_argument("--dir", required=True, help="Project root directory")
    p.add_argument("--quiet", action="store_true", help="Suppress warnings")
    a = p.parse_args(); _quiet = a.quiet
    root = Path(os.path.realpath(a.dir))
    if not root.is_dir():
        json.dump({"error": f"'{a.dir}' not a valid directory"}, sys.stdout); sys.exit(1)
    try:
        ns = detect_namespace(root)
        pfx, tf, mp = detect_prefix(root)
        json.dump({"namespace": ns, "prefix": pfx, "moduleStructure": detect_modules(root),
            "targetModule": detect_target(root), "material3": detect_m3(root),
            "composeBomVersion": detect_bom(root), "detectedThemeFiles": tf,
            "detectedMultiplePrefixes": mp, "warnings": _warnings}, sys.stdout, indent=2)
        print()
    except Exception as e:
        json.dump({"error": str(e)}, sys.stdout); print(); sys.exit(1)

if __name__ == "__main__":
    main()
