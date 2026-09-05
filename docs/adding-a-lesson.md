# Adding a lesson

A lesson is one Kotlin `object` holding one AGSL program plus the metadata the app and the web gallery show around it. Everything below was checked against the code in this repo; file paths are relative to the repo root. Read the [AGSL cheatsheet](agsl-cheatsheet.md) first if AGSL is new to you.

## Checklist

1. Pick a category package and the next free id (`<prefix>-<NN>-<slug>`).
2. Create `app/src/main/java/com/dantech/dreams/data/lesson/source/<package>/<ObjectName>.kt`.
3. Declare every uniform your controls write; declare `resolution`/`time`/`touchPos`+`touchTime` only if you use them.
4. Add `<ObjectName>.id` to the category's `*Bootstrap.touch()`.
5. Update `LessonRegistryTest.kt` (count, and the ordering list for Basics/Patterns).
6. `./gradlew test`, then `./gradlew :app:installDebug` and look at the preview on a device.
7. Regenerate the catalog, README table, gallery site and thumbnail; commit the generated files.

## 1. Choose a category

| `LessonCategory` | Package under `data/lesson/source/` | Bootstrap object | id prefix | Lessons today | Extra test rules |
|---|---|---|---|:-:|---|
| `BASICS` | `basics` | `BasicsBootstrap` | `basics` | 6 | fixed order, exactly 3 learning notes |
| `PATTERNS` | `patterns` | `PatternsBootstrap` | `patterns` | 10 | fixed order, exactly 3 learning notes |
| `COLOR` | `colorlab` | `ColorBootstrap` | `color` | 4 | |
| `SDF` | `sdf` | `SdfBootstrap` | `sdf` | 6 | |
| `NOISE` | `noise` | `NoiseBootstrap` | `noise` | 6 | |
| `MOTION` | `motion` | `MotionBootstrap` | `motion` | 4 | |
| `FRACTALS` | `fractals` | `FractalsBootstrap` | `fractals` | 4 | |
| `LIGHTING` | `lighting` | `LightingBootstrap` | `lighting` | 4 | |
| `INTERACTIVE` | `interactive` | `InteractiveBootstrap` | `interactive` | 4 | |
| `POSTFX` | `posteffect` | `PostFxBootstrap` | `postfx` | 6 | `RENDER_EFFECT` lessons |
| `SHOWCASE` | `showcase` | `ShowcaseBootstrap` | `showcase` | 2 | `CUSTOM` lessons, own tab |

Ids are `<prefix>-<NN>-<slug>`: two-digit `NN` continuing the category's numbering (Showcase currently uses `05` and `06`), lowercase kebab-case slug, e.g. `basics-05-polar-coords`, `postfx-02-chromatic-aberration`. The id must be unique across the whole registry (`LessonRegistry.register` throws `Duplicate lesson id`), and it becomes the thumbnail filename `docs/gallery/<id>.png` and the gallery deep link `#<id>`, so pick it once. All current ids are listed in [`docs/catalog/lessons.json`](catalog/lessons.json).

Some categories share AGSL helper snippets as Kotlin string constants that you splice into your source with a template: `$NOISE_HELPERS` (`hash21`, `valueNoise`, `fbm` in `noise/NoiseHelpers.kt`, also imported by Patterns and Post-FX), `$SDF_HELPERS` (`sdf/SdfHelpers.kt`) and `$FRACTAL_HELPERS` (`fractals/FractalsHelpers.kt`). Put the template above `main()`, after your uniforms, as `noise/NoiseLessons.kt` does.

## 2. Create the lesson file

One `object` per file, following `basics/PolarCoords.kt`. This complete example adds an eleventh Patterns lesson (it is not in the repo; adapt names and package to your category):

