# Snackbar Tokens

Token class for snackbar component styling. Maps to Material 3 surface and action semantics.

```kotlin
@Immutable
data class {Prefix}SnackbarTokens(
    val containerColor: Color,         // surfaceContainerHigh
    val containerShape: Shape,         // medium / 8dp
    val containerElevation: Dp,        // medium / 4dp
    val containerPadding: PaddingValues, // 12dp horizontal, 8dp vertical
    val contentColor: Color,           // onSurface
    val messageStyle: TextStyle,       // bodyMedium
    val actionColor: Color,            // primary
    val actionStyle: TextStyle,        // labelMedium
    val bottomOffset: Dp,              // 16dp
    val horizontalPadding: Dp,         // 16dp
    val maxWidth: Dp,                  // 600dp
    val durationShort: Int,            // 4000ms
    val durationLong: Int,             // 8000ms
    val durationIndefinite: Int,       // -1
    val enterDuration: Int,            // 200ms
    val exitDuration: Int,             // 150ms
    val dismissIconSize: Dp,           // 24dp
    val dismissIconColor: Color,       // onSurfaceVariant
)
```

## Default Values

Default values are resolved at runtime from `{Prefix}ColorTokens` and `{Prefix}TypographyTokens` —
not hardcoded to avoid `@Composable` context requirements.

```kotlin
// Call inside your theme provider or pass tokens explicitly
fun default{Prefix}SnackbarTokens(
    colors: {Prefix}ColorTokens,
    typography: {Prefix}TypographyTokens,
): {Prefix}SnackbarTokens = {Prefix}SnackbarTokens(
    containerColor = colors.surfaceContainerHigh,  // semantic: surfaceContainerHigh
    containerShape = RoundedCornerShape(8.dp),
    containerElevation = 4.dp,
    containerPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    contentColor = colors.onSurface,               // semantic: onSurface
    messageStyle = typography.bodyMedium,
    actionColor = colors.primary,                  // semantic: primary
    actionStyle = typography.labelMedium,
    bottomOffset = 16.dp,
    horizontalPadding = 16.dp,
    maxWidth = 600.dp,
    durationShort = 4000,
    durationLong = 8000,
    durationIndefinite = -1,
    enterDuration = 200,
    exitDuration = 150,
    dismissIconSize = 24.dp,
    dismissIconColor = colors.onSurfaceVariant,    // semantic: onSurfaceVariant
)
```

## Variants

Each variant overrides `containerColor` + `contentColor`:

| Variant | containerColor         | contentColor             |
|---------|------------------------|--------------------------|
| default | surfaceContainerHigh   | onSurface                |
| info    | secondaryContainer     | onSecondaryContainer     |
| success | Color(0xFF1B5E20) tonal | Color(0xFFE8F5E9)       |
| warning | tertiaryContainer      | onTertiaryContainer      |
| error   | errorContainer         | onErrorContainer         |

## State Matrix

| State    | containerColor       | actionColor         | dismissIconColor    |
|----------|----------------------|---------------------|---------------------|
| default  | surfaceContainerHigh | primary             | onSurfaceVariant    |
| hovered  | surfaceContainerHigh | primary.copy(0.92f) | onSurfaceVariant    |
| pressed  | surfaceContainerHigh | primary.copy(0.88f) | onSurface           |
| disabled | n/a (auto-dismiss)   | n/a                 | n/a                 |

## Usage

```kotlin
{Prefix}Snackbar(
    snackbarData = snackbarData,
    tokens = {Prefix}SnackbarDefaults.copy(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ),
)
```
