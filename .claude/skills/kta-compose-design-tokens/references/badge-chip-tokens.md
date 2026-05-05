# Badge and Chip Tokens

Token classes for badge indicators and chip components.

## Badge Tokens

```kotlin
@Immutable
data class {Prefix}BadgeTokens(
    val containerColor: Color,     // error
    val contentColor: Color,       // onError
    val dotSize: Dp,               // 6dp (no count)
    val countSize: Dp,             // 16dp (with count)
    val shape: Shape,              // full / CircleShape
    val textStyle: TextStyle,      // labelSmall
    val containerPadding: Dp,      // 4dp horizontal (count badge only)
)
```

### Badge Defaults

Default values are resolved at runtime from `{Prefix}ColorTokens` and `{Prefix}TypographyTokens` —
not hardcoded to avoid `@Composable` context requirements.

```kotlin
fun default{Prefix}BadgeTokens(
    colors: {Prefix}ColorTokens,
    typography: {Prefix}TypographyTokens,
): {Prefix}BadgeTokens = {Prefix}BadgeTokens(
    containerColor = colors.error,       // semantic: error
    contentColor = colors.onError,       // semantic: onError
    dotSize = 6.dp,
    countSize = 16.dp,
    shape = CircleShape,
    textStyle = typography.labelSmall,
    containerPadding = 4.dp,
)
```

---

## Chip Tokens

```kotlin
@Immutable
data class {Prefix}ChipTokens(
    val containerHeight: Dp,               // 32dp
    val containerShape: Shape,             // small / 8dp
    val labelStyle: TextStyle,             // labelMedium
    val labelColor: Color,                 // onSurfaceVariant
    val iconSize: Dp,                      // 18dp
    val iconColor: Color,                  // onSurfaceVariant
    val borderColor: Color,                // outline (assist/input/suggestion)
    val borderWidth: Dp,                   // 1dp
    val contentPadding: PaddingValues,     // 8dp horizontal, 0dp vertical
    val iconSpacing: Dp,                   // 4dp between icon and label
    // Selected state (filter chip)
    val selectedContainerColor: Color,     // secondaryContainer
    val selectedLabelColor: Color,         // onSecondaryContainer
    val selectedIconColor: Color,          // onSecondaryContainer
    val selectedBorderWidth: Dp,           // 0dp
    val disabledOpacity: Float,            // 0.38f
)
```

### Chip Defaults

```kotlin
fun default{Prefix}ChipTokens(
    colors: {Prefix}ColorTokens,
    typography: {Prefix}TypographyTokens,
): {Prefix}ChipTokens = {Prefix}ChipTokens(
    containerHeight = 32.dp,
    containerShape = RoundedCornerShape(8.dp),
    labelStyle = typography.labelMedium,
    labelColor = colors.onSurfaceVariant,               // semantic: onSurfaceVariant
    iconSize = 18.dp,
    iconColor = colors.onSurfaceVariant,                // semantic: onSurfaceVariant
    borderColor = colors.outline,                       // semantic: outline
    borderWidth = 1.dp,
    contentPadding = PaddingValues(horizontal = 8.dp),
    iconSpacing = 4.dp,
    selectedContainerColor = colors.secondaryContainer, // semantic: secondaryContainer
    selectedLabelColor = colors.onSecondaryContainer,   // semantic: onSecondaryContainer
    selectedIconColor = colors.onSecondaryContainer,    // semantic: onSecondaryContainer
    selectedBorderWidth = 0.dp,
    disabledOpacity = 0.38f,
)
```

## Chip Variants

| Variant    | containerColor      | borderColor | selectedContainerColor  |
|------------|---------------------|-------------|-------------------------|
| assist     | transparent         | outline     | n/a                     |
| filter     | transparent         | outline     | secondaryContainer      |
| input      | transparent         | outline     | secondaryContainer      |
| suggestion | transparent         | outline     | n/a                     |

## State Matrix (Chip)

| State     | containerColor       | labelColor           | borderColor        |
|-----------|----------------------|----------------------|--------------------|
| default   | transparent          | onSurfaceVariant     | outline            |
| selected  | secondaryContainer   | onSecondaryContainer | none               |
| pressed   | secondaryContainer+12%| onSecondaryContainer| none               |
| disabled  | transparent          | onSurface 0.38f      | onSurface 0.12f    |

## Usage

```kotlin
// Badge on icon
BadgedBox(badge = { {Prefix}Badge(count = unreadCount, tokens = {Prefix}BadgeDefaults) }) {
    Icon(Icons.Default.Notifications, contentDescription = null)
}

// Filter chip
{Prefix}FilterChip(
    selected = isSelected,
    onClick = { isSelected = !isSelected },
    label = { Text("Kotlin") },
    tokens = {Prefix}ChipDefaults,
)
```
