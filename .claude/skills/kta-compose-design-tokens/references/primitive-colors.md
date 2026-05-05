# Primitive Colors Reference

Raw color palette values — no theme awareness, no semantic meaning.

## Object Pattern

```kotlin
object {Prefix}PrimitiveColors {
    val ColorName = Color(0xFFRRGGBB)
}
```

Usage: `{Prefix}PrimitiveColors.Blue500`

---

## Neutral Scale

| Token       | Hex         |
|-------------|-------------|
| `Black`     | `0xFF000000` |
| `White`     | `0xFFFFFFFF` |
| `Transparent` | `0x00000000` |

---

## Grey Scale (50–900)

| Token      | Hex          |
|------------|--------------|
| `Grey50`   | `0xFFFAFAFA` |
| `Grey100`  | `0xFFF5F5F5` |
| `Grey200`  | `0xFFEEEEEE` |
| `Grey300`  | `0xFFE0E0E0` |
| `Grey400`  | `0xFFBDBDBD` |
| `Grey500`  | `0xFF9E9E9E` |
| `Grey600`  | `0xFF757575` |
| `Grey700`  | `0xFF616161` |
| `Grey800`  | `0xFF424242` |
| `Grey900`  | `0xFF212121` |

---

## Brand Palette Scales (50–900)

### Blue

| Token      | Hex          |
|------------|--------------|
| `Blue50`   | `0xFFE3F2FD` |
| `Blue100`  | `0xFFBBDEFB` |
| `Blue200`  | `0xFF90CAF9` |
| `Blue300`  | `0xFF64B5F6` |
| `Blue400`  | `0xFF42A5F5` |
| `Blue500`  | `0xFF2196F3` |
| `Blue600`  | `0xFF1E88E5` |
| `Blue700`  | `0xFF1976D2` |
| `Blue800`  | `0xFF1565C0` |
| `Blue900`  | `0xFF0D47A1` |

### Purple

| Token        | Hex          |
|--------------|--------------|
| `Purple50`   | `0xFFF3E5F5` |
| `Purple100`  | `0xFFE1BEE7` |
| `Purple200`  | `0xFFCE93D8` |
| `Purple300`  | `0xFFBA68C8` |
| `Purple400`  | `0xFFAB47BC` |
| `Purple500`  | `0xFF9C27B0` |
| `Purple600`  | `0xFF8E24AA` |
| `Purple700`  | `0xFF7B1FA2` |
| `Purple800`  | `0xFF6A1B9A` |
| `Purple900`  | `0xFF4A148C` |

### Green

| Token       | Hex          |
|-------------|--------------|
| `Green50`   | `0xFFE8F5E9` |
| `Green100`  | `0xFFC8E6C9` |
| `Green200`  | `0xFFA5D6A7` |
| `Green300`  | `0xFF81C784` |
| `Green400`  | `0xFF66BB6A` |
| `Green500`  | `0xFF4CAF50` |
| `Green600`  | `0xFF43A047` |
| `Green700`  | `0xFF388E3C` |
| `Green800`  | `0xFF2E7D32` |
| `Green900`  | `0xFF1B5E20` |

### Red

| Token      | Hex          |
|------------|--------------|
| `Red50`    | `0xFFFFEBEE` |
| `Red100`   | `0xFFFFCDD2` |
| `Red200`   | `0xFFEF9A9A` |
| `Red300`   | `0xFFE57373` |
| `Red400`   | `0xFFEF5350` |
| `Red500`   | `0xFFF44336` |
| `Red600`   | `0xFFE53935` |
| `Red700`   | `0xFFD32F2F` |
| `Red800`   | `0xFFC62828` |
| `Red900`   | `0xFFB71C1C` |

### Yellow

| Token        | Hex          |
|--------------|--------------|
| `Yellow50`   | `0xFFFFFDE7` |
| `Yellow100`  | `0xFFFFF9C4` |
| `Yellow200`  | `0xFFFFF59D` |
| `Yellow300`  | `0xFFFFF176` |
| `Yellow400`  | `0xFFFFEE58` |
| `Yellow500`  | `0xFFFFEB3B` |
| `Yellow600`  | `0xFFFDD835` |
| `Yellow700`  | `0xFFFBC02D` |
| `Yellow800`  | `0xFFF9A825` |
| `Yellow900`  | `0xFFF57F17` |

### Orange

| Token        | Hex          |
|--------------|--------------|
| `Orange50`   | `0xFFFFF3E0` |
| `Orange100`  | `0xFFFFE0B2` |
| `Orange200`  | `0xFFFFCC80` |
| `Orange300`  | `0xFFFFB74D` |
| `Orange400`  | `0xFFFFA726` |
| `Orange500`  | `0xFFFF9800` |
| `Orange600`  | `0xFFFB8C00` |
| `Orange700`  | `0xFFF57C00` |
| `Orange800`  | `0xFFEF6C00` |
| `Orange900`  | `0xFFE65100` |

---

## Notes

- These are **raw values** — no semantic role (primary, error, surface, etc.)
- Assign semantic meaning in a separate semantic/alias token layer
- Alpha channel: `0xFF` = fully opaque, `0x00` = fully transparent
- Template: `~/.claude/skills/compose-design-tokens/templates/primitive-colors-template.kt`