```kotlin
// app/src/main/java/com/dantech/dreams/data/lesson/source/patterns/ConcentricRings.kt
package com.dantech.dreams.data.lesson.source.patterns

import androidx.compose.ui.graphics.Color
import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonControl
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRegistry
import kotlinx.collections.immutable.persistentListOf

object ConcentricRings {
    val id = "patterns-11-concentric-rings"

    private val SOURCE = """
        uniform float2 resolution;
        uniform float time;
        uniform float rings;
        layout(color) uniform half4 ink;

        half4 main(float2 fragCoord) {
            // Centre the origin and divide by height so rings stay circular at any aspect.
            float2 uv = (fragCoord - 0.5 * resolution) / resolution.y;
            float r = length(uv);
            // fract() repeats the radius `rings` times; subtracting time drifts the phase outward.
            float band = fract(r * rings - time * 0.5);
            // Threshold the 0..1 ramp into crisp rings, softened over one pixel.
            float px = rings / resolution.y;
            float mask = smoothstep(0.5 - px, 0.5 + px, band);
            half3 bg = half3(0.05, 0.06, 0.10);
            half3 col = mix(bg, ink.rgb, half(mask));
            return half4(col, 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id,
                title = "Concentric Rings",
                category = LessonCategory.PATTERNS,
                complexity = 2,
                conceptIntro = "Repeat the radius with fract() and threshold it: the polar cousin of diagonal stripes.",
                learningNotes = persistentListOf(
                    "Dividing by resolution.y instead of resolution keeps the rings round on every screen.",
                    "fract(r * rings) restarts a 0..1 ramp every 1/rings units of radius.",
                    "smoothstep over one pixel (rings / resolution.y) anti-aliases the edge for free.",
                ),
                agslSource = SOURCE,
                controls = persistentListOf(
                    LessonControl.FloatRange("Rings", "rings", 2f, 40f, 12f),
                    LessonControl.ColorPicker("Ink", "ink", Color(0xFF35F6FF)),
                ),
                screenRecordingHint = "Sweep Rings from 2 to 40 for a zoom-tunnel feel.",
            )
        )
    }
}
```

### `LessonModel` fields (`data/lesson/LessonModel.kt`)

| Field | Required | Notes |
|---|:-:|---|
| `id`, `title`, `category`, `conceptIntro`, `agslSource` | yes | The test `every lesson has non-blank source` fails on a blank title, intro or source |
| `complexity` | yes | `Int`; use 1..5, shown as bolts in the app and README |
| `learningNotes` | no | `ImmutableList<String>`; the "What to notice" box. Basics and Patterns tests require exactly 3 non-blank notes |
| `controls` | no | `LessonControl.FloatRange(name, uniformName, min, max, default)` or `LessonControl.ColorPicker(name, uniformName, default: Color)` |
| `renderMode` | no | `BRUSH` (default), `RENDER_EFFECT`, `CUSTOM` — see below |
| `screenRecordingHint` | no | Shown as a "Recording hint" in the detail screen |
| `extraAgslSources` | no | Additional programs a `CUSTOM` preview compiles itself; also compiled by `LessonRegistry.validateAll()` |
| `postEffectContent`, `customPreview` | mode-specific | Composable lambdas for `RENDER_EFFECT` / `CUSTOM` |

Use named arguments for every `LessonModel` parameter: the catalog extractor rejects positional ones.

## 3. Uniforms: what the runtime writes, and how it decides

The preview never writes a uniform the shader does not declare (writing an undeclared uniform throws, and under CheckJNI aborts the process). Detection is by regex over `agslSource`, so **declare each uniform on its own line, exactly in this shape**:

| Uniform | Regex (source file) | Value |
|---|---|---|
| `uniform float2 resolution;` | `uniform\s+float2\s+resolution\s*;` (`ui/feature/common/AgslCanvas.kt`) | Preview size in px, set on size change |
| `uniform float time;` | `uniform\s+float\s+time\s*;` (`ShaderUniformBindings.kt`, `ShaderTimeUniform.kt`) | Seconds since the preview opened, wraps at 1000, paused during nav transitions; stays 0 if not declared |
| `uniform float2 touchPos;` **and** `uniform float touchTime;` | `uniform\s+float2\s+touchPos\s*;` + `uniform\s+float\s+touchTime\s*;` | Last tap in 0..1 UV and the `time` of that tap; `(-1,-1)` / `-1` before any tap. Both must be present or the tap gesture is not installed (`LessonPreview.kt`) |
| `uniform float <name>;` | `uniform\s+float\s+<name>\s*;` | Value of the `FloatRange` control with that `uniformName` |
| `layout(color) uniform half4 <name>;` | `(?:layout\s*\(\s*color\s*\)\s+)?uniform\s+half4\s+<name>\s*;` | Value of the `ColorPicker` control (`setColorUniform`); `layout(color)` makes Skia convert the colour into the working colour space |
| `uniform shader content;` | bound by name via `RenderEffect.createRuntimeShaderEffect(shader, "content")` | Post-FX only: the Compose subtree under the effect, read with `content.eval(coord)` |

