# Motion Tokens

Animation duration and easing tokens for Compose transitions.

## Primitive Layer

```kotlin
object {Prefix}PrimitiveMotion {
    // Duration (milliseconds)
    val DurationInstant = 0
    val DurationFast = 100
    val DurationNormal = 200
    val DurationSlow = 300
    val DurationSlower = 400
    val DurationSlowest = 500

    // Easing curves (M3 spec)
    val EaseLinear = LinearEasing
    val EaseIn = CubicBezierEasing(0.4f, 0f, 1f, 1f)        // accelerate into target
    val EaseOut = CubicBezierEasing(0f, 0f, 0.2f, 1f)       // decelerate from source
    val EaseInOut = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)   // standard M3
    val EaseEmphasis = CubicBezierEasing(0.2f, 0f, 0f, 1f)  // M3 emphasized
}
```

## Semantic Layer

```kotlin
@Immutable
data class {Prefix}MotionTokens(
    val durationShort: Int,      // micro-interactions (100ms)
    val durationMedium: Int,     // standard transitions (200ms)
    val durationLong: Int,       // complex animations (300ms)
    val durationEmphasis: Int,   // attention-grabbing (400ms)
    val easeStandard: Easing,    // general purpose (EaseInOut)
    val easeDecelerate: Easing,  // entering elements (EaseOut)
    val easeAccelerate: Easing,  // exiting elements (EaseIn)
)

val default{Prefix}Motion = {Prefix}MotionTokens(
    durationShort = {Prefix}PrimitiveMotion.DurationFast,
    durationMedium = {Prefix}PrimitiveMotion.DurationNormal,
    durationLong = {Prefix}PrimitiveMotion.DurationSlow,
    durationEmphasis = {Prefix}PrimitiveMotion.DurationSlower,
    easeStandard = {Prefix}PrimitiveMotion.EaseInOut,
    easeDecelerate = {Prefix}PrimitiveMotion.EaseOut,
    easeAccelerate = {Prefix}PrimitiveMotion.EaseIn,
)
```

## Usage

```kotlin
// Fade in animation
val alpha by animateFloatAsState(
    targetValue = if (visible) 1f else 0f,
    animationSpec = tween(
        durationMillis = {Prefix}Theme.motion.durationMedium,
        easing = {Prefix}Theme.motion.easeDecelerate,
    )
)

// Slide + fade enter transition
AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn(
        animationSpec = tween({Prefix}Theme.motion.durationLong, easing = {Prefix}Theme.motion.easeDecelerate)
    ) + slideInVertically(),
    exit = fadeOut(
        animationSpec = tween({Prefix}Theme.motion.durationShort, easing = {Prefix}Theme.motion.easeAccelerate)
    )
)
```

## Easing Guide

| Easing | Use For | M3 Name |
|--------|---------|---------|
| `easeStandard` | General transitions | Standard |
| `easeDecelerate` | Elements entering screen | Decelerate |
| `easeAccelerate` | Elements leaving screen | Accelerate |
| `EaseEmphasis` | Hero animations, attention | Emphasized |
| `EaseLinear` | Progress indicators, loading | Linear |

## Common Durations

| Duration | Use For |
|----------|---------|
| Short (100ms) | Ripple, state change, toggle |
| Medium (200ms) | Fade, color change, expand |
| Long (300ms) | Slide, modal enter, page transition |
| Emphasis (400ms) | Shared element, hero animation |
