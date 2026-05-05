# Primitive Typography, Motion & Opacity Reference

Raw type, animation, and alpha tokens — no semantic meaning, no theme awareness.

---

## Typography — `{Prefix}PrimitiveTypography`

### Font Sizes

All values in `sp`. Import `androidx.compose.ui.unit.sp`.

| Token    | Value  |
|----------|--------|
| `Xs`     | 10.sp  |
| `Sm`     | 12.sp  |
| `Md`     | 14.sp  |
| `Lg`     | 16.sp  |
| `Xl`     | 18.sp  |
| `Xxl`    | 20.sp  |
| `Xxxl`   | 24.sp  |
| `Huge`   | 28.sp  |
| `Giant`  | 32.sp  |
| `Display`| 40.sp  |
| `Hero`   | 48.sp  |

### Line Heights

**MUST be `TextUnit` (`.em` or `.sp`) — not `Float`. `Float` will not compile in `TextStyle.lineHeight`.**

| Token         | Value    | Notes              |
|---------------|----------|--------------------|
| `Tight`       | 1.1.em   | Dense/display text |
| `Snug`        | 1.25.em  | Headings           |
| `Normal`      | 1.4.em   | Body default       |
| `Relaxed`     | 1.6.em   | Long-form content  |
| `Loose`       | 1.8.em   | High legibility    |

```kotlin
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

object {Prefix}PrimitiveTypography {
    // Font sizes
    val FontXs      = 10.sp
    val FontSm      = 12.sp
    val FontMd      = 14.sp
    val FontLg      = 16.sp
    val FontXl      = 18.sp
    val FontXxl     = 20.sp
    val FontXxxl    = 24.sp
    val FontHuge    = 28.sp
    val FontGiant   = 32.sp
    val FontDisplay = 40.sp
    val FontHero    = 48.sp

    // Line heights — TextUnit required, NOT Float
    val LineHeightTight   = 1.1.em
    val LineHeightSnug    = 1.25.em
    val LineHeightNormal  = 1.4.em
    val LineHeightRelaxed = 1.6.em
    val LineHeightLoose   = 1.8.em
}
```

---

## Motion — `{Prefix}PrimitiveMotion`

### Durations

All values in milliseconds (`Int`). Use with `tween()`, `spring()`, `animateXAsState`.
Names align with `motion-tokens.md` semantic layer.

| Token              | Value | Use case                     |
|--------------------|-------|------------------------------|
| `DurationInstant`  | 0     | No animation / immediate     |
| `DurationFast`     | 100   | Hover, focus states          |
| `DurationNormal`   | 200   | Default transitions          |
| `DurationSlow`     | 300   | Dialogs, sheets              |
| `DurationSlower`   | 400   | Page transitions             |
| `DurationSlowest`  | 500   | Emphasis / intro animations  |

### Easing Curves

Import `androidx.compose.animation.core.CubicBezierEasing` and `androidx.compose.animation.core.LinearEasing`.

| Token          | Curve                              | Use case                          |
|----------------|------------------------------------|-----------------------------------|
| `EaseLinear`   | `LinearEasing`                     | Progress bars, looping            |
| `EaseIn`       | `CubicBezierEasing(0.4f,0f,1f,1f)` | Accelerate INTO target (exit)     |
| `EaseOut`      | `CubicBezierEasing(0f,0f,0.2f,1f)` | Decelerate OUT from source (enter)|
| `EaseInOut`    | `CubicBezierEasing(0.4f,0f,0.2f,1f)` | Shared axis transitions        |
| `EaseEmphasis` | `CubicBezierEasing(0.2f,0f,0f,1f)` | M3 emphasized — large motion     |

Note: **EaseIn = element accelerates as it moves toward its destination (typically exit/dismiss). EaseOut = element decelerates as it arrives (typically enter/appear). Matches Material 3 spec.**

```kotlin
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing

object {Prefix}PrimitiveMotion {
    // Durations (ms) — matches motion-tokens.md semantic naming
    const val DurationInstant = 0
    const val DurationFast    = 100
    const val DurationNormal  = 200
    const val DurationSlow    = 300
    const val DurationSlower  = 400
    const val DurationSlowest = 500

    // Easing
    val EaseLinear   = LinearEasing
    val EaseIn       = CubicBezierEasing(0.4f, 0f, 1f, 1f)
    val EaseOut      = CubicBezierEasing(0f, 0f, 0.2f, 1f)
    val EaseInOut    = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
    val EaseEmphasis = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}
```

---

## Opacity — `{Prefix}PrimitiveOpacity`

Alpha `Float` (0f–1f). Use with `Modifier.alpha()` or `Color.copy(alpha = ...)`.

```kotlin
object {Prefix}PrimitiveOpacity {
    const val Invisible  = 0f     // hidden, occupies space
    const val Hint       = 0.08f  // hover/pressed overlays
    const val Disabled   = 0.38f  // M3 disabled content
    const val Scrim      = 0.32f  // light modal scrim
    const val Medium     = 0.5f   // secondary overlays
    const val ScrimHeavy = 0.6f   // full-screen scrim
    const val High       = 0.74f  // subdued but visible
    const val Opaque     = 1f     // fully visible
}
```

## Notes

- Line heights: `TextUnit` only — `.em` or `.sp`. Raw `Float` will not compile in `TextStyle.lineHeight`.
- Durations: `const val Int` — compatible with `tween(durationMillis = ...)`.
- Easing: M3-aligned. `EaseEmphasis` = M3 "emphasized" curve for large spatial transitions.
