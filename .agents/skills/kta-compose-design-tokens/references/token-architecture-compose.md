# Token Architecture — Kotlin Compose

3-layer system mapping design decisions to Compose code patterns.

## Layer 1: Primitive Tokens

Raw values without semantic meaning. Kotlin `object` with `val` properties.

```kotlin
object QzdsPrimitiveColors {
    val Blue500 = Color(0xFF3B82F6)
    val Blue600 = Color(0xFF2563EB)
    // Full scale: 50, 100, 200, ..., 900
}

object QzdsPrimitiveSpacing {
    val Xs = 4.dp
    val Sm = 10.dp
    val Md = 16.dp
}
```

**Rules:**
- `object` (singleton, no state)
- PascalCase properties: `{ColorName}{Scale}` (e.g., `Blue500`)
- No `@Immutable` needed (object with val = inherently stable)
- One object per category: Colors, Spacing, Shape, Motion, Opacity, Elevation, Border, IconSize

## Layer 2: Semantic Tokens

Purpose-based aliases referencing primitives. `@Immutable data class` + dark/light instances.

```kotlin
@Immutable
data class QzdsColorTokens(
    val primary: Color,
    val onPrimary: Color,
    val background: Color,
    // ...
)

val darkQzdsColors = QzdsColorTokens(
    primary = QzdsPrimitiveColors.Purple500,
    onPrimary = QzdsPrimitiveColors.White,
    // ...
)
val lightQzdsColors = QzdsColorTokens(
    primary = QzdsPrimitiveColors.Purple600,
    // ...
)

val LocalQzdsColors = staticCompositionLocalOf { darkQzdsColors }
```

**Rules:**
- `@Immutable data class` (Compose stability)
- camelCase properties: `primary`, `onPrimary`, `surfaceVariant`
- Dark + Light instances for color tokens
- Single default instance for non-color tokens (spacing, shape, motion)
- `staticCompositionLocalOf` (not `compositionLocalOf` — tokens rarely change)

## Layer 3: Component Tokens

Component-specific overrides referencing semantic + primitive layers.

```kotlin
@Immutable
data class QzdsDialogTokens(
    val containerShape: Shape,
    val containerColor: Color,
    val contentPadding: Dp,
    // ...
)

val defaultQzdsDialog = QzdsDialogTokens(
    containerShape = QzdsPrimitiveShape.Lg,
    containerColor = QzdsPrimitiveColors.White,
    contentPadding = QzdsPrimitiveSpacing.Md,
)

val LocalQzdsDialog = staticCompositionLocalOf { defaultQzdsDialog }
```

**Rules:**
- Same pattern as semantic, but scoped to one component
- Properties named `{aspect}`: `containerShape`, `contentPadding`, `borderColor`
- Reference primitives directly (not semantic) for component-level control

## QzdsTheme Registration

Every token must register in two places:

### 1. CompositionLocalProvider (QzdsTheme composable)
```kotlin
CompositionLocalProvider(
    LocalQzdsNewToken provides defaultQzdsNewToken,
) { ... }
```

### 2. QzdsTheme accessor object
```kotlin
object QzdsTheme {
    val newToken: QzdsNewTokens
        @Composable get() = LocalQzdsNewToken.current
}
```

## Usage in Composables

```kotlin
// Access via QzdsTheme object
val color = QzdsTheme.colors.primary
val padding = QzdsTheme.spacing.screenPadding
val shape = QzdsTheme.dialog.containerShape
```

## Existing Token Inventory

| Layer | Class | Properties |
|-------|-------|------------|
| Primitive | QzdsPrimitiveColors | ~80 colors (10 scales) |
| Primitive | QzdsPrimitiveSpacing | 18 sizes (1-140dp) |
| Primitive | QzdsPrimitiveShape | 9 shapes (None-Full) |
| Primitive | QzdsPrimitiveMotion | 4 durations + 3 easings |
| Primitive | QzdsPrimitiveElevation | elevation values |
| Primitive | QzdsPrimitiveOpacity | opacity values |
| Primitive | QzdsPrimitiveBorder | border values |
| Primitive | QzdsPrimitiveIconSize | icon sizes |
| Semantic | QzdsColorTokens | 28 colors (dark+light) |
| Semantic | QzdsSpacingTokens | 8 spacings |
| Semantic | QzdsShapeTokens | shape aliases |
| Semantic | QzdsElevationTokens | elevation aliases |
| Semantic | QzdsTypographyTokens | 21 text styles |
| Semantic | QzdsMotionTokens | 4 durations + 3 easings |
| Semantic | QzdsOpacityTokens | opacity aliases |
| Semantic | QzdsGameColorTokens | 17 game colors (dark+light) |
| Component | QzdsDialogTokens | 8 properties |
| Component | QzdsBottomSheetTokens | 7 properties |
| Component | QzdsCardTokens | 9 properties |
