# Text Field Tokens

Token class for text field component styling. Covers filled and outlined variants with all input states.

```kotlin
@Immutable
data class {Prefix}TextFieldTokens(
    val containerShape: Shape,             // small top corners / 4dp, flat bottom
    val containerColor: Color,             // surfaceContainerHighest
    val inputStyle: TextStyle,             // bodyLarge
    val inputColor: Color,                 // onSurface
    val labelStyle: TextStyle,             // bodySmall (when floating)
    val labelColor: Color,                 // onSurfaceVariant
    val labelFocusedColor: Color,          // primary
    val labelErrorColor: Color,            // error
    val placeholderStyle: TextStyle,       // bodyLarge
    val placeholderColor: Color,           // onSurfaceVariant
    val cursorColor: Color,                // primary
    val cursorErrorColor: Color,           // error
    val indicatorColor: Color,             // onSurfaceVariant
    val indicatorFocusedColor: Color,      // primary
    val indicatorErrorColor: Color,        // error
    val indicatorWidth: Dp,                // 1dp
    val indicatorFocusedWidth: Dp,         // 2dp
    val errorColor: Color,                 // error
    val supportingStyle: TextStyle,        // bodySmall
    val supportingColor: Color,            // onSurfaceVariant
    val supportingErrorColor: Color,       // error
    val contentPadding: PaddingValues,     // 16dp horizontal, 8dp vertical
    val minHeight: Dp,                     // 56dp
    val iconSize: Dp,                      // 24dp
    val iconColor: Color,                  // onSurfaceVariant
    val disabledOpacity: Float,            // 0.38f
    val disabledContainerColor: Color,     // onSurface 0.04f
)
```

## Default Values

Default values are resolved at runtime from `{Prefix}ColorTokens` and `{Prefix}TypographyTokens` —
not hardcoded to avoid `@Composable` context requirements.

```kotlin
fun default{Prefix}TextFieldTokens(
    colors: {Prefix}ColorTokens,
    typography: {Prefix}TypographyTokens,
): {Prefix}TextFieldTokens = {Prefix}TextFieldTokens(
    containerShape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
    containerColor = colors.surfaceContainerHighest,          // semantic: surfaceContainerHighest
    inputStyle = typography.bodyLarge,
    inputColor = colors.onSurface,                            // semantic: onSurface
    labelStyle = typography.bodySmall,
    labelColor = colors.onSurfaceVariant,                     // semantic: onSurfaceVariant
    labelFocusedColor = colors.primary,                       // semantic: primary
    labelErrorColor = colors.error,                           // semantic: error
    placeholderStyle = typography.bodyLarge,
    placeholderColor = colors.onSurfaceVariant,               // semantic: onSurfaceVariant
    cursorColor = colors.primary,                             // semantic: primary
    cursorErrorColor = colors.error,                          // semantic: error
    indicatorColor = colors.onSurfaceVariant,                 // semantic: onSurfaceVariant
    indicatorFocusedColor = colors.primary,                   // semantic: primary
    indicatorErrorColor = colors.error,                       // semantic: error
    indicatorWidth = 1.dp,
    indicatorFocusedWidth = 2.dp,
    errorColor = colors.error,                                // semantic: error
    supportingStyle = typography.bodySmall,
    supportingColor = colors.onSurfaceVariant,                // semantic: onSurfaceVariant
    supportingErrorColor = colors.error,                      // semantic: error
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    minHeight = 56.dp,
    iconSize = 24.dp,
    iconColor = colors.onSurfaceVariant,                      // semantic: onSurfaceVariant
    disabledOpacity = 0.38f,
    disabledContainerColor = colors.onSurface.copy(alpha = 0.04f), // semantic: onSurface
)
```

## State Matrix

| State    | indicatorColor    | indicatorWidth | labelColor        | containerColor              |
|----------|-------------------|----------------|-------------------|-----------------------------|
| default  | onSurfaceVariant  | 1dp            | onSurfaceVariant  | surfaceContainerHighest     |
| focused  | primary           | 2dp            | primary           | surfaceContainerHighest     |
| error    | error             | 2dp            | error             | surfaceContainerHighest     |
| disabled | onSurface 0.38f   | 1dp            | onSurface 0.38f   | onSurface 0.04f             |

## Usage

```kotlin
{Prefix}TextField(
    value = email,
    onValueChange = { email = it },
    label = "Email",
    tokens = {Prefix}TextFieldDefaults,
    isError = !isEmailValid,
    supportingText = if (!isEmailValid) "Invalid email" else null,
)
```
