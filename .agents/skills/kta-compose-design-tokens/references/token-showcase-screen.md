# Token Showcase Screen

MANDATORY: After creating token-backed components, generate a showcase screen that displays all components for visual preview.

## Purpose

The showcase screen lets the user:
1. See all generated components rendered with their tokens
2. Verify light/dark theme switching
3. Preview component variants (filled, outlined, text buttons, etc.)
4. Validate visual consistency before shipping

## Screen Structure

```kotlin
@Composable
fun {Prefix}TokenShowcaseScreen(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
) {
    {Prefix}Theme(darkTheme = darkTheme) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background({Prefix}Theme.colors.background),
            contentPadding = PaddingValues({Prefix}Theme.spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy({Prefix}Theme.spacing.contentGap),
        ) {
            // Section: Color Palette
            item { ShowcaseSectionHeader("Color Palette") }
            item { ColorPaletteShowcase() }

            // Section: Typography Scale
            item { ShowcaseSectionHeader("Typography") }
            item { TypographyShowcase() }

            // Section: Spacing & Shape
            item { ShowcaseSectionHeader("Spacing & Shapes") }
            item { SpacingShapeShowcase() }

            // Section: Buttons
            item { ShowcaseSectionHeader("Buttons") }
            item { ButtonShowcase() }

            // Section: Cards
            item { ShowcaseSectionHeader("Cards") }
            item { CardShowcase() }

            // Section: Text Fields
            item { ShowcaseSectionHeader("Text Fields") }
            item { TextFieldShowcase() }

            // Section: Dialogs (preview inline)
            item { ShowcaseSectionHeader("Dialogs") }
            item { DialogShowcase() }

            // ... additional sections per generated components
        }
    }
}
```

## Section Templates

### Color Palette
```kotlin
@Composable
private fun ColorPaletteShowcase() {
    val colors = {Prefix}Theme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ColorSwatchRow("Primary", colors.primary, colors.onPrimary)
        ColorSwatchRow("Secondary", colors.secondary, colors.onSecondary)
        ColorSwatchRow("Tertiary", colors.tertiary, colors.onTertiary)
        ColorSwatchRow("Surface", colors.surface, colors.onSurface)
        ColorSwatchRow("Background", colors.background, colors.onBackground)
        ColorSwatchRow("Error", colors.error, colors.onError)
    }
}

@Composable
private fun ColorSwatchRow(label: String, bg: Color, fg: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(bg, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("Aa", color = fg, style = MaterialTheme.typography.labelSmall)
        }
        Text(label, style = {Prefix}Theme.typography.bodyMedium)
    }
}
```

### Typography Scale
```kotlin
@Composable
private fun TypographyShowcase() {
    val typography = {Prefix}Theme.typography
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Display Large", style = typography.displayLarge)
        Text("Headline Medium", style = typography.headlineMedium)
        Text("Title Large", style = typography.titleLarge)
        Text("Body Medium — The quick brown fox", style = typography.bodyMedium)
        Text("Label Small", style = typography.labelSmall)
    }
}
```

### Button Variants
```kotlin
@Composable
private fun ButtonShowcase() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            {Prefix}FilledButton(onClick = {}) { Text("Filled") }
            {Prefix}OutlinedButton(onClick = {}) { Text("Outlined") }
            {Prefix}TextButton(onClick = {}) { Text("Text") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            {Prefix}FilledButton(onClick = {}, enabled = false) { Text("Disabled") }
            {Prefix}OutlinedButton(onClick = {}, enabled = false) { Text("Disabled") }
        }
    }
}
```

### Cards
```kotlin
@Composable
private fun CardShowcase() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        {Prefix}Card {
            Text("Filled Card", modifier = Modifier.padding(16.dp))
        }
        {Prefix}OutlinedCard {
            Text("Outlined Card", modifier = Modifier.padding(16.dp))
        }
    }
}
```

## Input-Source Specific Showcase

### From Screenshot/PNG
When tokens derived from a screenshot, the showcase screen MUST:
1. Display the source image at the top for side-by-side comparison
2. Recreate key UI elements visible in the screenshot using generated tokens
3. Add "Source vs Recreated" labels

### From Figma
When tokens derived from Figma, the showcase screen MUST:
1. Mirror Figma frame layout where possible
2. Match component naming from Figma component library
3. Include a "Figma Mapping" section showing token ↔ Figma variable names

### From Stitch (Google)
When tokens derived from Stitch, the showcase screen MUST:
1. Use `generate_screen_from_text` to create the Stitch preview first
2. Build the Compose showcase to match the Stitch-generated screen
3. Include side-by-side comparison section

## Preview Functions

MUST include multiple `@Preview` functions:

```kotlin
@Preview(name = "Light Theme", showBackground = true)
@Composable
private fun TokenShowcaseLight() {
    {Prefix}TokenShowcaseScreen(darkTheme = false)
}

@Preview(name = "Dark Theme", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TokenShowcaseDark() {
    {Prefix}TokenShowcaseScreen(darkTheme = true)
}
```

## File Location

Place in the UI module's showcase package:
```
{ui-module}/src/main/kotlin/.../component/showcase/{Prefix}TokenShowcaseScreen.kt
```

## Delegation to kta-compose-developer

When delegating to `kta-compose-developer` agent, include in prompt:
```
Task: Create TokenShowcaseScreen aggregating all built components + recreated screens.
Components to showcase: {list of created component names from Phase 1}
Recreated screens: {list of `done` rows from plans/{date-slug}/screens-todo.md}
Token classes: {list of token class names}
Source type: {figma | stitch | claude-design}
Source reference: {file key | project ID | spec dir path}
Theme path: {project_root}/{ui-module}/.../theme/
Prefix: {Prefix}
Module: {ui-module path}
Compile + light/dark @Preview required.
```

## Checklist

1. [ ] Showcase screen created with all generated components
2. [ ] Light and dark theme @Preview functions present
3. [ ] Color palette section shows all semantic colors
4. [ ] Typography section shows representative text styles
5. [ ] Every component variant displayed (filled, outlined, etc.)
6. [ ] Source-specific comparison section included (if applicable)
7. [ ] Compiles: `./gradlew :{ui-module}:compileDebugKotlin`