`LessonRegistryTest.lesson controls target declared uniforms` applies the same two control regexes, so a control whose uniform is missing, has a different type (`half` instead of `float`), or is declared in a comma list (`uniform float a, b;`) fails the build. Conversely a declared uniform with no control and no auto-binding is never written — give it a control or turn it into a constant. Do not leave uniform declarations inside comments: the Kotlin regexes see them, the catalog extractor strips comments first, and the two will disagree.

## 4. Render modes

| Mode | Preview | You provide |
|---|---|---|
| `BRUSH` (default) | `AgslBrushCanvas`: `ShaderBrush(shader)` fills the square preview via `drawRect` inside `drawBehind` | Just `agslSource` |
| `RENDER_EFFECT` | `AgslRenderEffectCanvas`: your shader runs as a `RenderEffect` over `postEffectContent`, with `compositingStrategy = Offscreen` and the effect rebuilt every frame so fresh uniforms reach Skia | `uniform shader content;` in the source, `postEffectContent = { SampleContent() }` (`ui/feature/common/SampleContent.kt`, the gradient-plus-card every Post-FX lesson distorts). Reuse the `postFxLesson(...)` factory in `posteffect/PostFxLessons.kt`, which sets the mode, category and content for you; it is `private`, so add your object to that same file. `fragCoord` is in pixels, so pixel-based controls like `radius` are in px |
| `CUSTOM` | `LessonPreview` calls `customPreview` and nothing else: no auto-uniforms, no tap handling | A composable that owns its `RuntimeShader`, clock (`core/agsl/RuntimeShaderUtils.kt: rememberShaderTime()`), gestures and, for effects, `Modifier.runtimeShaderEffect` (`core/agsl/ShaderModifiers.kt`). Put secondary programs in `extraAgslSources` (they are compiled by `validateAll()` on device and by `npm run check` in CI). See `showcase/RippleOnTap.kt` and `showcase/CodexSplashShowcase.kt` |

The web tooling knows the two existing showcases by id; a new `CUSTOM` lesson is previewed in the gallery as a single brush pass of `agslSource`, so make that source a meaningful standalone frame (the backdrop, say).

## 5. Register, test, run

1. **Bootstrap** — add `ConcentricRings.id` as its own line in `patterns/PatternsBootstrap.kt`'s `touch()`. `LessonRegistry.bootstrap()` calls every Bootstrap; touching `id` runs the object's `init`, which registers the lesson. `byCategory()` preserves registration order, so the position in `touch()` is the position in the app.
2. **Tests** — in `app/src/test/java/com/dantech/dreams/data/lesson/LessonRegistryTest.kt` bump the category's count in `each category has expected lesson count` (Patterns: `10` → `11`). For Basics or Patterns also append the id to the list in `basics lessons stay in learning order` / `patterns lessons stay in learning order`, and make sure `learningNotes` has exactly 3 entries.
3. **Run** — `./gradlew test` (JVM only, no device) and `./gradlew :app:assembleDebug`. On a device (`./gradlew :app:installDebug`, Android 13+), a compile error shows as an error card in the preview, and debug builds log every failing program at startup under the tag `LessonRepo` (`DreamsApp.kt` calls `repo.validate()`).

## 6. Regenerate the catalog and thumbnail

`docs/catalog/lessons.json` is the single source of truth for the README table, the gallery site and the thumbnails. Requirements: Node.js 20+ and Playwright with Chromium.

```bash
cd tools/shader-catalog
npm install && npx playwright install chromium      # once
npm run extract                                     # docs/catalog/lessons.json
npm run check                                       # every lesson compiles under WebGL2 (AGSL → GLSL)
node render-thumbnails.mjs --only patterns-11-concentric-rings   # docs/gallery/<id>.png
npm run readme                                      # README.md catalog section
npm run site                                        # docs/index.html
```

`npm run all` does extract + every thumbnail and the hero poster + readme + site in one go. If the default frame (t = 2.5 s, default slider values, a simulated tap at (0.62, 0.42) for touch lessons) looks dull, add an entry to `THUMB_STATES.overrides` in `thumb-states.mjs` (`time`, `touch: {x, y, t}`, `values: { uniformName: number }` — or an `"#AARRGGBB"` string for a colour uniform) and re-render. Commit `docs/catalog/lessons.json`, `docs/gallery/<id>.png`, `README.md` and `docs/index.html`: CI (`.github/workflows/ci.yml`) reruns `npm run catalog` and fails if any of `lessons.json`, `docs/index.html` or `README.md` differ, then runs `npm run check`.

