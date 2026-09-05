# AGSL + Compose Cheat Sheet

A practical reference for Android engineers who know Jetpack Compose but not shaders.
Everything below is distilled from the code and comments in this repository; each Kotlin
snippet names the file it was taken from, and each AGSL idiom names the lesson that teaches it
(ids match [`docs/catalog/lessons.json`](catalog/lessons.json) and the in-app lesson list).

## 1. What AGSL is

AGSL (Android Graphics Shading Language) is Skia's SkSL dialect exposed to Android apps. It runs
on the GPU once per pixel and is available on Android 13+ (API 33) — which is why this project
sets `minSdk = 33` in [`app/build.gradle.kts`](../app/build.gradle.kts).

| Building block | Class | What it does | Where this repo uses it |
|---|---|---|---|
| Compile | `android.graphics.RuntimeShader(source)` | Compiles AGSL; throws on a compile error | `AgslBrushCanvas`, `rememberRuntimeShader` |
| Paint | `androidx.compose.ui.graphics.ShaderBrush(shader)` | Fills any Compose draw call with the shader | `LessonRenderMode.BRUSH` (48 lessons) |
| Post-process | `RenderEffect.createRuntimeShaderEffect(shader, "content")` | Runs the shader over a composable's rendered pixels | `LessonRenderMode.RENDER_EFFECT` (6 Post-FX lessons) |
| Own the pipeline | `Modifier.runtimeShaderEffect` + gestures | Lesson-owned composable, no auto-uniform wiring | `LessonRenderMode.CUSTOM` (2 showcases) |

Every shader in the app is a Kotlin string in a lesson file
(`app/src/main/java/com/dantech/dreams/data/lesson/source/<category>/*.kt`); a Kotlin `object`
registers it as a `LessonModel` through `LessonRegistry` — see
[`system-architecture.md`](system-architecture.md).

## 2. AGSL vs GLSL, at a glance

| Topic | GLSL ES (WebGL / OpenGL) | AGSL | Seen in |
|---|---|---|---|
| Entry point | `void main()` writing `gl_FragColor` / an `out` variable | `half4 main(float2 fragCoord)` — the return value is the pixel | every lesson |
| Header | `#version`, `precision mediump float;` | none | every lesson |
| Vector types | `vec2 vec3 vec4`, `mat3` | `float2 float3 float4`, `float3x3`; also `half2..half4`, `int2..`, `bool2..` | every lesson |
| Precision | qualifiers (`lowp/mediump/highp`) | distinct types: `half` (reduced) vs `float` (full). Lessons cast explicitly when mixing them, e.g. `mix(inkA, inkB, half(v))` | `patterns-01-diagonal-stripes` |
| Colour uniforms | `uniform vec4 c;` | `layout(color) uniform half4 c;` — colour-managed by Skia, set with `setColorUniform` | `basics-01-solid` |
| Sampling other content | `uniform sampler2D s; texture(s, uv01)` | `uniform shader content; content.eval(coordPx)` — coordinates are in pixels, same space as `fragCoord` | `postfx-01-blur` |
| Pixel coordinate | `gl_FragCoord`, origin bottom-left | `fragCoord` parameter, local pixel space, origin top-left, **y grows downward** (`p.y = -p.y; // apex up: fragCoord grows downwards`) | `fractals-04-sierpinski` |
| Loops | dynamic bounds allowed | bounds must be compile-time constants (`const int MAX = 96;`, literals). Vary the *effective* count with `if (float(i) >= octaves) break;` | `noise-03-fbm`, `fractals-01-mandelbrot` |
| Array uniforms | `uniform float a[64];`, any index | `uniform float rip[64];` set from a `FloatArray`. Indices must be a constant multiple of the loop variable plus a constant — `rip[i * 4 + 1]` works, pre-computing `int b = i * 4; rip[b]` is rejected | `showcase-05-ripple-on-tap` |
| Extra intrinsics | — | `saturate()`, `unpremul()`, `toLinearSrgb()`, `fromLinearSrgb()` exist in SkSL (the web transpiler in [`tools/shader-catalog/agsl-to-glsl.mjs`](../tools/shader-catalog/agsl-to-glsl.mjs) polyfills them; no lesson currently uses them) | — |
| Shared built-ins | `sin cos atan(y,x) mix smoothstep step fract mod floor clamp length distance dot normalize reflect pow exp log2 abs sign min max sqrt` | identical | everywhere |
| Shadowing | allowed | allowed, but a code smell: `SLOTS deliberately not named N — local float3 N = normalize(...) later would shadow it` | `RippleOnTap.kt` |
| Out-of-bounds `eval` | depends on wrap mode | transparent black by default — clamp coordinates to the layer bounds to avoid dark fringes | `RippleOnTap.kt` `sampleSurface()` |

