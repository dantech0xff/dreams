# Semantic Layout & Typography Tokens

## Spacing Tokens

```kotlin
@Immutable
data class {Prefix}SpacingTokens(
    val componentPadding: Dp,   // 12.dp — internal padding inside a component
    val componentGap: Dp,       // 8.dp  — gap between items within a component
    val sectionPadding: Dp,     // 16.dp — padding inside a section/card
    val sectionGap: Dp,         // 12.dp — gap between adjacent sections
    val screenPadding: Dp,      // 16.dp — screen-edge horizontal/vertical padding
    val inlineGap: Dp,          // 4.dp  — gap between inline elements (icon + label)
    val stackGap: Dp,           // 8.dp  — gap between vertically stacked items
    val touchTargetMin: Dp,     // 48.dp — minimum touch target (WCAG AA)
)

val default{Prefix}Spacing = {Prefix}SpacingTokens(
    componentPadding = 12.dp,
    componentGap     = 8.dp,
    sectionPadding   = 16.dp,
    sectionGap       = 12.dp,
    screenPadding    = 16.dp,
    inlineGap        = 4.dp,
    stackGap         = 8.dp,
    touchTargetMin   = 48.dp,
)

val Local{Prefix}Spacing = staticCompositionLocalOf { default{Prefix}Spacing }
```

## Shape Tokens

```kotlin
@Immutable
data class {Prefix}ShapeTokens(
    val small: Shape,       // RoundedCornerShape(4.dp)
    val medium: Shape,      // RoundedCornerShape(8.dp)
    val large: Shape,       // RoundedCornerShape(16.dp)
    val extraLarge: Shape,  // RoundedCornerShape(28.dp)
    val full: Shape,        // CircleShape
)

val default{Prefix}Shapes = {Prefix}ShapeTokens(
    small      = RoundedCornerShape(4.dp),
    medium     = RoundedCornerShape(8.dp),
    large      = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
    full       = CircleShape,
)

val Local{Prefix}Shapes = staticCompositionLocalOf { default{Prefix}Shapes }
```

## Elevation Tokens

```kotlin
@Immutable
data class {Prefix}ElevationTokens(
    val none: Dp,     // 0.dp
    val low: Dp,      // 2.dp
    val medium: Dp,   // 4.dp
    val high: Dp,     // 8.dp
    val highest: Dp,  // 16.dp
)

val default{Prefix}Elevation = {Prefix}ElevationTokens(
    none    = 0.dp,
    low     = 2.dp,
    medium  = 4.dp,
    high    = 8.dp,
    highest = 16.dp,
)

val Local{Prefix}Elevation = staticCompositionLocalOf { default{Prefix}Elevation }
```

## Typography Tokens

```kotlin
@Immutable
data class {Prefix}TypographyTokens(
    val displayLarge: TextStyle,    // 57.sp / Regular
    val displayMedium: TextStyle,   // 45.sp / Regular
    val displaySmall: TextStyle,    // 36.sp / Regular
    val headlineLarge: TextStyle,   // 32.sp / Bold
    val headlineMedium: TextStyle,  // 28.sp / Bold
    val titleLarge: TextStyle,      // 22.sp / Medium
    val titleMedium: TextStyle,     // 16.sp / Medium
    val bodyLarge: TextStyle,       // 16.sp / Regular
    val bodyMedium: TextStyle,      // 14.sp / Regular
    val labelLarge: TextStyle,      // 14.sp / Medium
    val labelMedium: TextStyle,     // 12.sp / Medium
    val labelSmall: TextStyle,      // 11.sp / Medium
    val captionSmall: TextStyle,    // 10.sp / Regular — hint/meta text
)

val default{Prefix}Typography = {Prefix}TypographyTokens(
    displayLarge   = TextStyle(fontSize = 57.sp, fontWeight = FontWeight.Normal, lineHeight = 64.sp),
    displayMedium  = TextStyle(fontSize = 45.sp, fontWeight = FontWeight.Normal, lineHeight = 52.sp),
    displaySmall   = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Normal, lineHeight = 44.sp),
    headlineLarge  = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold,   lineHeight = 40.sp),
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold,   lineHeight = 36.sp),
    titleLarge     = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium, lineHeight = 28.sp),
    titleMedium    = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 24.sp),
    bodyLarge      = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp),
    bodyMedium     = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
    labelLarge     = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp),
    labelMedium    = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp),
    labelSmall     = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp),
    captionSmall   = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Normal, lineHeight = 14.sp),
)

val Local{Prefix}Typography = staticCompositionLocalOf { default{Prefix}Typography }
```