The extractor is a small Kotlin reader tuned to the lesson conventions. Keep these true so it, and CI, stay happy:

- `val id = "..."` is a string literal; `private val SOURCE = """ ... """.trimIndent()` holds the program.
- Templates it resolves: `$NAME` for a raw-string or `const val` int constant, `${fn()}` for a `fun fn() = """..."""` helper (like `${centeredUv()}` in `sdf/SdfLessons.kt`), and `${A * B}` arithmetic on int constants. Constant names are global to the source tree, so keep them unique.
- Colours are written as `Color(0xAARRGGBB)` literals; controls as `LessonControl.FloatRange(...)` / `LessonControl.ColorPicker(...)`.
- Register via `LessonModel(...)` or `postFxLesson(...)`; other factory functions are rejected.
- Every lesson object must be touched by a Bootstrap, and every Bootstrap entry must exist; `npm run extract` fails otherwise, so a forgotten `touch()` line is caught even before the Kotlin test.

## AGSL style checklist

- Start with a comment that states the one idea the lesson teaches; add short comments at each non-obvious step (the source viewer shows them).
- Normalise `fragCoord` with `resolution` and correct for aspect (`uv.x *= resolution.x / resolution.y`, or divide by `resolution.y`) so the square preview and a phone screen agree.
- Loops need a constant bound (`for (int i = 0; i < 6; i++)`); use `break` on a uniform (`if (float(i) >= octaves) break;`) as `noise-03-fbm` does. Uniform-array indices must be a constant multiple of the loop variable plus a constant, e.g. `rip[i * 4 + 1]` (see the comment in `showcase/RippleOnTap.kt`).
- Compute geometry in `float`, colours in `half`/`half3`/`half4`, and cast explicitly where they meet (`mix(a, b, half(v))`).
- Return opaque `half4(col, 1.0)` unless transparency is the point (Post-FX `Dissolve` returns alpha `c.a * half(a)`).
- Read every uniform you declare; keep slider ranges meaningful across the whole range, and pick a `default` that photographs well.
- No `#version`, `precision`, `gl_FragCoord`, `texture()` or `sampler2D` — AGSL has `half4 main(float2 fragCoord)`, `uniform shader` + `.eval()`, and `layout(color)`. The gallery transpiler renames identifiers that collide with GLSL reserved words or built-ins, so prefer names that are not GLSL keywords.

## Adding a whole new category

1. `data/lesson/LessonCategory.kt`: add `NEWCAT("Display name", "tagline")` before the `;`. Enum order is card order in the Lesson tab (`lessonOnly()` excludes only `SHOWCASE`). Keep the constant to capital letters on one line: `extract-lessons.mjs` reads the enum with `^\s*([A-Z]+)\("([^"]+)",\s*"([^"]+)"\)`.
2. Create `data/lesson/source/<newcat>/NewCatBootstrap.kt` with `object NewCatBootstrap { fun touch() { FirstLesson.id } }` — one `Object.id` per line — and at least one lesson file.
3. `data/lesson/LessonRegistry.kt`: add `com.dantech.dreams.data.lesson.source.<newcat>.NewCatBootstrap.touch()` to `bootstrap()`, fully qualified like the existing lines (the extractor derives registration order from `source\.(\w+)\.(\w+)Bootstrap\.touch\(\)`).
4. `ui/theme/Color.kt`: add `val AccentNewCat = Color(0xFF......)` next to the other per-category accents; `ui/theme/CategoryStyle.kt`: add `LessonCategory.NEWCAT -> AccentNewCat` to the exhaustive `when` (the build fails without it).
5. `LessonRegistryTest.kt`: add `assertEquals(n, repo.byCategory(LessonCategory.NEWCAT).size)`; add ordering and learning-notes tests if the category is meant to be read in sequence.
6. Rerun the section 6 commands; `lessons.json` and the README table pick the category up from the enum. The "N lessons in M categories" line inside README's catalog block is generated too, but update the hand-written counts elsewhere in `README.md` (the `56` lesson total) and in `docs/learning-path.md` (`10 category cards`, per-category counts).
