---
title: AGSL Integration with Jetpack Compose — Technical Reference
date: 2026-05-05
author: researcher
scope: AGSL API fundamentals, Compose integration paths, animation patterns, performance
---

# AGSL Compose Integration: Engineer's Reference

## 1. RuntimeShader API Basics

**Requirements:** API 33+ (minSdk bump confirmed)

### Constructor & Uniform Setters
```kotlin
// Instantiate from AGSL/SkSL source string
val shader = RuntimeShader(agslSourceCode)

// Set scalar uniforms
shader.setFloatUniform("time", elapsedMs / 1000f)
shader.setIntUniform("frameCount", frameNum)

// Set color uniform (packed ARGB int)
shader.setColorUniform("tint", Color.GREEN)

// Compose additional uniform float2, float3 via varargs
shader.setFloatUniform("resolution", width.toFloat(), height.toFloat())
```

**Error handling:** Wrap constructor in try-catch for compile-time errors (invalid AGSL → RuntimeException with Skia error message; surface in dev UI for iteration).

### AGSL Main Signature & Coordinate Space
```glsl
half4 main(float2 fragCoord) {
  // fragCoord is pixel coordinate, origin upper-left (Canvas coords)
  // NOT lower-left like GLSL
  return half4(color, 1.0);
}
```

**Coordinate conversion if needed:** Android's Y-axis flows top→bottom, opposite GLSL. Use matrix transforms to flip if porting GLSL shaders.

### Essential Intrinsics
```glsl
// Smoothstep (0→1 transition over range)
float smoothstep(float edge0, float edge1, float x)

// Linear interpolation
half4 mix(half4 x, half4 y, float a)

// Magnitude of vector
float length(float2 v)

// Fractional part (0.0–1.0)
float fract(float x)

// Component-wise min/max/clamp
float min(float x, float y)
float max(float x, float y)
float clamp(float x, float minVal, float maxVal)

// Color space (AGSL-specific)
half3 toLinearSrgb(half3 color)
half3 fromLinearSrgb(half3 color)
```

### Vector Swizzles & Precision Types
```glsl
vec4 v; v.xy; v.rgb; v.rgba; v.rgb1;  // rgb1 = (rgb, 1.0)

// Precision: prefer half (medium) for colors, float for geometry
half4 color;          // 16-bit, color operations
float2 position;      // 32-bit, transform/anim
```

---

## 2. Three Compose Integration Paths

### **Path A: ShaderBrush (fill shapes, backgrounds)**
Use for drawing fills on Canvas or Compose drawScope.

```kotlin
val shader = RuntimeShader(myAgslCode)
val brush = ShaderBrush(shader)

Canvas(modifier = Modifier.fillMaxSize()) {
  shader.setFloatUniform("iResolution", size.width, size.height)
  shader.setFloatUniform("iTime", timeMs / 1000f)
  
  // Draw with shader
  drawCircle(brush = brush, radius = 100f)
  drawRect(brush = brush)
}
```

**Gotchas:**
- Shader runs for *every pixel* in the shape; expensive for large surfaces.
- Uniforms set *before* draw call; no per-vertex state.
- Resolution uniform must match actual draw bounds for correct coordinate mapping.

---

### **Path B: graphicsLayer RenderEffect (post-process Composables)**

Post-process existing Composable output (blur, dissolve, displacement overlay).

```kotlin
val shader = RuntimeShader("""
  uniform shader content;
  uniform float2 iResolution;
  uniform float iTime;
  
  half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float2 offset = vec2(sin(iTime), cos(iTime)) * 0.05;
    return content.eval(fragCoord + offset * iResolution);
  }
""")

Box(
  modifier = Modifier
    .graphicsLayer {
      renderEffect = RenderEffect
        .createRuntimeShaderEffect(shader, "content")
        .asComposeRenderEffect()
    }
) {
  Text("Content to distort")
}
```

**Requirements:**
- `uniform shader content;` declared in AGSL (variable name passed to createRuntimeShaderEffect).
- `content.eval(fragCoord)` samples the Composable's rendered pixels.
- API 33 minimum; Compose 1.5+ recommended.

**Use cases:** Blur, ripple, chromatic aberration, dissolve, displacement maps.

---

### **Path C: Direct Canvas + Paint**

Low-level control; layer the shader paint on Canvas.

