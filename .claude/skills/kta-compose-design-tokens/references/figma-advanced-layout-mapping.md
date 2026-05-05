# Figma Advanced Layout Mapping

Extended Figma → Compose mapping for complex layouts beyond basic Column/Row.

## Auto-Layout Advanced

| Figma Property | Compose Equivalent |
|---------------|-------------------|
| Auto-layout direction: vertical | `Column(verticalArrangement = ...)` |
| Auto-layout direction: horizontal | `Row(horizontalArrangement = ...)` |
| Spacing (gap between items) | `Arrangement.spacedBy(X.dp)` |
| Space between (distribute) | `Arrangement.SpaceBetween` |
| Padding (uniform) | `Modifier.padding(X.dp)` |
| Padding (per side) | `Modifier.padding(start=, top=, end=, bottom=)` |
| Counter axis alignment: center | `horizontalAlignment = Alignment.CenterHorizontally` (Column) |
| Counter axis alignment: space-between | Nested `Row` with `Arrangement.SpaceBetween` |
| Wrap | `FlowRow {}` or `FlowColumn {}` (foundation) |
| Min width / Max width | `Modifier.widthIn(min = X.dp, max = Y.dp)` |
| Min height / Max height | `Modifier.heightIn(min = X.dp, max = Y.dp)` |

## Sizing & Constraints

| Figma | Compose |
|-------|---------|
| Fill container (horizontal) | `Modifier.fillMaxWidth()` |
| Fill container (vertical) | `Modifier.fillMaxHeight()` |
| Fill container (both) | `Modifier.fillMaxSize()` |
| Fixed W×H | `Modifier.size(W.dp, H.dp)` |
| Fixed width only | `Modifier.width(W.dp)` |
| Hug contents | `Modifier.wrapContentSize()` (default — omit modifier) |
| Aspect ratio lock | `Modifier.aspectRatio(W / H)` |
| Fixed + Fill mix | `Modifier.width(W.dp).fillMaxHeight()` |

## Absolute Positioning

| Figma | Compose |
|-------|---------|
| Absolute position child | `Box { child(Modifier.align(Alignment.X).offset(x.dp, y.dp)) }` |
| Constraints: top+left | `Modifier.align(Alignment.TopStart)` |
| Constraints: center | `Modifier.align(Alignment.Center)` |
| Constraints: bottom+right | `Modifier.align(Alignment.BottomEnd)` |
| Constraints: left+right (stretch) | `Modifier.fillMaxWidth().align(Alignment.CenterStart)` |

## Scroll Behavior

| Figma | Compose |
|-------|---------|
| Vertical scroll frame | `LazyColumn` (many items) or `Column(Modifier.verticalScroll(rememberScrollState()))` (few) |
| Horizontal scroll frame | `LazyRow` (many) or `Row(Modifier.horizontalScroll(...))` (few) |
| No scroll (clip content) | `Modifier.clip(shape)` with fixed size |
| Fixed header + scroll body | `Scaffold(topBar = { ... }) { LazyColumn(Modifier.padding(it)) }` |
| Sticky section header | `LazyColumn { stickyHeader { ... }; items(...) }` |

## Overlay & Z-Index

| Figma | Compose |
|-------|---------|
| Overlapping layers (stacked) | `Box { child1; child2; child3 }` — last child = top |
| Semi-transparent overlay | `Box { content; Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.5f))) }` |
| Fixed bottom element | `Scaffold(bottomBar = { ... })` or `Box { content; bar(Modifier.align(BottomCenter)) }` |
| Floating action button | `Scaffold(floatingActionButton = { ... })` |

## Component Variants → Composable Parameters

Figma components with variants map to Compose function parameters:

```
Figma variant properties:
  size: sm | md | lg
  state: enabled | disabled
  style: filled | outlined | text

→ Compose:
enum class ButtonSize { Sm, Md, Lg }
enum class ButtonStyle { Filled, Outlined, Text }

@Composable
fun {Prefix}Button(
    size: ButtonSize = ButtonSize.Md,
    style: ButtonStyle = ButtonStyle.Filled,
    enabled: Boolean = true,
    ...
)
```

**Boolean variants** → `Boolean` parameter (e.g., `hasIcon: Boolean`)
**Enum variants** → `enum class` or `sealed interface`
**Text variants** → `String` parameter with default

## Responsive Patterns

| Figma Frame Width | Device Class | Compose Pattern |
|------------------|-------------|-----------------|
| 360-412px | Compact (phone) | Single column, full-width components |
| 600-840px | Medium (tablet portrait) | 2-column grid or `NavigationRail` |
| 840px+ | Expanded (tablet landscape/desktop) | Side panel + content, `PermanentNavigationDrawer` |

```kotlin
BoxWithConstraints {
    when {
        maxWidth < 600.dp -> CompactLayout()
        maxWidth < 840.dp -> MediumLayout()
        else -> ExpandedLayout()
    }
}
```

Or use `WindowSizeClass` from `material3-window-size-class` library.

## Figma Effects → Compose

| Figma Effect | Compose |
|-------------|---------|
| Drop shadow | `Modifier.shadow(elevation = X.dp, shape = ...)` |
| Inner shadow | No direct equivalent — use `Modifier.border()` + gradient |
| Layer blur | `Modifier.blur(radius = X.dp)` (API 31+) |
| Background blur | Semi-transparent overlay (no native backdrop blur) |

## Image Fills

| Figma | Compose |
|-------|---------|
| Image fill: fill | `Image(contentScale = ContentScale.Crop)` |
| Image fill: fit | `Image(contentScale = ContentScale.Fit)` |
| Image fill: tile | Custom `drawBehind` with `ImageBitmap` |
| Clip to shape | `Image(modifier = Modifier.clip(shape))` |
| Circular image | `Image(modifier = Modifier.size(X.dp).clip(CircleShape))` |