## 3. Uniform recipes used in this repo

Declare the uniform in AGSL, then write it from Kotlin. **Only ever write uniforms the source
declares** — see the CheckJNI note in section 4.

| Uniform | AGSL declaration | Kotlin write | Who writes it |
|---|---|---|---|
| Canvas size (px) | `uniform float2 resolution;` | `shader.setFloatUniform("resolution", w, h)` | `AgslBrushCanvas` / `AgslRenderEffectCanvas` in `onSizeChanged` |
| Time (seconds) | `uniform float time;` | `shader.setFloatUniform("time", time)` | `ShaderBindings.applyUniforms` from `rememberShaderTime` |
| Tap position (0..1 UV, `(-1,-1)` before any tap) | `uniform float2 touchPos;` | `shader.setFloatUniform("touchPos", x, y)` | `LessonPreview` via `detectTapGestures` |
| Tap time (seconds, `-1` before any tap) | `uniform float touchTime;` | `shader.setFloatUniform("touchTime", t)` | `LessonPreview` — `touchTime = time at tap` |
| Slider control | `uniform float radius;` + `LessonControl.FloatRange("Radius", "radius", min, max, default)` | `shader.setFloatUniform(c.uniformName, value)` | `ShaderBindings.applyUniforms` |
| Colour control | `layout(color) uniform half4 baseColor;` + `LessonControl.ColorPicker("Base color", "baseColor", Color(...))` | `shader.setColorUniform(name, color.toArgb())` | `ShaderBindings.applyUniforms` |
| Float array | `uniform float rip[64];` | `shader.setFloatUniform("rip", floatArray)` | `RippleTapDemo`, `CodexSplashWaterSurface` |
| Child shader | `uniform shader content;` | bound by `RenderEffect.createRuntimeShaderEffect(shader, "content")` | `AgslRenderEffectCanvas`, `Modifier.runtimeShaderEffect` |

Interactive lessons degrade gracefully before the first tap by testing the sentinel:

```glsl
// interactive-01-spotlight
float2 center = (touchPos.x < 0.0) ? float2(0.5) : touchPos;
// interactive-02-ripple
float age = max(time - touchTime, 0.0);
```

Post-FX lessons distort a real Compose subtree — `SampleContent()` (a gradient + text card in
[`ui/feature/common/SampleContent.kt`](../app/src/main/java/com/dantech/dreams/ui/feature/common/SampleContent.kt))
is passed as `postEffectContent` in `PostFxLessons.kt`.

## 4. Compose integration patterns

### 4.1 ShaderBrush in `drawBehind`

`app/src/main/java/com/dantech/dreams/ui/feature/common/AgslCanvas.kt`

```kotlin
val (shader, error) = remember(shaderSrc) {
    try {
        RuntimeShader(shaderSrc) to null
    } catch (t: Throwable) {
        null to (t.message ?: "compile error")
    }
}
if (error != null || shader == null) {
    AgslErrorCard(message = error ?: "unknown", modifier = modifier)
    return
}

val hasResolution = remember(shaderSrc) { declaresResolution(shaderSrc) }
var size by remember { mutableStateOf(IntSize.Zero) }
Box(
    modifier = modifier
        .fillMaxSize()
        .onSizeChanged {
            if (it != size) {
                size = it
                if (hasResolution) {
                    shader.setFloatUniform("resolution", it.width.toFloat(), it.height.toFloat())
                }
            }
        }
        .drawBehind {
            setUniforms(shader)
            drawRect(brush = ShaderBrush(shader))
        },
)
```

