# Component Implementation Requirements

After generating token files, MUST create composable components that consume those tokens. This is NOT optional.

## Token-to-Component Mapping

Each token type maps to required component(s):

| Token Generated | Required Components | Priority |
|----------------|-------------------|----------|
| ColorTokens | `{Prefix}Surface`, `{Prefix}Background` | P0 |
| ColorTokens + TypographyTokens | `{Prefix}Text`, `{Prefix}Label` | P0 |
| ButtonTokens | `{Prefix}FilledButton`, `{Prefix}OutlinedButton`, `{Prefix}TextButton` | P0 |
| CardTokens | `{Prefix}Card`, `{Prefix}OutlinedCard` | P1 |
| TextFieldTokens | `{Prefix}TextField`, `{Prefix}OutlinedTextField` | P1 |
| DialogTokens | `{Prefix}Dialog` | P1 |
| BottomSheetTokens | `{Prefix}BottomSheet` | P2 |
| SnackbarTokens | `{Prefix}Snackbar` | P2 |
| TopBarTokens | `{Prefix}TopBar` | P2 |
| BadgeChipTokens | `{Prefix}Badge`, `{Prefix}Chip` | P2 |
| DividerTokens | `{Prefix}Divider` | P2 |
| SkeletonTokens | `{Prefix}SkeletonRow` | P2 |

**Rule**: P0 = always created. P1 = created if tokens exist. P2 = created if comprehensive scope.

## Component Structure Pattern

Every token-backed component follows this pattern:

```kotlin
@Composable
fun {Prefix}FilledButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable RowScope.() -> Unit,
) {
    val tokens = {Prefix}Theme.button  // reads from CompositionLocal
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = tokens.shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = tokens.filledContainerColor,
            contentColor = tokens.filledContentColor,
            disabledContainerColor = tokens.filledDisabledContainerColor,
            disabledContentColor = tokens.filledDisabledContentColor,
        ),
        contentPadding = tokens.contentPadding,
    ) {
        label()
    }
}
```

## P0 Component Templates

### {Prefix}Surface
```kotlin
@Composable
fun {Prefix}Surface(
    modifier: Modifier = Modifier,
    shape: Shape = {Prefix}Theme.shapes.medium,
    color: Color = {Prefix}Theme.colors.surface,
    contentColor: Color = {Prefix}Theme.colors.onSurface,
    tonalElevation: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        content = content,
    )
}
```

### {Prefix}Text
```kotlin
@Composable
fun {Prefix}Text(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = {Prefix}Theme.typography.bodyMedium,
    color: Color = {Prefix}Theme.colors.onSurface,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
    )
}
```

### {Prefix}Background
```kotlin
@Composable
fun {Prefix}Background(
    modifier: Modifier = Modifier,
    color: Color = {Prefix}Theme.colors.background,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color),
        content = content,
    )
}
```

## File Placement

Components go in the UI module (discovered via convention protocol), NOT the theme module:
- Theme module → token data classes + defaults + CompositionLocals
- UI module → composable components that consume tokens

```
ui-components/src/main/kotlin/.../component/
├── button/     → {Prefix}FilledButton.kt, {Prefix}OutlinedButton.kt, {Prefix}TextButton.kt
├── text/       → {Prefix}Text.kt, {Prefix}Label.kt
├── surface/    → {Prefix}Surface.kt, {Prefix}Background.kt
├── card/       → {Prefix}Card.kt
├── dialog/     → {Prefix}Dialog.kt
├── field/      → {Prefix}TextField.kt
└── showcase/   → {Prefix}TokenShowcaseScreen.kt
```

## Checklist

1. [ ] Every generated token class has at least one consuming composable
2. [ ] Components use `{Prefix}Theme.*` accessors, zero hardcoded values
3. [ ] Each component has `modifier: Modifier = Modifier` param
4. [ ] Each component file has `@Preview` function
5. [ ] Components compile: `./gradlew :{ui-module}:compileDebugKotlin`
