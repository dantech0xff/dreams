# Opacity & Border Tokens

Alpha scale and border width tokens for Compose components.

## Opacity — Primitive Layer

```kotlin
object {Prefix}PrimitiveOpacity {
    val Invisible = 0f
    val Hint = 0.08f          // hover overlays
    val Disabled = 0.38f      // M3 disabled standard
    val Medium = 0.5f         // semi-transparent
    val High = 0.74f          // mostly opaque
    val Scrim = 0.32f         // backdrop overlay
    val ScrimHeavy = 0.6f     // heavy backdrop
    val Opaque = 1f           // fully visible
}
```

## Opacity — Semantic Layer

```kotlin
@Immutable
data class {Prefix}OpacityTokens(
    val disabled: Float,    // 0.38 — M3 disabled content/containers
    val scrim: Float,       // 0.32 — dialog/sheet backdrops
    val hover: Float,       // 0.08 — hover state overlay
    val pressed: Float,     // 0.12 — press state overlay
    val dragged: Float,     // 0.16 — drag state overlay
)

val default{Prefix}Opacity = {Prefix}OpacityTokens(
    disabled = {Prefix}PrimitiveOpacity.Disabled,
    scrim = {Prefix}PrimitiveOpacity.Scrim,
    hover = {Prefix}PrimitiveOpacity.Hint,
    pressed = 0.12f,
    dragged = 0.16f,
)
```

## Usage — Opacity

```kotlin
// Disabled state
Icon(
    modifier = Modifier.alpha(
        if (enabled) 1f else {Prefix}Theme.opacity.disabled
    )
)

// Scrim backdrop
Box(
    modifier = Modifier
        .fillMaxSize()
        .background({Prefix}Theme.colors.scrim.copy(alpha = {Prefix}Theme.opacity.scrim))
)

// Interactive state overlay
Box(
    modifier = Modifier
        .background(Color.White.copy(alpha = {Prefix}Theme.opacity.hover))
)
```

## Border — Primitive Layer

```kotlin
object {Prefix}PrimitiveBorder {
    val None = 0.dp
    val Hairline = 0.5.dp    // subtle divider
    val Thin = 1.dp          // standard border
    val Medium = 2.dp        // emphasis border
    val Thick = 4.dp         // strong emphasis
}
```

## Usage — Border

```kotlin
// Outlined card
Card(
    border = BorderStroke(
        width = {Prefix}PrimitiveBorder.Thin,
        color = {Prefix}Theme.colors.outline,
    )
)

// Focus ring
Box(
    modifier = Modifier.border(
        width = {Prefix}PrimitiveBorder.Medium,
        color = {Prefix}Theme.colors.primary,
        shape = {Prefix}Theme.shapes.medium,
    )
)

// Divider
HorizontalDivider(
    thickness = {Prefix}PrimitiveBorder.Hairline,
    color = {Prefix}Theme.colors.outlineVariant,
)
```

## State Overlay Matrix

| State | Overlay Alpha | Border Change |
|-------|--------------|---------------|
| Default | 0f | As designed |
| Hover | 0.08f | No change |
| Focused | 0f | +2dp focus ring |
| Pressed | 0.12f | No change |
| Dragged | 0.16f | No change |
| Disabled | Content 0.38f | 0.12f opacity border |