```kotlin
val paint = Paint().apply { shader = myRuntimeShader }

Canvas(modifier = Modifier.fillMaxSize()) {
  myShader.setFloatUniform("iResolution", size.width, size.height)
  drawIntoCanvas { canvas ->
    canvas.nativeCanvas.drawPaint(paint)
    // or drawRect, drawCircle, drawText with paint
  }
}
```

**When:** Full control over blending, layering, or mixing multiple shaders.

---

## 3. Driving Uniforms Over Time & Interaction

### **Animation: withFrameNanos + LaunchedEffect**

```kotlin
var timeMs by remember { mutableStateOf(0f) }

LaunchedEffect(Unit) {
  var startNanos = 0L
  while (isActive) {
    withFrameNanos { frameNanos ->
      if (startNanos == 0L) startNanos = frameNanos
      timeMs = (frameNanos - startNanos) / 1_000_000f // ms
      shader.setFloatUniform("iTime", timeMs / 1000f)  // seconds
    }
  }
}
```

**Alternative: produceState**
```kotlin
val timeMs = produceState(initialValue = 0f) {
  var startNanos = System.nanoTime()
  while (isActive) {
    withFrameNanos { frameNanos ->
      value = (frameNanos - startNanos) / 1_000_000f
    }
  }
}.value
```

### **Touch/Pointer Input → Uniforms**

```kotlin
var touchPos by remember { mutableStateOf(Offset.Zero) }

Box(
  modifier = Modifier
    .fillMaxSize()
    .pointerInput(Unit) {
      detectTapGestures { offset ->
        touchPos = offset
        shader.setFloatUniform("iTouchPos", offset.x, offset.y)
      }
    }
) { /* ... */ }
```

### **Slider → Parameter Playground**

```kotlin
var intensity by remember { mutableStateOf(0.5f) }

Slider(
  value = intensity,
  onValueChange = { intensity = it }
)

LaunchedEffect(intensity) {
  shader.setFloatUniform("iIntensity", intensity)
}
```

### **Performance: Mutate, Never Recreate**

```kotlin
// ✅ Correct: Remember shader, mutate uniforms
val shader = remember { RuntimeShader(agslCode) }

LaunchedEffect(timeMs) {
  shader.setFloatUniform("iTime", timeMs)  // cheap
}

// ❌ Wrong: Recreate per frame
LaunchedEffect(timeMs) {
  val newShader = RuntimeShader(agslCode)  // expensive, triggers compile
  shader.setFloatUniform("iTime", timeMs)
}
```

Shader instantiation is *expensive*; Skia compiles SkSL to GPU bytecode. Reuse the same instance, only mutate uniforms.

---

## 4. Sampling Composable Content (Input Shaders)

### **How setInputShader Works**

```kotlin
val shader = RuntimeShader("""
  uniform shader image;
  uniform float2 iResolution;
  
  half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    half4 src = image.eval(fragCoord);
    return half4(src.rgb * 2.0, src.a);  // brighten sampled pixels
  }
""")

// In Java/Kotlin, bind Composable via RenderEffect
Box(modifier = Modifier.graphicsLayer {
  renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "image")
    .asComposeRenderEffect()
}) {
  // Content here becomes the "image" uniform
}
```

### **Use Cases**

| Effect | Sampling Pattern |
|--------|------------------|
| **Blur** | Sample neighbors; average |
| **Chromatic Aberration** | Offset RGB samples by color channel |
| **Ripple** | Displace fragCoord by sin/cos wave from touch point |
| **Dissolve Transition** | Lerp between old & new via noise texture |
| **Displacement Map** | Offset fragCoord by another shader/texture |

### **Caveats**

- **Coordinate space:** `fragCoord` is pixels; `uniform float2 iResolution` must match rendered bounds for correct sampling.
- **Normalized coords:** If shader expects 0–1 UV, divide by iResolution: `float2 uv = fragCoord / iResolution`.
- **Sampling cost:** Each `image.eval()` re-renders the Composable at that coord; complex Content + complex sampling = frame drops. Profile on-device.

---

## 5. Performance & Debugging Gotchas

### **Compile Errors Surface at Runtime**

AGSL is compiled when `RuntimeShader(source)` is called, not at app compile time.

```kotlin
val shader = try {
  RuntimeShader(userProvidedAgslCode)
} catch (e: Exception) {
  Log.e("Shader", "Compile error: ${e.message}")  // Skia error output
  null
}
```

For lesson UI: wrap shader instantiation in error boundary, display error in dev mode.

### **AGSL ≠ GLSL: Notable Differences**

