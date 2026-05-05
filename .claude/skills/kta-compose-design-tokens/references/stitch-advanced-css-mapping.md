# Stitch Advanced CSS-to-Compose Mapping

Extended HTML/CSS → Compose mapping for Stitch-generated screens.

## CSS Grid → Compose

| CSS | Compose |
|-----|---------|
| `display: grid; grid-template-columns: repeat(2, 1fr)` | `LazyVerticalGrid(columns = GridCells.Fixed(2))` |
| `grid-template-columns: repeat(auto-fill, minmax(150px, 1fr))` | `LazyVerticalGrid(columns = GridCells.Adaptive(150.dp))` |
| `grid-template-columns: 1fr 2fr` | `Row { Box(Modifier.weight(1f)); Box(Modifier.weight(2f)) }` |
| `grid-gap: 16px` | `Arrangement.spacedBy(16.dp)` in both directions |
| `grid-column: span 2` | `item(span = { GridItemSpan(2) })` in `LazyVerticalGrid` |

## Flexbox Advanced → Compose

| CSS | Compose |
|-----|---------|
| `display: flex; flex-direction: column` | `Column` |
| `display: flex; flex-direction: row` | `Row` |
| `flex-wrap: wrap` | `FlowRow` (foundation library) |
| `flex-wrap: wrap` + column | `FlowColumn` |
| `justify-content: center` | `Arrangement.Center` |
| `justify-content: space-between` | `Arrangement.SpaceBetween` |
| `justify-content: space-around` | `Arrangement.SpaceAround` |
| `justify-content: space-evenly` | `Arrangement.SpaceEvenly` |
| `align-items: center` | `verticalAlignment = Alignment.CenterVertically` (Row) |
| `align-items: stretch` | `Modifier.fillMaxHeight()` on children (Row) |
| `flex: 1` | `Modifier.weight(1f)` |
| `flex: 0 0 auto` | `Modifier.wrapContentWidth()` (default) |
| `gap: 16px` | `Arrangement.spacedBy(16.dp)` |
| `order: N` | Reorder children in composable code |

## Position → Compose

| CSS | Compose |
|-----|---------|
| `position: relative` container | `Box` |
| `position: absolute; top: 0; left: 0` | `Box { child(Modifier.align(Alignment.TopStart)) }` |
| `position: absolute; bottom: 16px; right: 16px` | `Box { child(Modifier.align(Alignment.BottomEnd).padding(16.dp)) }` |
| `position: fixed` (bottom bar) | `Scaffold(bottomBar = { ... })` |
| `position: sticky` (header) | `LazyColumn { stickyHeader { ... } }` |
| `z-index: 1` | Child ordering in `Box` — later children render on top |
| `transform: translate(x, y)` | `Modifier.offset(x.dp, y.dp)` |

## Visual Effects → Compose

| CSS | Compose |
|-----|---------|
| `box-shadow: 0 2px 8px rgba(0,0,0,0.1)` | `Modifier.shadow(elevation = 4.dp, shape = shape)` |
| `border: 1px solid #ccc` | `Modifier.border(1.dp, Color(0xFFCCCCCC), shape)` |
| `border-radius: 12px` | Shape tokens → `RoundedCornerShape(12.dp)` |
| `border-radius: 50%` | `CircleShape` |
| `opacity: 0.5` | `Modifier.alpha(0.5f)` |
| `background: linear-gradient(...)` | `Modifier.background(Brush.linearGradient(...))` |
| `background: radial-gradient(...)` | `Modifier.background(Brush.radialGradient(...))` |
| `backdrop-filter: blur(10px)` | No native equivalent — use semi-transparent `Surface` |
| `overflow: hidden` | `Modifier.clip(shape)` |
| `overflow: auto` / `scroll` | `Modifier.verticalScroll(rememberScrollState())` |

## Typography → Compose

| CSS | Compose |
|-----|---------|
| `font-size: 16px` | `fontSize = 16.sp` |
| `font-weight: 400` | `fontWeight = FontWeight.Normal` |
| `font-weight: 600` | `fontWeight = FontWeight.SemiBold` |
| `font-weight: 700` | `fontWeight = FontWeight.Bold` |
| `line-height: 1.5` | `lineHeight = 24.sp` (fontSize × 1.5) |
| `letter-spacing: 0.5px` | `letterSpacing = 0.5.sp` |
| `text-transform: uppercase` | Apply `.uppercase()` to string |
| `text-align: center` | `textAlign = TextAlign.Center` |
| `text-overflow: ellipsis` + `overflow: hidden` | `overflow = TextOverflow.Ellipsis, maxLines = 1` |
| `white-space: nowrap` | `maxLines = 1` |
| `text-decoration: underline` | `textDecoration = TextDecoration.Underline` |
| `text-decoration: line-through` | `textDecoration = TextDecoration.LineThrough` |

## Transitions & Animations → Compose

| CSS | Compose |
|-----|---------|
| `transition: opacity 300ms ease` | `animateFloatAsState(targetValue, tween(300))` |
| `transition: transform 200ms` | `animateDpAsState(targetDp, tween(200))` |
| `transition: background-color 150ms` | `animateColorAsState(targetColor, tween(150))` |
| `@keyframes fadeIn` | `AnimatedVisibility(enter = fadeIn())` |
| `@keyframes slideUp` | `AnimatedVisibility(enter = slideInVertically())` |
| `animation: spin 1s linear infinite` | `InfiniteTransition` + `animateFloat` |
| `:hover` state change | No hover on mobile — map to press/focus state |

## Common Stitch Patterns

### Card Grid
```html
<div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px">
  <div class="card">...</div>
```
→
```kotlin
LazyVerticalGrid(
    columns = GridCells.Fixed(2),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
) {
    items(cards) { card -> {Prefix}Card { CardContent(card) } }
}
```

### Hero Section
```html
<section style="position: relative; min-height: 400px; background: url(...)">
  <div style="position: absolute; bottom: 32px; left: 24px">
```
→
```kotlin
Box(Modifier.fillMaxWidth().heightIn(min = 400.dp)) {
    Image(contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
    Column(Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 32.dp)) {
        // Hero text content
    }
}
```
