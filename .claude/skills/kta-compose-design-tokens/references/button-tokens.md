# Button Tokens

Token class for button component styling. Covers filled, outlined, text, and tonal variants.

```kotlin
@Immutable
data class {Prefix}ButtonTokens(
    // Base
    val minHeight: Dp,                     // 48dp
    val minWidth: Dp,                      // 48dp
    val shape: Shape,                      // medium / 50% pill
    val contentPadding: PaddingValues,     // 16dp horizontal, 8dp vertical
    val iconSize: Dp,                      // 18dp
    val iconSpacing: Dp,                   // 8dp between icon and label
    val labelStyle: TextStyle,             // labelLarge
    val disabledOpacity: Float,            // 0.38f
    // Filled variant
    val filledContainerColor: Color,       // primary
    val filledContentColor: Color,         // onPrimary
    val filledDisabledContainerColor: Color, // onSurface 0.12f
    val filledDisabledContentColor: Color, // onSurface 0.38f
    // Outlined variant
    val outlinedBorderColor: Color,        // outline
    val outlinedBorderWidth: Dp,           // 1dp
    val outlinedContentColor: Color,       // primary
    val outlinedDisabledBorderColor: Color,// onSurface 0.12f
    // Text / ghost variant
    val textContentColor: Color,           // primary
    // Tonal variant
    val tonalContainerColor: Color,        // secondaryContainer
    val tonalContentColor: Color,          // onSecondaryContainer
)
```

## Default Values

Default values are resolved at runtime from `{Prefix}ColorTokens` and `{Prefix}TypographyTokens` —
not hardcoded to avoid `@Composable` context requirements.

```kotlin
fun default{Prefix}ButtonTokens(
    colors: {Prefix}ColorTokens,
    typography: {Prefix}TypographyTokens,
): {Prefix}ButtonTokens = {Prefix}ButtonTokens(
    minHeight = 48.dp,
    minWidth = 48.dp,
    shape = RoundedCornerShape(50),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    iconSize = 18.dp,
    iconSpacing = 8.dp,
    labelStyle = typography.labelLarge,
    disabledOpacity = 0.38f,
    filledContainerColor = colors.primary,                      // semantic: primary
    filledContentColor = colors.onPrimary,                      // semantic: onPrimary
    filledDisabledContainerColor = colors.onSurface.copy(0.12f),// semantic: onSurface
    filledDisabledContentColor = colors.onSurface.copy(0.38f),  // semantic: onSurface
    outlinedBorderColor = colors.outline,                       // semantic: outline
    outlinedBorderWidth = 1.dp,
    outlinedContentColor = colors.primary,                      // semantic: primary
    outlinedDisabledBorderColor = colors.onSurface.copy(0.12f), // semantic: onSurface
    textContentColor = colors.primary,                          // semantic: primary
    tonalContainerColor = colors.secondaryContainer,            // semantic: secondaryContainer
    tonalContentColor = colors.onSecondaryContainer,            // semantic: onSecondaryContainer
)
```

## State Matrix

### Filled

| State    | containerColor        | contentColor         |
|----------|-----------------------|----------------------|
| default  | primary               | onPrimary            |
| hovered  | primary + 8% white    | onPrimary            |
| pressed  | primary + 12% white   | onPrimary            |
| disabled | onSurface 0.12f       | onSurface 0.38f      |

### Outlined

| State    | borderColor           | contentColor         |
|----------|-----------------------|----------------------|
| default  | outline               | primary              |
| focused  | primary               | primary              |
| pressed  | primary               | primary 0.88f        |
| disabled | onSurface 0.12f       | onSurface 0.38f      |

### Tonal

| State    | containerColor           | contentColor             |
|----------|--------------------------|--------------------------|
| default  | secondaryContainer       | onSecondaryContainer     |
| hovered  | secondaryContainer+8%    | onSecondaryContainer     |
| pressed  | secondaryContainer+12%   | onSecondaryContainer     |
| disabled | onSurface 0.12f          | onSurface 0.38f          |

## Usage

```kotlin
{Prefix}FilledButton(
    onClick = onConfirm,
    tokens = {Prefix}ButtonDefaults.copy(filledContainerColor = MaterialTheme.colorScheme.error),
    label = { Text("Delete") },
)
```
