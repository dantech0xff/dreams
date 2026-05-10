# Skeleton Tokens

Token class for skeleton/shimmer loading placeholder styling.

```kotlin
@Immutable
data class {Prefix}SkeletonTokens(
    val baseColor: Color,          // surfaceContainerHighest
    val highlightColor: Color,     // surfaceContainer
    val shape: Shape,              // medium / 8dp
    val shimmerDuration: Int,      // 1500ms per cycle
    val shimmerDelay: Int,         // 200ms initial delay
    val shimmerAngle: Float,       // 20f degrees
)
```

## Default Values

Default values are resolved at runtime from `{Prefix}ColorTokens` —
not hardcoded to avoid `@Composable` context requirements.

```kotlin
fun default{Prefix}SkeletonTokens(
    colors: {Prefix}ColorTokens,
): {Prefix}SkeletonTokens = {Prefix}SkeletonTokens(
    baseColor = colors.surfaceContainerHighest,  // semantic: surfaceContainerHighest
    highlightColor = colors.surfaceContainer,    // semantic: surfaceContainer
    shape = RoundedCornerShape(8.dp),
    shimmerDuration = 1500,
    shimmerDelay = 200,
    shimmerAngle = 20f,
)
```

## Variants

| Variant     | height  | shape        | Typical use               |
|-------------|---------|--------------|---------------------------|
| text        | 16dp    | small / 4dp  | body text lines           |
| circular    | dynamic | full         | avatars, icons            |
| rectangular | dynamic | medium / 8dp | images, cards, banners    |
| rounded     | dynamic | large / 16dp | pill buttons, chips       |

```kotlin
fun text{Prefix}SkeletonTokens(colors: {Prefix}ColorTokens) =
    default{Prefix}SkeletonTokens(colors).copy(shape = RoundedCornerShape(4.dp))
fun circular{Prefix}SkeletonTokens(colors: {Prefix}ColorTokens) =
    default{Prefix}SkeletonTokens(colors).copy(shape = CircleShape)
fun rounded{Prefix}SkeletonTokens(colors: {Prefix}ColorTokens) =
    default{Prefix}SkeletonTokens(colors).copy(shape = RoundedCornerShape(16.dp))
```

## Shimmer Implementation

```kotlin
@Composable
fun {Prefix}ShimmerBrush(tokens: {Prefix}SkeletonTokens = {Prefix}SkeletonDefaults): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = tokens.shimmerDuration, delayMillis = tokens.shimmerDelay),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerTranslate",
    )
    return Brush.linearGradient(
        colors = listOf(tokens.baseColor, tokens.highlightColor, tokens.baseColor),
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 0f),
    )
}
```

## Usage

```kotlin
// Text line skeleton
Box(
    modifier = Modifier
        .fillMaxWidth(0.7f)
        .height(16.dp)
        .clip({Prefix}SkeletonTextDefaults.shape)
        .background({Prefix}ShimmerBrush({Prefix}SkeletonTextDefaults))
)

// Card skeleton with avatar + lines
{Prefix}SkeletonCard(
    tokens = {Prefix}SkeletonDefaults,
    lines = 3,
    showAvatar = true,
)
```
