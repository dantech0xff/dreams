# Bottom Sheet Tokens

Token class for modal and persistent bottom sheet styling. Maps to Material 3 surface semantics.

```kotlin
@Immutable
data class {Prefix}BottomSheetTokens(
    val containerColor: Color,           // surfaceContainerLow
    val containerShape: Shape,           // top corners extraLarge / 28dp
    val containerElevation: Dp,          // low / 1dp
    val containerMaxWidth: Dp,           // 640dp
    val dragHandleColor: Color,          // onSurfaceVariant at 0.4 alpha
    val dragHandleWidth: Dp,             // 32dp
    val dragHandleHeight: Dp,            // 4dp
    val dragHandleShape: Shape,          // full / CircleShape
    val dragHandleTopPadding: Dp,        // 22dp
    val dragHandleBottomPadding: Dp,     // 22dp
    val contentPadding: Dp,             // 16dp horizontal
    val contentBottomPadding: Dp,        // navBar inset + 16dp
    val scrimColor: Color,               // scrim
    val scrimOpacity: Float,             // 0.32f
    val peekHeight: Dp,                  // 56dp
    val halfExpandedRatio: Float,        // 0.5f
    val enterDuration: Int,              // 300ms
    val exitDuration: Int,               // 250ms
)
```

## Default Values

Default values are resolved at runtime from `{Prefix}ColorTokens` —
not hardcoded to avoid `@Composable` context requirements.

```kotlin
fun default{Prefix}BottomSheetTokens(
    colors: {Prefix}ColorTokens,
): {Prefix}BottomSheetTokens = {Prefix}BottomSheetTokens(
    containerColor = colors.surfaceContainerLow,                      // semantic: surfaceContainerLow
    containerShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    containerElevation = 1.dp,
    containerMaxWidth = 640.dp,
    dragHandleColor = colors.onSurfaceVariant.copy(alpha = 0.4f),     // semantic: onSurfaceVariant
    dragHandleWidth = 32.dp,
    dragHandleHeight = 4.dp,
    dragHandleShape = CircleShape,
    dragHandleTopPadding = 22.dp,
    dragHandleBottomPadding = 22.dp,
    contentPadding = 16.dp,
    contentBottomPadding = 16.dp, // add WindowInsets.navigationBars at call site
    scrimColor = colors.scrim,                                         // semantic: scrim
    scrimOpacity = 0.32f,
    peekHeight = 56.dp,
    halfExpandedRatio = 0.5f,
    enterDuration = 300,
    exitDuration = 250,
)
```

## Variants

| Variant    | scrimOpacity | containerElevation | Notes                          |
|------------|--------------|--------------------|--------------------------------|
| modal      | 0.32f        | 1dp                | blocks interaction with scrim  |
| persistent | 0f           | 0dp tonal          | no scrim, content shifts up    |

## State Matrix

| State        | containerColor      | dragHandleColor               | scrimOpacity  |
|--------------|---------------------|-------------------------------|---------------|
| hidden       | surfaceContainerLow | onSurfaceVariant 0.4          | 0f            |
| peeked       | surfaceContainerLow | onSurfaceVariant 0.4          | 0.16f         |
| halfExpanded | surfaceContainerLow | onSurfaceVariant 0.4          | 0.24f         |
| expanded     | surfaceContainerLow | onSurfaceVariant 0.4          | 0.32f         |
| dragging     | surfaceContainerLow | onSurfaceVariant 0.6 (active) | interpolated  |

## Usage

```kotlin
{Prefix}ModalBottomSheet(
    onDismissRequest = { showSheet = false },
    tokens = {Prefix}BottomSheetDefaults.copy(peekHeight = 120.dp),
) {
    SheetContent()
}
```
