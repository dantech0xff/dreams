# Naming Conventions

Token naming rules for three-layer Compose design token systems.

## Layer Naming Patterns

| Layer | Object/Class Pattern | Field Pattern | Example |
|-------|---------------------|---------------|---------|
| Primitive | `{Prefix}Primitive{Category}` | `{Name}{Scale}` or `{Scale}` | `AppPrimitiveColors.Blue500` |
| Semantic | `{Prefix}{Category}Tokens` | `camelCase purpose` | `AppColorTokens.primary` |
| Component | `{Prefix}{Component}Tokens` | `camelCase part+property` | `AppDialogTokens.containerColor` |

## CompositionLocal Naming

| Type | Pattern | Example |
|------|---------|---------|
| Semantic Local | `Local{Prefix}{Category}` | `LocalAppColors` |
| Component Local | `Local{Prefix}{Component}Tokens` | `LocalAppDialogTokens` |

## Theme Accessor

```kotlin
object {Prefix}Theme {
    val colors    // → Local{Prefix}Colors.current
    val spacing   // → Local{Prefix}Spacing.current
    val shapes    // → Local{Prefix}Shapes.current
    val dialog    // → Local{Prefix}DialogTokens.current
}
```

Usage: `{Prefix}Theme.colors.primary`, `{Prefix}Theme.spacing.screenPadding`

## File Naming (Kotlin PascalCase)

| File | Contents |
|------|----------|
| `{Prefix}PrimitiveColors.kt` | Color palette object |
| `{Prefix}PrimitiveSpacing.kt` | Spacing scale object |
| `{Prefix}PrimitiveShape.kt` | Shape scale object |
| `{Prefix}PrimitiveTypography.kt` | Font size/weight/lineHeight object |
| `{Prefix}PrimitiveMotion.kt` | Duration + easing object |
| `{Prefix}PrimitiveOpacity.kt` | Alpha scale object |
| `{Prefix}PrimitiveBorder.kt` | Border width object |
| `{Prefix}PrimitiveElevation.kt` | Elevation scale object |
| `{Prefix}PrimitiveIconSize.kt` | Icon dimension object |
| `{Prefix}ColorTokens.kt` | Semantic color data class + defaults |
| `{Prefix}SpacingTokens.kt` | Semantic spacing data class |
| `{Prefix}DialogTokens.kt` | Component token class + defaults |
| `{Prefix}Theme.kt` | Provider + accessor |
| `{Prefix}Locals.kt` | All CompositionLocal definitions |

## Prefix Convention

- `{Prefix}` = project-specific prefix (e.g., `Qzds`, `App`, `Acme`)
- Set once per project, used consistently across all token files
- Templates use `{Prefix}` placeholder — replace on first use

## Scale Naming

### Size Scales (Spacing, IconSize, Elevation, Border)

```
Xxxs → Xxs → Xs → Sm → SmPlus → Md → Lg → Xl → Xxl → Xxxl → Huge → Giant → Enormous → Massive
```

### Color Scales

```
{Color}50 → {Color}100 → ... → {Color}900
```

50 = lightest, 900 = darkest. Follows Material Design convention.

### Semantic Field Names

- **Color fields**: `primary`, `onPrimary`, `surface`, `onSurface`, `error`, etc.
- **Spacing fields**: `componentPadding`, `sectionGap`, `screenPadding`
- **Shape fields**: `small`, `medium`, `large`, `extraLarge`, `full`
- **Typography fields**: `displayLarge`, `headlineMedium`, `bodyLarge`, `labelSmall`
- **Component fields**: `container{Property}`, `content{Property}`, `title{Property}`

## Anti-Patterns

| Wrong | Right | Why |
|-------|-------|-----|
| `val blue = Color(...)` | `val Blue500 = Color(...)` | Missing scale |
| `val dialogBg` | `val containerColor` | Use part+property pattern |
| `MyColors` | `{Prefix}ColorTokens` | Missing prefix convention |
| `COLOR_PRIMARY` | `primary` | Semantic fields use camelCase |
| `LocalColors` | `Local{Prefix}Colors` | Missing prefix in Local |
