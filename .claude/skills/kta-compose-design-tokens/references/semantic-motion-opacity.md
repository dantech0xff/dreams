# Semantic Motion & Opacity Tokens

## Motion Tokens

```kotlin
@Immutable
data class {Prefix}MotionTokens(
    // Durations (milliseconds)
    val durationShort: Int,      // 100 — micro feedback (button press)
    val durationMedium: Int,     // 200 — standard transitions (fade, slide)
    val durationLong: Int,       // 300 — complex transitions (dialog open)
    val durationEmphasis: Int,   // 400 — attention-drawing animations (success)
    // Easing curves
    val easeStandard: Easing,    // FastOutSlowInEasing — enter/exit same screen
    val easeDecelerate: Easing,  // LinearOutSlowInEasing — elements entering screen
    val easeAccelerate: Easing,  // FastOutLinearInEasing — elements leaving screen
)

val default{Prefix}Motion = {Prefix}MotionTokens(
    durationShort    = 100,
    durationMedium   = 200,
    durationLong     = 300,
    durationEmphasis = 400,
    easeStandard     = FastOutSlowInEasing,
    easeDecelerate   = LinearOutSlowInEasing,
    easeAccelerate   = FastOutLinearInEasing,
)

val Local{Prefix}Motion = staticCompositionLocalOf { default{Prefix}Motion }
```

## Opacity Tokens

```kotlin
@Immutable
data class {Prefix}OpacityTokens(
    val disabled: Float,  // 0.38f — disabled controls (MD3 spec)
    val scrim: Float,     // 0.32f — modal backdrop scrim
    val hover: Float,     // 0.08f — hover state overlay
    val pressed: Float,   // 0.12f — pressed state overlay
    val dragged: Float,   // 0.16f — dragged state overlay
)

val default{Prefix}Opacity = {Prefix}OpacityTokens(
    disabled = 0.38f,
    scrim    = 0.32f,
    hover    = 0.08f,
    pressed  = 0.12f,
    dragged  = 0.16f,
)

val Local{Prefix}Opacity = staticCompositionLocalOf { default{Prefix}Opacity }
```

## Usage Examples

```kotlin
// Animate a dialog with motion tokens
val motion = {Prefix}Theme.motion
AnimatedVisibility(
    visible = visible,
    enter = fadeIn(
        animationSpec = tween(
            durationMillis = motion.durationLong,
            easing = motion.easeDecelerate,
        )
    ) + scaleIn(initialScale = 0.92f),
    exit = fadeOut(
        animationSpec = tween(
            durationMillis = motion.durationMedium,
            easing = motion.easeAccelerate,
        )
    ),
) { DialogContent() }

// Apply disabled opacity
val opacity = {Prefix}Theme.opacity
Box(
    modifier = Modifier.alpha(
        if (enabled) 1f else opacity.disabled
    )
)

// Scrim overlay
Box(
    modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = opacity.scrim))
        .clickable(onClick = onDismiss)
)
```
