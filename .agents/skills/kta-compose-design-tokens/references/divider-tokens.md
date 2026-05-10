# Divider Tokens

Token class for horizontal and vertical divider styling. Simple component — minimal tokens needed.

```kotlin
@Immutable
data class {Prefix}DividerTokens(
    val color: Color,          // outlineVariant
    val thickness: Dp,         // 1dp
    val startIndent: Dp,       // 0dp full-bleed / 16dp inset
    val endIndent: Dp,         // 0dp full-bleed / 16dp middleInset
)
```

## Default Values

Default values are resolved at runtime from `{Prefix}ColorTokens` —
not hardcoded to avoid `@Composable` context requirements.

```kotlin
fun default{Prefix}DividerTokens(
    colors: {Prefix}ColorTokens,
): {Prefix}DividerTokens = {Prefix}DividerTokens(
    color = colors.outlineVariant,  // semantic: outlineVariant
    thickness = 1.dp,
    startIndent = 0.dp,
    endIndent = 0.dp,
)
```

## Variants

| Variant      | startIndent | endIndent | Use case                         |
|--------------|-------------|-----------|----------------------------------|
| fullBleed    | 0dp         | 0dp       | full-width section separator     |
| inset        | 16dp        | 0dp       | list items with leading icon     |
| middleInset  | 16dp        | 16dp      | centered content rows            |

```kotlin
val {Prefix}DividerInsetDefaults = {Prefix}DividerDefaults.copy(startIndent = 16.dp)
val {Prefix}DividerMiddleDefaults = {Prefix}DividerDefaults.copy(startIndent = 16.dp, endIndent = 16.dp)
```

## Vertical Divider

Uses the same color and thickness tokens — applied to a vertical element between inline items.

```kotlin
val {Prefix}VerticalDividerDefaults = {Prefix}DividerDefaults
// thickness applies as width; height is determined by the parent layout
```

## Usage

```kotlin
// Horizontal full-bleed
{Prefix}Divider(tokens = {Prefix}DividerDefaults)

// Inset (e.g. below list item with 56dp leading avatar)
{Prefix}Divider(tokens = {Prefix}DividerInsetDefaults.copy(startIndent = 56.dp))

// Vertical between two inline elements
{Prefix}VerticalDivider(modifier = Modifier.height(24.dp), tokens = {Prefix}VerticalDividerDefaults)
```
