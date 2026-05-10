# Top Bar Tokens

Token class for top app bar styling. Covers small, medium, and large variants with scroll behavior.

```kotlin
@Immutable
data class {Prefix}TopBarTokens(
    val containerColor: Color,             // surface
    val containerHeight: Dp,               // 64dp small / 112dp medium / 152dp large
    val containerElevation: Dp,            // 0dp default
    val titleStyle: TextStyle,             // titleLarge (small) / headlineMedium (large)
    val titleColor: Color,                 // onSurface
    val titleCenteredAlignment: Boolean,   // false (small) / true (centerAligned)
    val subtitleStyle: TextStyle,          // bodyMedium (medium/large only)
    val subtitleColor: Color,              // onSurfaceVariant
    val navigationIconSize: Dp,            // 24dp
    val navigationIconColor: Color,        // onSurface
    val navigationIconPadding: Dp,         // 4dp touch target padding
    val actionIconSize: Dp,                // 24dp
    val actionIconColor: Color,            // onSurfaceVariant
    val actionIconPadding: Dp,             // 4dp touch target padding
    val scrolledContainerColor: Color,     // surfaceContainer
    val scrolledElevation: Dp,             // low / 2dp
    val scrolledTitleOpacity: Float,       // 1f (small stays) / animates in (medium/large)
    val collapseDuration: Int,             // 200ms
)
```

## Default Values

Default values are resolved at runtime from `{Prefix}ColorTokens` and `{Prefix}TypographyTokens` —
not hardcoded to avoid `@Composable` context requirements.

```kotlin
fun default{Prefix}TopBarTokens(
    colors: {Prefix}ColorTokens,
    typography: {Prefix}TypographyTokens,
): {Prefix}TopBarTokens = {Prefix}TopBarTokens(
    containerColor = colors.surface,                    // semantic: surface
    containerHeight = 64.dp,
    containerElevation = 0.dp,
    titleStyle = typography.titleLarge,
    titleColor = colors.onSurface,                      // semantic: onSurface
    titleCenteredAlignment = false,
    subtitleStyle = typography.bodyMedium,
    subtitleColor = colors.onSurfaceVariant,            // semantic: onSurfaceVariant
    navigationIconSize = 24.dp,
    navigationIconColor = colors.onSurface,             // semantic: onSurface
    navigationIconPadding = 4.dp,
    actionIconSize = 24.dp,
    actionIconColor = colors.onSurfaceVariant,          // semantic: onSurfaceVariant
    actionIconPadding = 4.dp,
    scrolledContainerColor = colors.surfaceContainer,   // semantic: surfaceContainer
    scrolledElevation = 2.dp,
    scrolledTitleOpacity = 1f,
    collapseDuration = 200,
)
```

## Variants

| Variant       | containerHeight | titleStyle      | centered | subtitleVisible |
|---------------|-----------------|-----------------|----------|-----------------|
| small         | 64dp            | titleLarge      | false    | no              |
| centerAligned | 64dp            | titleLarge      | true     | no              |
| medium        | 112dp           | headlineSmall   | false    | yes             |
| large         | 152dp           | headlineMedium  | false    | yes             |

```kotlin
// Large variant helper
fun large{Prefix}TopBarTokens(colors: {Prefix}ColorTokens, typography: {Prefix}TypographyTokens) =
    default{Prefix}TopBarTokens(colors, typography).copy(
        containerHeight = 152.dp,
        titleStyle = typography.headlineMedium,
    )
```

## State Matrix

| State      | containerColor    | elevation | titleColor      |
|------------|-------------------|-----------|-----------------|
| default    | surface           | 0dp       | onSurface       |
| scrolled   | surfaceContainer  | 2dp       | onSurface       |
| collapsed  | surfaceContainer  | 2dp       | onSurface       |
| pinned     | surface           | 0dp       | onSurface       |

## Usage

```kotlin
{Prefix}TopBar(
    title = { Text("Settings") },
    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
    tokens = {Prefix}TopBarDefaults,
    scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
)
```
