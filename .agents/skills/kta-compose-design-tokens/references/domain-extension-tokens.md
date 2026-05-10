# Domain Extension Tokens

Core skill tokens are domain-free. Add project-specific semantic tokens as separate
extensions alongside the core theme — never modify the core skill files.

## Pattern

```kotlin
// 1. Define domain-specific data class
@Immutable
data class {Prefix}GameColorTokens(
    val correct: Color,
    val onCorrect: Color,
    val incorrect: Color,
    val onIncorrect: Color,
    val milestone: Color,       // safe-haven level highlight
    val onMilestone: Color,
    val safeHaven: Color,       // guaranteed prize floor
    val onSafeHaven: Color,
)

// 2. Declare its CompositionLocal
val Local{Prefix}GameColors = staticCompositionLocalOf {
    {Prefix}GameColorTokens(
        correct      = Color(0xFF10B981),
        onCorrect    = Color(0xFFFFFFFF),
        incorrect    = Color(0xFFEF4444),
        onIncorrect  = Color(0xFFFFFFFF),
        milestone    = Color(0xFFFFD700),
        onMilestone  = Color(0xFF1F2937),
        safeHaven    = Color(0xFF3B82F6),
        onSafeHaven  = Color(0xFFFFFFFF),
    )
}
```

## Game Example (full)

```kotlin
// GameColorTokens.kt — owns game-specific tokens only
@Immutable
data class {Prefix}GameColorTokens(
    val correct: Color,
    val onCorrect: Color,
    val incorrect: Color,
    val onIncorrect: Color,
    val milestone: Color,
    val onMilestone: Color,
    val safeHaven: Color,
    val onSafeHaven: Color,
)

val darkGame{Prefix}Colors = {Prefix}GameColorTokens(
    correct     = Color(0xFF34D399),
    onCorrect   = Color(0xFF064E3B),
    incorrect   = Color(0xFFF87171),
    onIncorrect = Color(0xFF7F1D1D),
    milestone   = Color(0xFFFFD700),
    onMilestone = Color(0xFF1F2937),
    safeHaven   = Color(0xFF60A5FA),
    onSafeHaven = Color(0xFF1E3A5F),
)

val lightGame{Prefix}Colors = {Prefix}GameColorTokens(
    correct     = Color(0xFF059669),
    onCorrect   = Color(0xFFFFFFFF),
    incorrect   = Color(0xFFDC2626),
    onIncorrect = Color(0xFFFFFFFF),
    milestone   = Color(0xFFB45309),
    onMilestone = Color(0xFFFFFFFF),
    safeHaven   = Color(0xFF2563EB),
    onSafeHaven = Color(0xFFFFFFFF),
)
```

## E-Commerce Example

```kotlin
@Immutable
data class {Prefix}ShopColorTokens(
    val promotion: Color,
    val onPromotion: Color,
    val discount: Color,
    val onDiscount: Color,
    val cartHighlight: Color,
    val onCartHighlight: Color,
)

val Local{Prefix}ShopColors = staticCompositionLocalOf {
    {Prefix}ShopColorTokens(
        promotion      = Color(0xFFEC4899),
        onPromotion    = Color(0xFFFFFFFF),
        discount       = Color(0xFFEF4444),
        onDiscount     = Color(0xFFFFFFFF),
        cartHighlight  = Color(0xFF10B981),
        onCartHighlight = Color(0xFFFFFFFF),
    )
}
```

## Integration — provide alongside core tokens

```kotlin
@Composable
fun {Prefix}Theme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val gameColors = if (darkTheme) darkGame{Prefix}Colors else lightGame{Prefix}Colors

    CompositionLocalProvider(
        Local{Prefix}Colors        provides if (darkTheme) dark{Prefix}Colors else light{Prefix}Colors,
        Local{Prefix}GameColors    provides gameColors,   // domain extension
        // other core locals...
    ) {
        MaterialTheme(
            colorScheme = Local{Prefix}Colors.current.toMaterial3ColorScheme(darkTheme),
            content = content,
        )
    }
}

// Access in composables
val gameColors = Local{Prefix}GameColors.current
Box(modifier = Modifier.background(gameColors.correct))
```

> RULE: Core skill files (`semantic-colors.md`, templates) MUST stay domain-free.
> Extensions are always project-specific and live outside the skill directory.
