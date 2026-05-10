# Code Templates

## Template 1: Primitive Color Object

```kotlin
package {PACKAGE}

import androidx.compose.ui.graphics.Color

object {PREFIX}Primitive{NAME} {
    // {SCALE_NAME} scale
    val {NAME}900 = Color(0xFF{HEX_900})
    val {NAME}800 = Color(0xFF{HEX_800})
    val {NAME}700 = Color(0xFF{HEX_700})
    val {NAME}600 = Color(0xFF{HEX_600})
    val {NAME}500 = Color(0xFF{HEX_500})
    val {NAME}400 = Color(0xFF{HEX_400})
    val {NAME}300 = Color(0xFF{HEX_300})
    val {NAME}200 = Color(0xFF{HEX_200})
    val {NAME}100 = Color(0xFF{HEX_100})
    val {NAME}50 = Color(0xFF{HEX_50})
}
```

## Template 2: Primitive Spacing/Size Object

```kotlin
package {PACKAGE}

import androidx.compose.ui.unit.dp

object {PREFIX}Primitive{CATEGORY} {
    val Xxxs = {V1}.dp
    val Xxs = {V2}.dp
    val Xs = {V3}.dp
    val Sm = {V4}.dp
    val Md = {V5}.dp
    val Lg = {V6}.dp
    val Xl = {V7}.dp
    val Xxl = {V8}.dp
    val Xxxl = {V9}.dp
}
```

## Template 3: Semantic Token (with dark/light)

```kotlin
package {PACKAGE}

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class {PREFIX}{NAME}Tokens(
    val {PROP1}: Color,
    val {PROP2}: Color,
    // ...
)

val dark{PREFIX}{NAME} = {PREFIX}{NAME}Tokens(
    {PROP1} = {PREFIX}PrimitiveColors.{DARK_VAL1},
    {PROP2} = {PREFIX}PrimitiveColors.{DARK_VAL2},
)

val light{PREFIX}{NAME} = {PREFIX}{NAME}Tokens(
    {PROP1} = {PREFIX}PrimitiveColors.{LIGHT_VAL1},
    {PROP2} = {PREFIX}PrimitiveColors.{LIGHT_VAL2},
)

val Local{PREFIX}{NAME} = staticCompositionLocalOf { dark{PREFIX}{NAME} }
```

## Template 4: Semantic Token (single default)

```kotlin
package {PACKAGE}

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp

@Immutable
data class {PREFIX}{NAME}Tokens(
    val {PROP1}: Dp,
    val {PROP2}: Dp,
)

val default{PREFIX}{NAME} = {PREFIX}{NAME}Tokens(
    {PROP1} = {PREFIX}Primitive{CATEGORY}.{VAL1},
    {PROP2} = {PREFIX}Primitive{CATEGORY}.{VAL2},
)

val Local{PREFIX}{NAME} = staticCompositionLocalOf { default{PREFIX}{NAME} }
```

## Template 5: Component Token

```kotlin
package {PACKAGE}

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

@Immutable
data class {PREFIX}{COMPONENT}Tokens(
    val containerShape: Shape,
    val containerColor: Color,
    val contentPadding: Dp,
    val elevation: Dp,
    val borderWidth: Dp,
    val borderColor: Color,
)

val default{PREFIX}{COMPONENT} = {PREFIX}{COMPONENT}Tokens(
    containerShape = {PREFIX}PrimitiveShape.{SHAPE},
    containerColor = {PREFIX}PrimitiveColors.{COLOR},
    contentPadding = {PREFIX}PrimitiveSpacing.{SPACING},
    elevation = {PREFIX}PrimitiveElevation.{ELEVATION},
    borderWidth = {PREFIX}PrimitiveSpacing.{BORDER},
    borderColor = {PREFIX}PrimitiveColors.{BORDER_COLOR},
)

val Local{PREFIX}{COMPONENT} = staticCompositionLocalOf { default{PREFIX}{COMPONENT} }
```

## Template 6: QzdsTheme Registration

### Add to CompositionLocalProvider
```kotlin
// In QzdsTheme composable function:
Local{PREFIX}{NAME} provides default{PREFIX}{NAME},
```

### Add to QzdsTheme object
```kotlin
// In QzdsTheme object:
val {accessorName}: {PREFIX}{NAME}Tokens
    @Composable get() = Local{PREFIX}{NAME}.current
```

## Checklist After Generation
1. [ ] File placed in `theme/src/main/kotlin/.../theme/`
2. [ ] Package declaration matches directory
3. [ ] CompositionLocal registered in QzdsTheme composable
4. [ ] Accessor added to QzdsTheme object
5. [ ] `./gradlew :theme:compileDebugKotlin` passes
