# CompositionLocal Wiring & Theme Provider

## All CompositionLocal Declarations

```kotlin
// Semantic locals — staticCompositionLocalOf (never change after initial provide)
val Local{Prefix}Colors    = staticCompositionLocalOf { dark{Prefix}Colors }
val Local{Prefix}Spacing   = staticCompositionLocalOf { default{Prefix}Spacing }
val Local{Prefix}Shapes    = staticCompositionLocalOf { default{Prefix}Shapes }
val Local{Prefix}Elevation = staticCompositionLocalOf { default{Prefix}Elevation }
val Local{Prefix}Typography = staticCompositionLocalOf { default{Prefix}Typography }
val Local{Prefix}Motion    = staticCompositionLocalOf { default{Prefix}Motion }
val Local{Prefix}Opacity   = staticCompositionLocalOf { default{Prefix}Opacity }

// Component locals — compositionLocalOf (may change at subtree level, OPT-IN)
// NOT included in main theme by default; add only when per-component overrides needed
val Local{Prefix}ButtonTokens = compositionLocalOf { default{Prefix}ButtonTokens }
val Local{Prefix}CardTokens   = compositionLocalOf { default{Prefix}CardTokens }
```

## M3 Bridge

```kotlin
fun {Prefix}ColorTokens.toMaterial3ColorScheme(isDark: Boolean): ColorScheme =
    if (isDark) darkColorScheme(
        primary            = primary,
        onPrimary          = onPrimary,
        primaryContainer   = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary          = secondary,
        onSecondary        = onSecondary,
        tertiary           = tertiary,
        onTertiary         = onTertiary,
        background         = background,
        onBackground       = onBackground,
        surface            = surface,
        onSurface          = onSurface,
        surfaceVariant     = surfaceVariant,
        onSurfaceVariant   = onSurfaceVariant,
        outline            = outline,
        outlineVariant     = outlineVariant,
        scrim              = scrim,
        error              = error,
        onError            = onError,
    ) else lightColorScheme(
        primary            = primary,
        onPrimary          = onPrimary,
        primaryContainer   = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary          = secondary,
        onSecondary        = onSecondary,
        tertiary           = tertiary,
        onTertiary         = onTertiary,
        background         = background,
        onBackground       = onBackground,
        surface            = surface,
        onSurface          = onSurface,
        surfaceVariant     = surfaceVariant,
        onSurfaceVariant   = onSurfaceVariant,
        outline            = outline,
        outlineVariant     = outlineVariant,
        scrim              = scrim,
        error              = error,
        onError            = onError,
    )
```

## Theme Composable

```kotlin
@Composable
fun {Prefix}Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) dark{Prefix}Colors else light{Prefix}Colors
    CompositionLocalProvider(
        Local{Prefix}Colors     provides colors,
        Local{Prefix}Spacing    provides default{Prefix}Spacing,
        Local{Prefix}Shapes     provides default{Prefix}Shapes,
        Local{Prefix}Elevation  provides default{Prefix}Elevation,
        Local{Prefix}Typography provides default{Prefix}Typography,
        Local{Prefix}Motion     provides default{Prefix}Motion,
        Local{Prefix}Opacity    provides default{Prefix}Opacity,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterial3ColorScheme(darkTheme),
            content = content,
        )
    }
}
```

## Theme Object Accessor

```kotlin
// NO @ReadOnlyComposable — locals may change in subtrees
object {Prefix}Theme {
    val colors: {Prefix}ColorTokens
        @Composable get() = Local{Prefix}Colors.current
    val spacing: {Prefix}SpacingTokens
        @Composable get() = Local{Prefix}Spacing.current
    val shapes: {Prefix}ShapeTokens
        @Composable get() = Local{Prefix}Shapes.current
    val elevation: {Prefix}ElevationTokens
        @Composable get() = Local{Prefix}Elevation.current
    val typography: {Prefix}TypographyTokens
        @Composable get() = Local{Prefix}Typography.current
    val motion: {Prefix}MotionTokens
        @Composable get() = Local{Prefix}Motion.current
    val opacity: {Prefix}OpacityTokens
        @Composable get() = Local{Prefix}Opacity.current
}
```

## Per-Component Token Override (OPT-IN)

```kotlin
// Override button tokens for a single subtree only
CompositionLocalProvider(
    Local{Prefix}ButtonTokens provides {Prefix}ButtonTokens(
        containerColor = {Prefix}Theme.colors.primaryContainer,
        contentColor   = {Prefix}Theme.colors.onPrimaryContainer,
        shape          = {Prefix}Theme.shapes.full,
    )
) {
    {Prefix}Button(onClick = {}) { Text("Custom") }
}
```

## Preview

```kotlin
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Preview(showBackground = true)
@Composable
private fun {Prefix}ThemePreview() {
    {Prefix}Theme {
        Surface(color = {Prefix}Theme.colors.background) {
            Text(
                text = "Hello {Prefix}Theme",
                style = {Prefix}Theme.typography.headlineMedium,
                color = {Prefix}Theme.colors.onBackground,
            )
        }
    }
}
```