| Feature | AGSL | GLSL ES 1.0 |
|---------|------|-----------|
| `main()` signature | `half4 main(float2 fragCoord)` | `void main()` |
| Coordinate origin | Upper-left (Canvas) | Lower-left |
| `#define` | ❌ Unsupported | ✅ Use `const` instead |
| `discard` | ❌ Not allowed | ✅ Supported |
| Loops | Must be unrollable (compile-time bounds) | Runtime loops OK |
| Precision | `half` (medium), `float` (high) | `mediump`, `highp` |
| Color functions | `toLinearSrgb()`, `fromLinearSrgb()` | Manual conversion |

**Float vs. Half:** `half` is 16-bit, `float` is 32-bit. Use `half` for colors (saves memory), `float` for precision-critical geometry/time.

### **State & Redraw Triggers**

Shader is invalidated (re-rendered) when:
- Composable size/bounds change
- Any uniform is updated (via `setFloatUniform`, etc.)
- State read inside `graphicsLayer` block changes

Minimize state reads in `graphicsLayer` to avoid unnecessary redraws.

---

## 6. Canonical References

### **Official Android Docs**
- [Android Graphics Shading Language (AGSL) Overview](https://developer.android.com/develop/ui/views/graphics/agsl)
- [Using AGSL in Your App](https://developer.android.com/develop/ui/views/graphics/agsl/using-agsl)
- [AGSL Quick Reference](https://developer.android.com/develop/ui/views/graphics/agsl/agsl-quick-reference)
- [AGSL vs. GLSL Differences](https://developer.android.com/develop/ui/views/graphics/agsl/agsl-vs-glsl)
- [RuntimeShader API Reference](https://developer.android.com/reference/android/graphics/RuntimeShader)

### **Skia References**
- [Skia SkSL Documentation](https://skia.org/docs/user/sksl/)
- [Skia SkSL GitHub README](https://github.com/google/skia/blob/main/src/sksl/README.md)

### **Authoritative Blog Posts**
- [**Chet Haase** — "AGSL: Made in the Shade(r). RenderEffects #2"](https://medium.com/androiddevelopers/agsl-made-in-the-shade-r-7d06d14fe02a) (Jan 2024) — RenderEffect patterns, frosted glass, blur integration.
- [**Romain Guy** — "Finger Shadows in Compose"](https://www.romainguy.dev/posts/2025/finger-shadows/) (Dec 2025) — Displacement mapping, shadow effects, GPU programming in Compose.
- [**KINTO Tech Blog** — "Transform Android UI with AGSL: Custom Shaders Made Easy"](https://blog.kinto-technologies.com/posts/2024-12-15-AGSL/) (Dec 2024) — Practical examples, animation patterns.

### **Community Examples**
- [**drinkthestars/shady** (GitHub)](https://github.com/drinkthestars/shady) — Curated AGSL shader gallery for Compose; excellent reference implementations.
- [**JumpingKeyCaps/DynamicVisualEffectsAGSL** (GitHub)](https://github.com/JumpingKeyCaps/DynamicVisualEffectsAGSL) — Real-time shaders with touch & motion sensors in Compose.
- [**Mortd3kay/liquid-glass-compose** (GitHub)](https://github.com/Mortd3kay/liquid-glass-compose) — Glass morphism library; blur + distortion showcase.
- [**Composing Pixels — AGSL Shaders Compose** (Medium)](https://blog.realogs.in/composing-pixels/) — Pixel manipulation techniques in Compose.

---

## Unresolved Questions

1. **Vendor fragmentation at API 33–35:** Are there known driver bugs or performance cliffs on specific chipsets (Qualcomm, MediaTek, Exynos)? Worth testing on real devices during lesson execution module.

2. **SkSL feature parity:** Does AGSL support all SkSL builtins, or is there a published subset? (e.g., `atan`, `asin`, `acos` seem present but not explicitly listed in quick reference).

3. **Input shader sampling performance ceiling:** Practical limit on simultaneous `image.eval()` calls per shader before frame drops? (Blurs with 9+ samples observed OK, but no formal guidance.)

4. **Compose runtime shader caching:** Does Compose/Skia cache compiled shaders by source hash, or does re-instantiation always recompile? Affects lesson switching UX.

---

**Status:** DONE  
**Summary:** Collected comprehensive AGSL–Compose integration patterns across three drawing paths, uniform animation techniques, performance pitfalls, and coordinated authoritative & community references for lesson module design and engineer-facing documentation.