The comment above this function is the key rule: `setUniforms` runs **inside** `drawBehind`, where
Compose tracks snapshot-state reads, so reading a slider value or the animated time there
invalidates drawing when it changes. Setting uniforms outside the draw block does *not* redraw.

### 4.2 RenderEffect via `Modifier.runtimeShaderEffect`

`app/src/main/java/com/dantech/dreams/core/agsl/ShaderModifiers.kt`

```kotlin
fun Modifier.runtimeShaderEffect(
    shader: RuntimeShader,
    inputName: String = "content",
    onFrame: (Size) -> Unit = {},
): Modifier = graphicsLayer {
    onFrame(size)
    clip = true
    compositingStrategy = CompositingStrategy.Offscreen
    renderEffect = NativeRenderEffect
        .createRuntimeShaderEffect(shader, inputName)
        .asComposeRenderEffect()
}
```

Two gotchas, both documented in that file:

- `compositingStrategy = Offscreen` **is mandatory**. The RenderEffect needs an offscreen texture
  to sample for `content.eval(...)`; with the default `Auto` strategy the effect may render to
  nothing (blank preview). `RippleOnTap.kt` records that an early version generated its backdrop
  in-shader because `content.eval()` "came back blank" — the missing `Offscreen` was the root cause.
- The runtime shader effect **snapshots uniforms at construction time** (Skia's
  `SkRuntimeImageFilter` holds a frozen builder), so the effect is rebuilt inside the layer block.
  Every frame the block re-runs ships the latest uniform values. `AgslRenderEffectCanvas` does the
  same inside its own `graphicsLayer {}`.

### 4.3 Driving time with `withFrameNanos`

`app/src/main/java/com/dantech/dreams/core/agsl/RuntimeShaderUtils.kt`

```kotlin
@Composable
fun rememberShaderTime(): State<Float> = produceState(initialValue = 0f) {
    var start = 0L
    while (true) {
        val now = withFrameNanos { it }
        if (start == 0L) start = now
        value = (now - start) / 1_000_000_000f
    }
}
```

Read `time.value` inside the draw / `graphicsLayer` block; the state read is what forces the next
frame. Writing the uniform on the `RuntimeShader` alone never invalidates Compose drawing.

The lesson screens use a second flavour,
[`ui/feature/common/ShaderTimeUniform.kt`](../app/src/main/java/com/dantech/dreams/ui/feature/common/ShaderTimeUniform.kt)
`rememberShaderTime(shaderSource, uniformName = "time", paused = false)`, which stays at `0` when
the source declares no `time` uniform, accumulates frame deltas so it can pause and resume
seamlessly, and wraps at 1000 s (`% 1000f`) to keep float precision. It is deliberately **not**
gated by the animator-duration-scale setting — shader playback is content, not a UI animation.

### 4.4 Guard every `setFloatUniform` with declared-uniform detection

`app/src/main/java/com/dantech/dreams/ui/feature/common/ShaderUniformBindings.kt`

```kotlin
private val TIME_UNIFORM_RX = Regex("""uniform\s+float\s+time\s*;""")
private val TOUCH_POS_RX = Regex("""uniform\s+float2\s+touchPos\s*;""")
private val TOUCH_TIME_RX = Regex("""uniform\s+float\s+touchTime\s*;""")

private fun floatUniformRx(name: String) =
    Regex("""uniform\s+float\s+$name\s*;""")

private fun colorUniformRx(name: String) =
    Regex("""(?:layout\s*\(\s*color\s*\)\s+)?uniform\s+half4\s+$name\s*;""")
```

