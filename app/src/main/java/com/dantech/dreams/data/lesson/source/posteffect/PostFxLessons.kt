package com.dantech.dreams.data.lesson.source.posteffect

import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonControl
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRegistry
import com.dantech.dreams.data.lesson.LessonRenderMode
import com.dantech.dreams.data.lesson.source.noise.NOISE_HELPERS
import com.dantech.dreams.ui.feature.common.SampleContent
import kotlinx.collections.immutable.persistentListOf

private fun postFxLesson(
    id: String,
    title: String,
    complexity: Int,
    intro: String,
    source: String,
    controls: kotlinx.collections.immutable.ImmutableList<LessonControl> = persistentListOf(),
    hint: String? = null,
) = LessonModel(
    id = id,
    title = title,
    category = LessonCategory.POSTFX,
    complexity = complexity,
    conceptIntro = intro,
    agslSource = source,
    controls = controls,
    renderMode = LessonRenderMode.RENDER_EFFECT,
    postEffectContent = { SampleContent() },
    screenRecordingHint = hint,
)

object Blur {
    val id = "postfx-01-blur"
    private val SOURCE = """
        uniform shader content;
        uniform float radius;
        half4 main(float2 fragCoord) {
            half4 sum = half4(0.0);
            for (int y = -1; y <= 1; y++) {
                for (int x = -1; x <= 1; x++) {
                    float2 o = float2(float(x), float(y)) * radius;
                    sum += content.eval(fragCoord + o);
                }
            }
            return sum / 9.0;
        }
    """.trimIndent()
    init {
        LessonRegistry.register(
            postFxLesson(
                id, "Box Blur", 2,
                "Average 9 samples around each pixel — the simplest convolution kernel. Increase radius for stronger blur.",
                SOURCE,
                persistentListOf(LessonControl.FloatRange("Radius", "radius", 0f, 12f, 4f)),
            )
        )
    }
}

object ChromaticAberration {
    val id = "postfx-02-chromatic-aberration"
    private val SOURCE = """
        uniform shader content;
        uniform float strength;
        half4 main(float2 fragCoord) {
            float2 dir = float2(strength, 0.0);
            half r = content.eval(fragCoord + dir).r;
            half g = content.eval(fragCoord).g;
            half b = content.eval(fragCoord - dir).b;
            return half4(r, g, b, 1.0);
        }
    """.trimIndent()
    init {
        LessonRegistry.register(
            postFxLesson(
                id, "Chromatic Aberration", 2,
                "Sample R/G/B at offset coordinates → cheap analog-camera fringe.",
                SOURCE,
                persistentListOf(LessonControl.FloatRange("Strength", "strength", 0f, 12f, 4f)),
            )
        )
    }
}

object RippleTap {
    val id = "postfx-03-ripple-tap"
    private val SOURCE = """
        uniform shader content;
        uniform float2 resolution;
        uniform float time;
        uniform float strength;
        half4 main(float2 fragCoord) {
            float2 c = resolution * 0.5;
            float d = distance(fragCoord, c);
            float wave = sin(d * 0.05 - time * 4.0) * exp(-d * 0.005) * strength;
            float2 dir = normalize(fragCoord - c + 0.001);
            return content.eval(fragCoord + dir * wave);
        }
    """.trimIndent()
    init {
        LessonRegistry.register(
            postFxLesson(
                id, "Ripple", 3,
                "Offset sample coords along radial direction by sin(dist - time) for a continuously-rippling pond.",
                SOURCE,
                persistentListOf(LessonControl.FloatRange("Strength", "strength", 0f, 30f, 12f)),
                hint = "Bump strength to 25 for the most dramatic recording.",
            )
        )
    }
}

object Dissolve {
    val id = "postfx-04-dissolve"
    private val SOURCE = """
        uniform shader content;
        uniform float2 resolution;
        uniform float time;
        $NOISE_HELPERS
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            float n = fbm(uv * 4.0);
            float threshold = 0.5 + 0.5 * sin(time * 0.6);
            float a = step(threshold, n);
            half4 c = content.eval(fragCoord);
            half edge = half(smoothstep(threshold - 0.05, threshold, n) - a);
            return half4(mix(c.rgb, half3(1.0, 0.5, 0.1), edge), c.a * half(a));
        }
    """.trimIndent()
    init {
        LessonRegistry.register(
            postFxLesson(
                id, "Dissolve", 4,
                "Use fbm noise as an animated alpha mask with a glowing edge — Thanos snap on a Composable.",
                SOURCE,
            )
        )
    }
}

object DisplacementGlass {
    val id = "postfx-05-displacement-glass"
    private val SOURCE = """
        uniform shader content;
        uniform float2 resolution;
        uniform float time;
        uniform float strength;
        $NOISE_HELPERS
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            float2 d = float2(fbm(uv * 3.0 + time * 0.10), fbm(uv * 3.0 + 9.7));
            d = (d - 0.5) * strength;
            return content.eval(fragCoord + d * resolution * 0.05);
        }
    """.trimIndent()
    init {
        LessonRegistry.register(
            postFxLesson(
                id, "Liquid Glass Displacement", 4,
                "Sample the input at a noise-displaced coord — produces the iOS 26-style refractive panel feel.",
                SOURCE,
                persistentListOf(LessonControl.FloatRange("Strength", "strength", 0f, 1f, 0.4f)),
                hint = "0.6 strength makes the displacement pop without losing legibility.",
            )
        )
    }
}

