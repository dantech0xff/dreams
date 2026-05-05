# Bottom Navigation Tokens

Token class for bottom navigation bar styling. Covers standard, floating, and sticky variants.

```kotlin
@Immutable
data class {Prefix}BottomNavTokens(
    // Container
    val containerColor: Color,             // surfaceContainer
    val containerElevation: Dp,            // 0dp tonal
    val containerHeight: Dp,              // 80dp (with labels) / 56dp (icons only)
    // Floating variant
    val floatingMarginHorizontal: Dp,      // 16dp
    val floatingMarginBottom: Dp,          // 16dp
    val floatingShape: Shape,              // full / pill
    val floatingBlurRadius: Dp,            // 10dp
    val floatingBorderColor: Color,        // White at 0.1 alpha
    val floatingBorderWidth: Dp,           // 1dp
    // Sticky variant
    val stickyDividerColor: Color,         // outlineVariant
    val stickyDividerHeight: Dp,           // 0.5dp
    // Item
    val itemIconSize: Dp,                  // 24dp
    val itemLabelStyle: TextStyle,         // labelSmall
    val itemColorUnselected: Color,        // onSurfaceVariant
    val itemColorSelected: Color,          // onSecondaryContainer
    val itemSpacing: Dp,                   // 4dp between icon and label
    // Indicator
    val indicatorColor: Color,             // secondaryContainer
    val indicatorShape: Shape,             // full / pill
    val indicatorHeight: Dp,               // 32dp
    val indicatorWidth: Dp,                // 64dp
    val indicatorAnimDuration: Int,        // 200ms
    // Badge
    val badgeColor: Color,                 // error
    val badgeTextColor: Color,             // onError
    val badgeDotSize: Dp,                  // 6dp (no count)
    val badgeCountSize: Dp,                // 16dp (with count)
)
```

## Default Values

Default values are resolved at runtime from `{Prefix}ColorTokens` and `{Prefix}TypographyTokens` —
not hardcoded to avoid `@Composable` context requirements.

```kotlin
fun default{Prefix}BottomNavTokens(
    colors: {Prefix}ColorTokens,
    typography: {Prefix}TypographyTokens,
): {Prefix}BottomNavTokens = {Prefix}BottomNavTokens(
    containerColor = colors.surfaceContainer,           // semantic: surfaceContainer
    containerElevation = 0.dp,
    containerHeight = 80.dp,
    floatingMarginHorizontal = 16.dp,
    floatingMarginBottom = 16.dp,
    floatingShape = RoundedCornerShape(50),
    floatingBlurRadius = 10.dp,
    floatingBorderColor = Color.White.copy(alpha = 0.1f),
    floatingBorderWidth = 1.dp,
    stickyDividerColor = colors.outlineVariant,         // semantic: outlineVariant
    stickyDividerHeight = 0.5.dp,
    itemIconSize = 24.dp,
    itemLabelStyle = typography.labelSmall,
    itemColorUnselected = colors.onSurfaceVariant,      // semantic: onSurfaceVariant
    itemColorSelected = colors.onSecondaryContainer,    // semantic: onSecondaryContainer
    itemSpacing = 4.dp,
    indicatorColor = colors.secondaryContainer,         // semantic: secondaryContainer
    indicatorShape = RoundedCornerShape(50),
    indicatorHeight = 32.dp,
    indicatorWidth = 64.dp,
    indicatorAnimDuration = 200,
    badgeColor = colors.error,                          // semantic: error
    badgeTextColor = colors.onError,                    // semantic: onError
    badgeDotSize = 6.dp,
    badgeCountSize = 16.dp,
)
```

## Variants

| Variant  | containerColor    | elevation | shape    | Notes                     |
|----------|-------------------|-----------|----------|---------------------------|
| standard | surfaceContainer  | 0dp tonal | none     | full-width, docked        |
| floating | surfaceContainer  | medium    | pill     | margin from screen edges  |
| sticky   | surfaceContainer  | 0dp       | none     | top divider line          |

## State Matrix (per item)

| State     | iconColor           | labelColor          | indicatorColor      |
|-----------|---------------------|---------------------|---------------------|
| unselected| onSurfaceVariant    | onSurfaceVariant    | transparent         |
| selected  | onSecondaryContainer| onSecondaryContainer| secondaryContainer  |
| pressed   | onSecondaryContainer| onSecondaryContainer| secondaryContainer 0.88f |
| disabled  | onSurface 0.38f     | onSurface 0.38f     | transparent         |

## Usage

```kotlin
{Prefix}BottomNavBar(
    selectedIndex = currentTab,
    tokens = {Prefix}BottomNavDefaults.copy(containerHeight = 56.dp),
    items = navItems,
    onItemSelected = { currentTab = it },
)
```
