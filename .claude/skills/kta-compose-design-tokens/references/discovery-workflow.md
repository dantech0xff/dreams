# Discovery Workflow

Instructions for Claude to guide users through design token discovery before generation.

## How to Use

1. Ask questions in order (Q1-Q10), respecting skip conditions
2. Use multiple-choice options — never open-ended except prefix/namespace
3. After all questions, construct the Discovery Context block
4. Run `detect-project-config.py` to auto-fill/verify prefix, namespace, module
5. Confirm all values with user before proceeding to generation

**Validation rules:**
- `prefix`: must match `^[A-Z][a-zA-Z0-9]*$` — re-ask if invalid
- `namespace`: must match `^[a-z][a-z0-9]*(\.[a-z][a-z0-9]*)*$` — re-ask if invalid

**M2 guard:** If detection finds `material3=false`, warn: "This skill generates Material 3 tokens. Your project uses Material 2. Add M3 dependency first or abort." Do NOT generate M3 code for M2 projects.

**Existing file conflict:** For `existing-enhance` projects, check for existing token files in target module. List found files and ask: "Overwrite, skip, or generate alongside with different names?"

## Question Flow

### Stage 1: CONTEXT (~1 min)

**Q1: Project state?**
- `new` — Starting a new project from scratch
- `existing-refactor` — Refactoring existing theme to token system
- `existing-enhance` — Adding tokens alongside existing theme

**Q2: Design maturity?**
- `finalized` — Have final designs/specs/Figma
- `direction-only` — Know the visual direction, no exact specs
- `starting-fresh` — No design decisions made yet

### Stage 2: SCOPE (~2 min)

**Q3: Token scope?**
- `minimal` — Colors + Spacing + Shapes only (~5 files)
- `standard` — + Typography + Elevation (~7 files)
- `comprehensive` — + Motion + Opacity + Border + Component specs (~15 files)

**Q4: Theme variants?**
- `single` — One theme only
- `light-dark` — Light and dark themes
- `multi-scheme` — Dynamic color / multiple branded themes

**Q5: Material integration?**
- `m3-default` — Use Material 3 defaults, extend with tokens
- `m3-custom` — Custom Material 3 color scheme with tokens
- `independent` — Standalone tokens, minimal M3 dependency

**Q6: Current theme state?** *(SKIP if Q1 = `new`)*
- `none` — No theme setup at all
- `basic-material` — Default MaterialTheme only
- `partial-local` — Some CompositionLocals / custom tokens exist
- `complete` — Full theme system, migrating to tokens

### Stage 3: GROUNDING (~1 min)

**Q7: Brand colors?**
- `have-hex-values` — Have exact hex codes ready
- `reference-file` — Colors in a design file/Figma link
- `use-m3-defaults` — Start with Material 3 default palette

**Q8: Spacing feel?** *(SKIP if Q3 = `minimal` AND user wants defaults)*
- `tight-4dp` — 4dp base unit (compact UI)
- `balanced-8dp` — 8dp base unit (standard)
- `no-preference` — Use 8dp default

**Q9: Typography approach?** *(SKIP if Q3 = `minimal`)*
- `system-fonts` — System default fonts only
- `custom-fonts` — Custom/Google fonts
- `minimal` — Minimal type scale (3-4 sizes)

### Stage 4: VALIDATION (~30 sec)

**Q10: Confidence level?**
- `ready` — Proceed to detection + generation
- `want-recommendations` — Claude suggests based on answers
- `need-help` — Claude walks through each decision

## Decision Tree (Skip Rules)

```
IF Q1 = "new" → SKIP Q6
IF Q3 = "minimal" → SKIP Q9
IF Q3 = "minimal" AND Q7 = "use-m3-defaults" → SKIP Q8
```

Shortest path: Q1→Q2→Q3(minimal)→Q4→Q5→Q7(m3-defaults)→Q10 = **7 questions**
Longest path: Q1(existing)→Q2→Q3(comprehensive)→Q4→Q5→Q6→Q7→Q8→Q9→Q10 = **10 questions**

## Discovery Context Output

After completing questions, construct this block:

```
## Discovery Context
- project_state: new|existing-refactor|existing-enhance
- design_maturity: finalized|direction-only|starting-fresh
- token_scope: minimal|standard|comprehensive
- theme_variants: single|light-dark|multi-scheme
- material_integration: m3-default|m3-custom|independent
- existing_theme: none|basic-material|partial-local|complete|n/a
- brand_colors: have-values|reference-file|m3-defaults
- spacing_base: 4dp|8dp
- typography: system|custom|minimal|n/a
- confidence: ready|recommendations|help
- prefix: {detected or user-specified}
- namespace: {detected or user-specified}
- target_module: {detected or user-specified}
```

This context drives scope preset selection and reference/template loading.

## Next Steps After Discovery

1. Run detection script: `python scripts/detect-project-config.py --dir <project-root>`
2. Merge detection results with discovery answers
3. Confirm prefix, namespace, target module with user
4. Load scope preset from `defaults/token-scope-presets.md`
5. Load matching references and templates
6. Generate token files