object Pixelate {
    val id = "postfx-06-pixelate"
    private val SOURCE = """
        uniform shader content;
        uniform float2 resolution;
        uniform float cellSize;
        half4 main(float2 fragCoord) {
            float2 q = floor(fragCoord / cellSize) * cellSize + cellSize * 0.5;
            return content.eval(q);
        }
    """.trimIndent()
    init {
        LessonRegistry.register(
            postFxLesson(
                id, "Pixelate", 2,
                "Snap fragCoord to a grid → mosaic. Increase cell size for chunky retro pixels.",
                SOURCE,
                persistentListOf(LessonControl.FloatRange("Cell Size", "cellSize", 2f, 64f, 16f)),
            )
        )
    }
}

object Glitch {
    val id = "postfx-07-glitch"
    private val SOURCE = """
        uniform shader content;
        uniform float2 resolution;
        uniform float time;
        uniform float amount;
        $NOISE_HELPERS
        // Quantise time into ~11 fps "glitch ticks", then let a hashed fraction
        // of horizontal rows jump sideways with a matching RGB split.
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            float qt = floor(time * 11.0);
            float row = floor(fragCoord.y / 18.0);
            float seed = hash21(float2(row, qt));
            float live = step(1.0 - amount * 0.42, seed);
            float shift = (hash21(float2(row, qt + 7.7)) - 0.5) * live * amount * 0.30;

            float2 c = fragCoord + float2(shift * resolution.x, 0.0);
            float split = amount * (1.5 + 30.0 * abs(shift));
            half r = content.eval(c + float2(split, 0.0)).r;
            half g = content.eval(c).g;
            half b = content.eval(c - float2(split, 0.0)).b;
            half3 col = half3(r, g, b);

            // Dimmer scanline shimmer on the shifted rows.
            col *= half(1.0 - live * 0.12 * (0.5 + 0.5 * sin(fragCoord.y * 3.14159)));
            // Slow rolling band sweeping top → bottom like a bad VHS head.
            col += half3(0.06 * amount) * half(exp(-pow((uv.y - fract(time * 0.13)) * 9.0, 2.0)));
            return half4(col, 1.0);
        }
    """.trimIndent()
    init {
        LessonRegistry.register(
            postFxLesson(
                id, "Glitch", 4,
                "Hash-driven row slices + chromatic split on a quantized clock — the composable looks like a corrupted signal.",
                SOURCE,
                persistentListOf(LessonControl.FloatRange("Amount", "amount", 0f, 1f, 0.55f)),
                hint = "0.75 amount with a fast time tick is maximum chaos.",
            )
        )
    }
}

object CrtScreen {
    val id = "postfx-08-crt-screen"
    private val SOURCE = """
        uniform shader content;
        uniform float2 resolution;
        uniform float time;
        uniform float curvature;
        uniform float scanlines;
        // CRT glass: barrel-warp the content, then lay scanlines and an
        // aperture-grille stripe mask over it — instant phosphor nostalgia.
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution * 2.0 - 1.0;
            float2 w = uv * (1.0 + curvature * 0.14 * dot(uv, uv));
            float2 c = (w * 0.5 + 0.5) * resolution;

            float inside = step(0.0, c.x) * step(c.x, resolution.x)
                         * step(0.0, c.y) * step(c.y, resolution.y);
            half4 px = content.eval(clamp(c, float2(0.0), resolution - 1.0));
            half3 col = px.rgb;

            // Scanlines: darkening ridges across the tube, depth via control.
            float sl = mix(1.0, 0.70 + 0.30 * (0.5 + 0.5 * sin(c.y * 2.094)), scanlines);
            col *= half(sl);
            // Aperture grille: RGB sub-pixels on a 3-px vertical lattice.
            float gp = mod(c.x, 3.0);
            half3 grille = half3(
                (gp < 1.0) ? 1.0 : 0.70,
                (gp >= 1.0 && gp < 2.0) ? 1.0 : 0.70,
                (gp >= 2.0) ? 1.0 : 0.70);
            col = mix(col, col * grille, half(0.55 * scanlines));
            // Phosphor vignette + 43 Hz flicker.
            float vig = smoothstep(2.0, 0.35, dot(uv, uv) * (1.0 + curvature * 0.4));
            col *= half(mix(0.45, 1.0, vig));
            col *= half(0.97 + 0.03 * sin(time * 43.0));

            half3 bezel = half3(0.012, 0.014, 0.018);
            return half4(mix(bezel, col, half(inside)), 1.0);
        }
    """.trimIndent()
    init {
        LessonRegistry.register(
            postFxLesson(
                id, "CRT Screen", 3,
                "Barrel warp + scanline ridges + a 3-px aperture grille turn any composable into a curved phosphor tube.",
                SOURCE,
                persistentListOf(
                    LessonControl.FloatRange("Curvature", "curvature", 0f, 1f, 0.45f),
                    LessonControl.FloatRange("Scanlines", "scanlines", 0f, 1f, 0.8f),
                ),
                hint = "Curvature 0.8, Scanlines 1.0 for full retro-TV.",
            )
        )
    }
}
