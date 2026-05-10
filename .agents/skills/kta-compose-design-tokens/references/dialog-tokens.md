# Dialog Tokens

Token class for dialog/modal component styling. Maps to Material 3 surface and scrim semantics.

```kotlin
@Immutable
data class {Prefix}DialogTokens(
    val scrimColor: Color,             // scrim
    val scrimOpacity: Float,           // 0.32f
    val containerColor: Color,         // surfaceContainerHigh
    val containerShape: Shape,         // large / 16dp
    val containerElevation: Dp,        // high / 8dp
    val containerMaxWidth: Dp,         // 560dp
    val containerMinWidth: Dp,         // 280dp
    val containerPadding: Dp,          // 24dp all sides
    val titleStyle: TextStyle,         // headlineMedium
    val titleColor: Color,             // onSurface
    val titleBottomSpacing: Dp,        // 16dp
    val bodyStyle: TextStyle,          // bodyLarge
    val bodyColor: Color,              // onSurfaceVariant
    val actionSpacing: Dp,             // 8dp between actions
    val actionTopSpacing: Dp,          // 24dp above action row
    val actionAlignment: Arrangement.Horizontal, // Arrangement.End
    val iconSize: Dp,                  // 24dp
    val iconColor: Color,              // secondary
    val iconBottomSpacing: Dp,         // 16dp
    val enterDuration: Int,            // 200ms
    val exitDuration: Int,             // 150ms
)
```

## Default Values

Default values are resolved at runtime from `{Prefix}ColorTokens` and `{Prefix}TypographyTokens` —
not hardcoded to avoid `@Composable` context requirements.

```kotlin
fun default{Prefix}DialogTokens(
    colors: {Prefix}ColorTokens,
    typography: {Prefix}TypographyTokens,
): {Prefix}DialogTokens = {Prefix}DialogTokens(
    scrimColor = colors.scrim,                     // semantic: scrim
    scrimOpacity = 0.32f,
    containerColor = colors.surfaceContainerHigh,  // semantic: surfaceContainerHigh
    containerShape = RoundedCornerShape(16.dp),
    containerElevation = 8.dp,
    containerMaxWidth = 560.dp,
    containerMinWidth = 280.dp,
    containerPadding = 24.dp,
    titleStyle = typography.headlineMedium,
    titleColor = colors.onSurface,                 // semantic: onSurface
    titleBottomSpacing = 16.dp,
    bodyStyle = typography.bodyLarge,
    bodyColor = colors.onSurfaceVariant,           // semantic: onSurfaceVariant
    actionSpacing = 8.dp,
    actionTopSpacing = 24.dp,
    actionAlignment = Arrangement.End,
    iconSize = 24.dp,
    iconColor = colors.secondary,                  // semantic: secondary
    iconBottomSpacing = 16.dp,
    enterDuration = 200,
    exitDuration = 150,
)
```

## Variants

| Variant    | containerColor         | iconColor   | Notes                        |
|------------|------------------------|-------------|------------------------------|
| alert      | surfaceContainerHigh   | error       | destructive confirm action   |
| confirm    | surfaceContainerHigh   | primary     | standard two-action          |
| info       | surfaceContainerHigh   | secondary   | read-only, single dismiss    |
| fullscreen | surface                | n/a         | no scrim, fills screen       |

## State Matrix

| State    | scrimOpacity | containerElevation | actionColor         |
|----------|--------------|--------------------|---------------------|
| entering | 0f → 0.32f   | 0dp → 8dp          | primary             |
| visible  | 0.32f        | 8dp                | primary             |
| exiting  | 0.32f → 0f   | 8dp → 0dp          | primary             |
| disabled | 0.32f        | 8dp                | onSurface 0.38f     |

## Usage

```kotlin
{Prefix}Dialog(
    onDismissRequest = { showDialog = false },
    tokens = {Prefix}DialogDefaults.copy(iconColor = MaterialTheme.colorScheme.error),
    title = { Text("Delete item?") },
    confirmButton = { TextButton(onClick = onConfirm) { Text("Delete") } },
)
```