```kotlin
// abridged — see the file for the full signature
fun ShaderBindings.applyUniforms(shader: RuntimeShader, time: Float, /* … */) {
    if (hasTime) shader.setFloatUniform("time", time)
    if (hasTouch) {
        shader.setFloatUniform("touchPos", touchPosX, touchPosY)
        shader.setFloatUniform("touchTime", touchTime)
    }
    floats.forEach { c ->
        shader.setFloatUniform(c.uniformName, floatValues[c.uniformName] ?: c.default)
    }
    colors.forEach { c ->
        shader.setColorUniform(c.uniformName, (colorValues[c.uniformName] ?: c.default).toArgb())
    }
}
```

Why: writing a uniform the shader does not declare throws, and — as `AgslCanvas.kt` warns — under
CheckJNI it **aborts the process with a Modified-UTF-8 error before a `try/catch` can catch it**.
`rememberShaderBindings(lesson)` filters the lesson's controls down to those actually declared in
`agslSource`; `LessonRegistryTest` enforces the same rule at build time
(`lesson controls target declared uniforms`).

### 4.5 Tap → `touchPos` / `touchTime`, and pausing during navigation transitions

`app/src/main/java/com/dantech/dreams/ui/feature/lesson/LessonPreview.kt`

```kotlin
// Pause shader during nav transitions (forward enter, predictive-back peek,
// and post-commit fade) so the GPU is free for the system's scale animation.
val transition = LocalNavAnimatedContentScope.current.transition
val paused = transition.currentState != EnterExitState.Visible ||
    transition.targetState != EnterExitState.Visible
val timeState = rememberShaderTime(lesson.agslSource, paused = paused)
val touchPosUv = remember(lesson.id) { mutableStateOf(Offset(-1f, -1f)) }
val touchTime = remember(lesson.id) { mutableFloatStateOf(-1f) }
```

```kotlin
Modifier
    .fillMaxSize()
    .pointerInput(lesson.id) {
        detectTapGestures(
            onPress = { offset ->
                val w = size.width.toFloat()
                val h = size.height.toFloat()
                if (w > 0f && h > 0f) {
                    touchPosUv.value = Offset(offset.x / w, offset.y / h)
                    touchTime.floatValue = timeState.value
                }
            },
        )
    }
```

The pointer modifier is only attached when `bindings.hasTouch` is true (both `touchPos` and
`touchTime` declared).

### 4.6 Float-array uniforms per frame

`app/src/main/java/com/dantech/dreams/data/lesson/source/showcase/RippleOnTap.kt`

```kotlin
.runtimeShaderEffect(shader) { layerSize ->
    shader.setFloatUniform("iResolution", layerSize.width, layerSize.height)
    shader.setFloatUniform("iTime", time)
    shader.setFloatUniform("rip", ripples)   // FloatArray(16 * 4): x, y, t0, strength per slot
}
```

`ripples` is a plain `FloatArray` ring buffer mutated by `pointerInput`; the `time` snapshot read
in the block is what re-runs it each frame.

## 5. Recurring math idioms

