# Card Tokens

Token class for card component styling. Covers filled, outlined, and elevated variants.

```kotlin
@Immutable
data class {Prefix}CardTokens(
    val containerColor: Color,         // surfaceContainerLow
    val containerShape: Shape,         // medium / 12dp
    val containerElevation: Dp,        // low / 1dp
    val contentPadding: Dp,            // 16dp all sides
    val borderColor: Color,            // transparent (outlined: outline)
    val borderWidth: Dp,               // 0dp (outlined: 1dp)
    // Header
    val headerTitleStyle: TextStyle,   // titleMedium
    val headerTitleColor: Color,       // onSurface
    val headerSubtitleStyle: TextStyle,// bodyMedium
    val headerSubtitleColor: Color,    // onSurfaceVariant
    val headerSpacing: Dp,             // 4dp between title and subtitle
    // Body
    val bodyStyle: TextStyle,          // bodyMedium
    val bodyColor: Color,              // onSurfaceVariant
    // Media
    val mediaShape: Shape,             // zero (clips to card shape at top)
)
```

## Default Values

Default values are resolved at runtime from `{Prefix}ColorTokens` and `{Prefix}TypographyTokens` —
not hardcoded to avoid `@Composable` context requirements.

```kotlin
fun default{Prefix}CardTokens(
    colors: {Prefix}ColorTokens,
    typography: {Prefix}TypographyTokens,
): {Prefix}CardTokens = {Prefix}CardTokens(
    containerColor = colors.surfaceContainerLow,        // semantic: surfaceContainerLow
    containerShape = RoundedCornerShape(12.dp),
    containerElevation = 1.dp,
    contentPadding = 16.dp,
    borderColor = Color.Transparent,
    borderWidth = 0.dp,
    headerTitleStyle = typography.titleMedium,
    headerTitleColor = colors.onSurface,                // semantic: onSurface
    headerSubtitleStyle = typography.bodyMedium,
    headerSubtitleColor = colors.onSurfaceVariant,      // semantic: onSurfaceVariant
    headerSpacing = 4.dp,
    bodyStyle = typography.bodyMedium,
    bodyColor = colors.onSurfaceVariant,                // semantic: onSurfaceVariant
    mediaShape = RectangleShape,
)
```

## Variants

| Variant  | containerColor        | elevation | borderColor  | borderWidth |
|----------|-----------------------|-----------|--------------|-------------|
| filled   | surfaceContainerLow   | 1dp       | transparent  | 0dp         |
| outlined | surface               | 0dp       | outline      | 1dp         |
| elevated | surfaceContainerLow   | 2dp       | transparent  | 0dp         |

```kotlin
// Variant helpers — also accept colors parameter at call site
fun outlined{Prefix}CardTokens(colors: {Prefix}ColorTokens, typography: {Prefix}TypographyTokens) =
    default{Prefix}CardTokens(colors, typography).copy(
        containerColor = colors.surface,    // semantic: surface
        containerElevation = 0.dp,
        borderColor = colors.outline,       // semantic: outline
        borderWidth = 1.dp,
    )

fun elevated{Prefix}CardTokens(colors: {Prefix}ColorTokens, typography: {Prefix}TypographyTokens) =
    default{Prefix}CardTokens(colors, typography).copy(containerElevation = 2.dp)
```

## State Matrix

| State    | containerColor          | elevation           |
|----------|-------------------------|---------------------|
| default  | surfaceContainerLow     | 1dp                 |
| hovered  | surfaceContainerLow+8%  | 2dp                 |
| pressed  | surfaceContainerLow+12% | 1dp                 |
| dragging | surfaceContainerLow     | 8dp                 |
| disabled | surfaceContainerLow     | 0dp, alpha 0.38f    |

## Usage

```kotlin
{Prefix}Card(
    tokens = {Prefix}CardOutlinedDefaults,
    onClick = { navigateToDetail(item.id) },
) {
    {Prefix}CardHeader(title = item.title, subtitle = item.subtitle)
}
```
