# Primitive Spacing, Shape, Elevation, Icon Size & Border Reference

Raw structural tokens — no semantic meaning, no theme awareness.

---

## Spacing — `{Prefix}PrimitiveSpacing`

4dp base grid. All values in `dp`.

| Token      | Value  |
|------------|--------|
| `Xxxs`     | 1.dp   |
| `Xxs`      | 2.dp   |
| `Xs`       | 4.dp   |
| `Sm`       | 8.dp   |
| `SmPlus`   | 10.dp  |
| `Md`       | 16.dp  |
| `Lg`       | 24.dp  |
| `Xl`       | 32.dp  |
| `Xxl`      | 40.dp  |
| `Xxxl`     | 48.dp  |
| `Huge`     | 64.dp  |
| `Giant`    | 80.dp  |
| `Enormous` | 100.dp |
| `Massive`  | 120.dp |

```kotlin
object {Prefix}PrimitiveSpacing {
    val Xxxs    = 1.dp
    val Xxs     = 2.dp
    val Xs      = 4.dp
    val Sm      = 8.dp
    val SmPlus  = 10.dp
    val Md      = 16.dp
    val Lg      = 24.dp
    val Xl      = 32.dp
    val Xxl     = 40.dp
    val Xxxl    = 48.dp
    val Huge    = 64.dp
    val Giant   = 80.dp
    val Enormous = 100.dp
    val Massive = 120.dp
}
```

---

## Shape — `{Prefix}PrimitiveShape`

`RoundedCornerShape` scale. Import `androidx.compose.foundation.shape.RoundedCornerShape`.

| Token  | Value                       |
|--------|-----------------------------|
| `None` | `RoundedCornerShape(0.dp)`  |
| `Xs`   | `RoundedCornerShape(2.dp)`  |
| `Sm`   | `RoundedCornerShape(4.dp)`  |
| `Md`   | `RoundedCornerShape(8.dp)`  |
| `Lg`   | `RoundedCornerShape(16.dp)` |
| `Xl`   | `RoundedCornerShape(24.dp)` |
| `Xxl`  | `RoundedCornerShape(32.dp)` |
| `Full` | `RoundedCornerShape(50)`    |

```kotlin
object {Prefix}PrimitiveShape {
    val None = RoundedCornerShape(0.dp)
    val Xs   = RoundedCornerShape(2.dp)
    val Sm   = RoundedCornerShape(4.dp)
    val Md   = RoundedCornerShape(8.dp)
    val Lg   = RoundedCornerShape(16.dp)
    val Xl   = RoundedCornerShape(24.dp)
    val Xxl  = RoundedCornerShape(32.dp)
    val Full = RoundedCornerShape(50)
}
```

Note: `RoundedCornerShape(50)` uses percent — produces a pill/circle shape.

---

## Elevation — `{Prefix}PrimitiveElevation`

Shadow/tonal elevation in `dp`. Used with `Modifier.shadow()` or `Card(elevation = ...)`.

| Token  | Value | Token  | Value  |
|--------|-------|--------|--------|
| `None` | 0.dp  | `Lg`   | 8.dp   |
| `Xs`   | 1.dp  | `Xl`   | 12.dp  |
| `Sm`   | 2.dp  | `Xxl`  | 16.dp  |
| `Md`   | 4.dp  | `Xxxl` | 24.dp  |

---

## Icon Size — `{Prefix}PrimitiveIconSize`

Square icon dimensions. Pass to `Modifier.size()`.

| Token      | Value  | Token      | Value  |
|------------|--------|------------|--------|
| `Xxs`      | 8.dp   | `Xxl`      | 48.dp  |
| `Xs`       | 12.dp  | `Xxxl`     | 64.dp  |
| `Sm`       | 16.dp  | `Huge`     | 80.dp  |
| `Md`       | 24.dp  | `Enormous` | 120.dp |
| `Lg`       | 32.dp  | `Xl`       | 40.dp  |

---

## Border — `{Prefix}PrimitiveBorder`

Stroke/border width in `dp`. Use with `Modifier.border()` or `BorderStroke`.

| Token    | Value |
|----------|-------|
| `None`   | 0.dp  |
| `Hairline` | 0.5.dp |
| `Thin`   | 1.dp  |
| `Medium` | 2.dp  |
| `Thick`  | 4.dp  |

```kotlin
object {Prefix}PrimitiveBorder {
    val None     = 0.dp
    val Hairline = 0.5.dp
    val Thin     = 1.dp
    val Medium   = 2.dp
    val Thick    = 4.dp
}
```

---

## Notes

- All tokens are raw values — map to semantic roles in a separate alias layer
- Template files: `~/.claude/skills/compose-design-tokens/templates/spacing-template.kt`, `shape-template.kt`