| Idiom | AGSL (one line) | Lesson |
|---|---|---|
| Centered, aspect-correct UV | `float2 uv = fragCoord / resolution - 0.5; uv.x *= resolution.x / resolution.y;` | `basics-04-radial-gradient` |
| Centered UV, height-normalised | `float2 uv = (fragCoord - 0.5 * resolution) / resolution.y;` | `fractals-01-mandelbrot` |
| Tile space | `float2 g = fract(uv * cells) - 0.5;` | `patterns-02-polka-dots` |
| Cell id (stable per tile) | `float2 g = floor(uv * cells);` | `sdf-04-checkerboard` |
| Crisp two-colour mask | `float v = step(0.5, fract((uv.x + uv.y * skew) * count));` | `patterns-01-diagonal-stripes` |
| Polar coordinates | `float r = length(uv); float a = atan(uv.y, uv.x);` | `basics-05-polar-coords` |
| Angular fold (kaleidoscope) | `float folded = abs(mod(angle + wedge * 0.5, wedge) - wedge * 0.5);` | `patterns-06-kaleidoscope-fold` |
| Anti-aliased SDF edge | `float a = 1.0 - smoothstep(0.0, 0.005, d);` | `sdf-01-circle` |
| Circle SDF | `float sdCircle(float2 p, float r) { return length(p) - r; }` | `sdf-01-circle` |
| Rounded box SDF | `float2 q = abs(p) - b; return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;` | `sdf-02-rounded-box` |
| Smooth union (metaballs) | `float h = clamp(0.5 + 0.5 * (d2 - d1) / k, 0.0, 1.0); return mix(d2, d1, h) - k * h * (1.0 - h);` | `sdf-03-metaballs` |
| Isolines | `float bands = abs(fract(r * density) - 0.5);` | `sdf-06-isolines` |
| Hash | `fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453)` | `noise-01-hash` |
| Value noise | bilinear `mix` of four `hash21` corners with `u = f * f * (3.0 - 2.0 * f)` | `noise-02-value` |
| fBM | `for (int i = 0; i < 6; i++) { v += a * valueNoise(p); p *= 2.0; a *= 0.5; }` | `noise-03-fbm` |
| Domain warp | `float v = fbm(p + 4.0 * r);` where `r = float2(fbm(p + 4.0 * q + ...), ...)` | `noise-06-warped-lava` |
| Voronoi | scan the 3×3 neighbour cells, keep `min(dot(r, r))` | `noise-04-voronoi` |
| Cosine palette | `float3 col = a + b * cos(6.2831 * (c * t + d + shift));` | `color-01-cosine-palette` |
| HSV → RGB | `float3 k = mod(float3(5.0, 3.0, 1.0) + h * 6.0, 6.0); return v - v * s * max(min(min(k, 4.0 - k), 1.0), 0.0);` | `color-02-hsv-wheel` |
| ACES tone map | `clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0)` | `color-04-aces-tonemap` |
| Lambert diffuse | `float diff = max(dot(n, L), 0.0);` | `lighting-01-lambert` |
| Phong specular | `float3 R = reflect(-L, n); float spec = pow(max(dot(R, V), 0.0), shininess);` | `lighting-02-phong` |
| Rim light | `float rim = pow(1.0 - max(dot(n, V), 0.0), power);` | `lighting-03-rim` |
| Schlick Fresnel | `float fresnel = pow(1.0 - NdotV, 5.0);` | `showcase-05-ripple-on-tap` |
| Tap age | `float age = max(time - touchTime, 0.0);` | `interactive-02-ripple` |
| Chromatic aberration | sample `.r`, `.g`, `.b` of `content.eval` at `+dir`, `0`, `-dir` | `postfx-02-chromatic-aberration` |
| Pixelate | `float2 q = floor(fragCoord / cellSize) * cellSize + cellSize * 0.5;` | `postfx-06-pixelate` |
| Easing | `float easeOutCubic(float t) { float u = 1.0 - t; return 1.0 - u*u*u; }` | `motion-01-easing` |
| Complex multiply | `float2(a.x * b.x - a.y * b.y, a.x * b.y + a.y * b.x)` | `fractals-01-mandelbrot` |

Shared helper blocks are interpolated into lesson sources as Kotlin string templates:
`SDF_HELPERS` ([`sdf/SdfHelpers.kt`](../app/src/main/java/com/dantech/dreams/data/lesson/source/sdf/SdfHelpers.kt)),
`NOISE_HELPERS` ([`noise/NoiseHelpers.kt`](../app/src/main/java/com/dantech/dreams/data/lesson/source/noise/NoiseHelpers.kt)),
`FRACTAL_HELPERS` ([`fractals/FractalsHelpers.kt`](../app/src/main/java/com/dantech/dreams/data/lesson/source/fractals/FractalsHelpers.kt)).

## 6. Debugging tips

