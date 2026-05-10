# Token Scope Presets

Maps discovery scope choice (Q3) to references and templates to load.

## Minimal (~5 files)

Best for: new projects, prototypes, simple apps.

| References to Load | Templates to Use |
|-------------------|------------------|
| `references/primitive-colors.md` | `templates/PrimitiveColorsTemplate.kt` |
| `references/primitive-spacing-shape.md` | `templates/PrimitiveSpacingTemplate.kt` |
| `references/semantic-colors.md` | `templates/PrimitiveShapeTemplate.kt` |
| `references/naming-conventions.md` | `templates/SemanticColorsTemplate.kt` |
| | `templates/ThemeProviderTemplate.kt` |

## Standard (~7 files)

Best for: most production apps with light/dark themes.

| References to Load | Templates to Use |
|-------------------|------------------|
| All Minimal references + | All Minimal templates + |
| `references/primitive-typography-motion.md` | Typography tokens (generated inline) |
| `references/semantic-layout-typography.md` | Elevation tokens (generated inline) |
| `references/composition-local-provider.md` | |

## Comprehensive (~15 files)

Best for: design system teams, component libraries, multi-app platforms.

| References to Load | Templates to Use |
|-------------------|------------------|
| All Standard references + | All Standard templates + |
| `references/semantic-motion-opacity.md` | Component spec tokens (per selection) |
| `references/motion-tokens.md` | |
| `references/opacity-border-tokens.md` | |
| `references/domain-extension-tokens.md` | |
| Component specs (user selects): | |
| `references/snackbar-tokens.md` | |
| `references/dialog-tokens.md` | |
| `references/bottom-sheet-tokens.md` | |
| `references/bottom-nav-tokens.md` | |
| `references/button-tokens.md` | |
| `references/card-tokens.md` | |
| `references/text-field-tokens.md` | |
| `references/top-bar-tokens.md` | |
| `references/badge-chip-tokens.md` | |
| `references/divider-tokens.md` | |
| `references/skeleton-tokens.md` | |
