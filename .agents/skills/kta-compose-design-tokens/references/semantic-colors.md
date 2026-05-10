# Semantic Color Tokens

## Data Class

```kotlin
@Immutable
data class {Prefix}ColorTokens(
    // Primary
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    // Secondary
    val secondary: Color,
    val onSecondary: Color,
    // Tertiary
    val tertiary: Color,
    val onTertiary: Color,
    // Semantic status
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val error: Color,
    val onError: Color,
    val info: Color,
    val onInfo: Color,
    // Backgrounds & surfaces
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    // Utility
    val outline: Color,
    val outlineVariant: Color,
    val scrim: Color,
    val disabled: Color,
    val onDisabled: Color,
)
```

## Dark Defaults (map to primitives)

Uses `{Prefix}PrimitiveColors` (50–900 scale). For neutral surface tones not in
`PrimitiveColors` (e.g. Neutral12, Neutral25), use hardcoded hex — or extend
`{Prefix}PrimitiveColors` with project-specific Neutral/Grey steps.

```kotlin
val dark{Prefix}Colors = {Prefix}ColorTokens(
    primary            = {Prefix}PrimitiveColors.Purple200,   // M3 ~Purple80 tonal
    onPrimary          = {Prefix}PrimitiveColors.Purple900,   // M3 ~Purple10
    primaryContainer   = {Prefix}PrimitiveColors.Purple700,   // M3 ~Purple30
    onPrimaryContainer = {Prefix}PrimitiveColors.Purple100,   // M3 ~Purple90
    secondary          = {Prefix}PrimitiveColors.Blue200,     // substitute for Teal80
    onSecondary        = {Prefix}PrimitiveColors.Blue900,     // substitute for Teal10
    tertiary           = {Prefix}PrimitiveColors.Blue200,     // M3 ~Blue80 tonal
    onTertiary         = {Prefix}PrimitiveColors.Blue900,     // M3 ~Blue10
    success            = {Prefix}PrimitiveColors.Green200,    // M3 ~Green80
    onSuccess          = {Prefix}PrimitiveColors.Green900,    // M3 ~Green10
    warning            = {Prefix}PrimitiveColors.Yellow200,   // substitute for Amber80
    onWarning          = {Prefix}PrimitiveColors.Yellow900,   // substitute for Amber10
    error              = {Prefix}PrimitiveColors.Red200,      // M3 ~Red80
    onError            = {Prefix}PrimitiveColors.Red900,      // M3 ~Red10
    info               = {Prefix}PrimitiveColors.Blue200,     // M3 ~Blue80 tonal
    onInfo             = {Prefix}PrimitiveColors.Blue900,     // M3 ~Blue10
    background         = {Prefix}PrimitiveColors.Grey900,     // ~Neutral10 dark bg
    onBackground       = {Prefix}PrimitiveColors.Grey100,     // ~Neutral90
    surface            = Color(0xFF1C1B1F),                   // ~Neutral12 (dark surface)
    onSurface          = {Prefix}PrimitiveColors.Grey100,     // ~Neutral90
    surfaceVariant     = {Prefix}PrimitiveColors.Grey700,     // ~NeutralVariant30
    onSurfaceVariant   = {Prefix}PrimitiveColors.Grey200,     // ~NeutralVariant80
    surfaceContainer   = {Prefix}PrimitiveColors.Grey800,     // ~Neutral20
    surfaceContainerHigh = Color(0xFF302D38),                 // ~Neutral25 (dark)
    outline            = {Prefix}PrimitiveColors.Grey400,     // ~NeutralVariant60
    outlineVariant     = {Prefix}PrimitiveColors.Grey700,     // ~NeutralVariant30
    scrim              = {Prefix}PrimitiveColors.Black,
    disabled           = {Prefix}PrimitiveColors.Grey700,     // ~Neutral30
    onDisabled         = {Prefix}PrimitiveColors.Grey400,     // ~Neutral60
)
```

## Light Defaults (map to primitives)

```kotlin
val light{Prefix}Colors = {Prefix}ColorTokens(
    primary            = {Prefix}PrimitiveColors.Purple600,   // M3 ~Purple40
    onPrimary          = {Prefix}PrimitiveColors.White,
    primaryContainer   = {Prefix}PrimitiveColors.Purple100,   // M3 ~Purple90
    onPrimaryContainer = {Prefix}PrimitiveColors.Purple900,   // M3 ~Purple10
    secondary          = {Prefix}PrimitiveColors.Blue600,     // substitute for Teal40
    onSecondary        = {Prefix}PrimitiveColors.White,
    tertiary           = {Prefix}PrimitiveColors.Blue600,     // M3 ~Blue40
    onTertiary         = {Prefix}PrimitiveColors.White,
    success            = {Prefix}PrimitiveColors.Green600,    // M3 ~Green40
    onSuccess          = {Prefix}PrimitiveColors.White,
    warning            = {Prefix}PrimitiveColors.Yellow600,   // substitute for Amber40
    onWarning          = {Prefix}PrimitiveColors.White,
    error              = {Prefix}PrimitiveColors.Red600,      // M3 ~Red40
    onError            = {Prefix}PrimitiveColors.White,
    info               = {Prefix}PrimitiveColors.Blue600,     // M3 ~Blue40
    onInfo             = {Prefix}PrimitiveColors.White,
    background         = {Prefix}PrimitiveColors.Grey50,      // ~Neutral99 light bg
    onBackground       = {Prefix}PrimitiveColors.Grey900,     // ~Neutral10
    surface            = {Prefix}PrimitiveColors.Grey50,      // ~Neutral99
    onSurface          = {Prefix}PrimitiveColors.Grey900,     // ~Neutral10
    surfaceVariant     = {Prefix}PrimitiveColors.Grey100,     // ~NeutralVariant90
    onSurfaceVariant   = {Prefix}PrimitiveColors.Grey700,     // ~NeutralVariant30
    surfaceContainer   = {Prefix}PrimitiveColors.Grey200,     // ~Neutral94
    surfaceContainerHigh = {Prefix}PrimitiveColors.Grey200,   // ~Neutral92
    outline            = {Prefix}PrimitiveColors.Grey500,     // ~NeutralVariant50
    outlineVariant     = {Prefix}PrimitiveColors.Grey200,     // ~NeutralVariant80
    scrim              = {Prefix}PrimitiveColors.Black,
    disabled           = {Prefix}PrimitiveColors.Grey100,     // ~Neutral90
    onDisabled         = {Prefix}PrimitiveColors.Grey600,     // ~Neutral40
)
```

## CompositionLocal

```kotlin
val Local{Prefix}Colors = staticCompositionLocalOf { dark{Prefix}Colors }
```

> NOTE: NO game-specific tokens (correct/incorrect/milestone/safeHaven) here.
> Those belong in `domain-extension-tokens.md` as project-specific extensions.
