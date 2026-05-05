# Animation Optimization Checks

Project rule (CLAUDE.md): every state change must animate. Snap-cuts are bugs. Min 150ms micro / 300ms screen.

## 1. Conditional render without transition

**Pattern**
```kotlin
if (isLoading) Spinner() else Content(data)
```
Hard swap → flicker.

**Fix**
```kotlin
AnimatedContent(targetState = isLoading, label = "loading") { loading ->
    if (loading) Spinner() else Content(data)
}
// or Crossfade for simple two-way swap
Crossfade(targetState = isLoading, label = "loading") { ... }
```

## 2. Visibility toggle without `AnimatedVisibility`

**Pattern**
```kotlin
if (showHint) HintCard()
```

**Fix**
```kotlin
AnimatedVisibility(
    visible = showHint,
    enter = fadeIn() + slideInVertically { -it / 2 },
    exit = fadeOut() + slideOutVertically { -it / 2 },
) { HintCard() }
```

## 3. Color change without animation

**Pattern**
```kotlin
val color = if (selected) primary else outline
Box(Modifier.background(color))
```

**Fix**
```kotlin
val color by animateColorAsState(
    targetValue = if (selected) primary else outline,
    animationSpec = tween(200),
    label = "selectColor",
)
```

## 4. Size / dp change without animation

**Pattern**
```kotlin
val size = if (expanded) 200.dp else 80.dp
Box(Modifier.size(size))
```

**Fix**
```kotlin
val size by animateDpAsState(
    targetValue = if (expanded) 200.dp else 80.dp,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "expandSize",
)
```

Or use `Modifier.animateContentSize()` for content-driven size.

## 5. Rotation without animation

**Pattern**
```kotlin
Icon(Modifier.rotate(if (expanded) 180f else 0f))
```

**Fix**
```kotlin
val angle by animateFloatAsState(
    targetValue = if (expanded) 180f else 0f,
    animationSpec = tween(250, easing = FastOutSlowInEasing),
    label = "chevronRotate",
)
Icon(Modifier.rotate(angle))
```

## 6. Offset / position jump

**Pattern**
```kotlin
Box(Modifier.offset(x = if (active) 16.dp else 0.dp))
```

**Fix**
```kotlin
val offsetX by animateDpAsState(if (active) 16.dp else 0.dp, label = "indicator")
Box(Modifier.offset(x = offsetX))
```

For continuous gesture-driven motion use `Animatable` + `animateTo`.

## 7. List item enter/exit/move

**Pattern**
```kotlin
LazyColumn { items(list, key = { it.id }) { Row(it) } }
```
Items pop in/out.

**Fix**
```kotlin
LazyColumn {
    items(list, key = { it.id }) { item ->
        Row(item, Modifier.animateItem())
    }
}
```

## 8. Alpha / fade flicker

Use `animateFloatAsState` for alpha or `Modifier.alpha(animatedAlpha)`. Avoid recomposing parent — animate inside the leaf.

## 9. Repeating / looping animation

For pulses, shimmer, indeterminate progress:
```kotlin
val infinite = rememberInfiniteTransition(label = "pulse")
val scale by infinite.animateFloat(
    initialValue = 1f,
    targetValue = 1.1f,
    animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
    label = "pulseScale",
)
```

## 10. Easing + duration discipline

| Use case | Spec |
|----------|------|
| Micro-interaction (toggle, ripple) | `tween(150–200, FastOutSlowInEasing)` |
| Standard transition | `tween(250–300, FastOutSlowInEasing)` |
| Screen / large surface | `tween(300–400, EaseOutCubic)` |
| Bouncy / playful | `spring(dampingRatio = MediumBouncy, stiffness = Low)` |
| Snappy controls | `spring(stiffness = StiffnessMediumLow)` |

Project may expose `<Prefix>MotionPresets` — prefer it over raw `tween`/`spring` literals.

## 11. Shared element / nav transition

For Compose Navigation 2.8+, use `SharedTransitionLayout` + `Modifier.sharedElement(...)`. Never snap between screens — use `enterTransition`/`exitTransition` lambdas on `composable()`.

## 12. Animation `label` parameter

Always pass `label = "..."` to `animate*AsState` / `AnimatedContent` / `Crossfade` — required for Layout Inspector debugging.

## Output format for findings

```
[animation][high] L88: <state name> changes without transition
  Issue: <one sentence>
  Fix: <named API + spec>, e.g. animateColorAsState(tween(200))
```