| Symptom | Cause / fix | Where |
|---|---|---|
| Red "AGSL compile error" card instead of a preview | `RuntimeShader(src)` threw; the message is Skia's compiler output. Both canvases catch it in `remember(shaderSrc)` and render `AgslErrorCard` | `AgslCanvas.kt`, `AgslErrorCard.kt` |
| Which lessons fail to compile? | `LessonRegistry.validateAll()` compiles `agslSource` plus every `extraAgslSources` entry and returns `(id, message)` pairs (extras are tagged `<id>#extra-<n>`). Debug builds run it from `DreamsApp.onCreate` and log to Logcat tags `LessonRegistry` and `LessonRepo` | `LessonRegistry.kt`, `DreamsApp.kt` |
| App crashes on slider drag / "Modified-UTF-8" abort | A uniform was written that the shader does not declare. Route writes through `rememberShaderBindings` + `applyUniforms`, never raw | `ShaderUniformBindings.kt`, `AgslCanvas.kt` |
| Post-FX preview is blank | Missing `compositingStrategy = CompositingStrategy.Offscreen` on the `graphicsLayer` | `ShaderModifiers.kt`, `RippleOnTap.kt` |
| Shader does not animate / slider has no effect | Uniforms are set outside the draw or layer block, or the RenderEffect is not rebuilt per frame | `AgslCanvas.kt` doc comments |
| Dark fringes where a distortion samples past the edge | Out-of-bounds `content.eval` returns transparent black; clamp coordinates: `clamp(coord, float2(0.0), iResolution - float2(1.0))` | `RippleOnTap.kt` `sampleSurface()` |
| Compile error on an array index | Use `rip[i * 4 + k]` directly in the loop; do not hoist the index into a variable | `RippleOnTap.kt` |
| Unit tests pass but the shader is broken | `./gradlew test` (JVM) never instantiates `RuntimeShader`; `LessonRegistryTest` checks counts, ordering, learning notes and control-to-uniform matching with regexes. Real compilation happens on a device (`validateAll`) or in WebGL (below) | `app/src/test/.../LessonRegistryTest.kt` |
| No device at hand | `node tools/shader-catalog/render-thumbnails.mjs --check` transpiles every lesson to GLSL ES 3.00 and compiles it in headless Chromium on SwiftShader (software GL, works on GPU-less CI). It catches syntax and most type errors but does not emulate `half` precision, colour management, or the AGSL array-index rule | `tools/shader-catalog/` |
| Emulator is slow or unreliable | `RuntimeShader` runs through HWUI's Skia GPU backend; prefer an AVD with hardware graphics acceleration or a physical device. The project docs only count a physical device or a real emulator as executing shaders for real | `codebase-summary.md` |

Tooling requirements: Node.js 20+ and Playwright with Chromium
(`npm i -D playwright && npx playwright install chromium` inside `tools/shader-catalog`, or a
global install). `node tools/shader-catalog/extract-lessons.mjs` regenerates
`docs/catalog/lessons.json` from the Kotlin lesson files; `node tools/shader-catalog/build-site.mjs`
builds the GitHub Pages gallery (`docs/index.html`) with live WebGL previews.

## 7. Links

- Official AGSL guide: <https://developer.android.com/develop/ui/views/graphics/agsl>
- AGSL quick reference: <https://developer.android.com/develop/ui/views/graphics/agsl/agsl-quick-reference>
- `RuntimeShader` API: <https://developer.android.com/reference/android/graphics/RuntimeShader>
- `RenderEffect` API: <https://developer.android.com/reference/android/graphics/RenderEffect>
- SkSL (the language AGSL is derived from): <https://skia.org/docs/user/sksl/>
- The Book of Shaders: <https://thebookofshaders.com>
- Inigo Quilez — articles: <https://iquilezles.org/articles/>
  (2D SDFs: <https://iquilezles.org/articles/distfunctions2d/>, cosine palettes:
  <https://iquilezles.org/articles/palettes/>, domain warping: <https://iquilezles.org/articles/warp/>)
- In this repo: [`README.md`](../README.md), [`system-architecture.md`](system-architecture.md),
  [`code-standards.md`](code-standards.md), [`catalog/lessons.json`](catalog/lessons.json)
