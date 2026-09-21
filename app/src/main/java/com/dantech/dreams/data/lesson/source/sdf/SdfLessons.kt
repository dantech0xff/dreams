package com.dantech.dreams.data.lesson.source.sdf

import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonControl
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRegistry
import kotlinx.collections.immutable.persistentListOf

private fun centeredUv() = """
    float2 uv = fragCoord / resolution - 0.5;
    uv.x *= resolution.x / resolution.y;
"""

object CircleSdf {
    val id = "sdf-01-circle"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float radius;
        $SDF_HELPERS
        half4 main(float2 fragCoord) {
            ${centeredUv()}
            float d = sdCircle(uv, radius);
            float a = 1.0 - smoothstep(0.0, 0.005, d);
            half3 col = mix(half3(0.05), half3(0.95, 0.45, 0.30), a);
            return half4(col, 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Circle SDF", category = LessonCategory.SDF, complexity = 2,
                conceptIntro = "A signed distance field is a function returning distance to a shape's edge. length(p) - r is the canonical circle.",
                agslSource = SOURCE,
                controls = persistentListOf(LessonControl.FloatRange("Radius", "radius", 0.05f, 0.5f, 0.3f)),
            )
        )
    }
}

object RoundedBoxSdf {
    val id = "sdf-02-rounded-box"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float corner;
        $SDF_HELPERS
        half4 main(float2 fragCoord) {
            ${centeredUv()}
            float d = sdBox(uv, float2(0.30, 0.18), corner);
            float a = 1.0 - smoothstep(0.0, 0.005, d);
            half3 col = mix(half3(0.05), half3(0.30, 0.85, 0.95), a);
            return half4(col, 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Rounded Box", category = LessonCategory.SDF, complexity = 2,
                conceptIntro = "Box SDF with corner radius: subtract `r` from the distance to round the edges.",
                agslSource = SOURCE,
                controls = persistentListOf(LessonControl.FloatRange("Corner", "corner", 0f, 0.18f, 0.06f)),
            )
        )
    }
}

object Metaballs {
    val id = "sdf-03-metaballs"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float time;
        uniform float smoothness;
        $SDF_HELPERS
        half4 main(float2 fragCoord) {
            ${centeredUv()}
            float d1 = sdCircle(uv - float2(sin(time)*0.25, cos(time)*0.25), 0.18);
            float d2 = sdCircle(uv - float2(cos(time*1.3)*0.20, sin(time*0.7)*0.20), 0.20);
            float d3 = sdCircle(uv - float2(sin(time*0.6)*0.30, sin(time*1.1)*0.10), 0.15);
            float d = opSmoothUnion(opSmoothUnion(d1, d2, smoothness), d3, smoothness);
            float a = 1.0 - smoothstep(0.0, 0.01, d);
            half3 col = mix(half3(0.05, 0.05, 0.10), half3(0.95, 0.30, 0.65), a);
            return half4(col, 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Smooth Metaballs", category = LessonCategory.SDF, complexity = 3,
                conceptIntro = "opSmoothUnion(d1,d2,k) blends two SDFs with a smoothness factor — yielding the classic goo/metaball look.",
                agslSource = SOURCE,
                controls = persistentListOf(LessonControl.FloatRange("Smoothness", "smoothness", 0.02f, 0.40f, 0.18f)),
                screenRecordingHint = "Try smoothness 0.05 → 0.5 for a goo merge effect.",
            )
        )
    }
}

object Checkerboard {
    val id = "sdf-04-checkerboard"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float cells;
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            float2 g = floor(uv * cells);
            float c = mod(g.x + g.y, 2.0);
            half3 col = mix(half3(0.10), half3(0.95, 0.85, 0.60), half(c));
            return half4(col, 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Checkerboard", category = LessonCategory.SDF, complexity = 2,
                conceptIntro = "floor() + mod() turn continuous space into discrete cells — the simplest periodic pattern.",
                agslSource = SOURCE,
                controls = persistentListOf(LessonControl.FloatRange("Cells", "cells", 2f, 32f, 8f)),
            )
        )
    }
}

object BreathingGrid {
    val id = "sdf-05-breathing-grid"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float time;
        uniform float cells;
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            float2 g = fract(uv * cells) - 0.5;
            float r = 0.18 + 0.10 * sin(time * 2.0);
            float d = length(g) - r;
            float a = 1.0 - smoothstep(0.0, 0.02, d);
            half3 col = mix(half3(0.04, 0.04, 0.08), half3(0.95, 0.45, 0.85), a);
            return half4(col, 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Breathing Grid", category = LessonCategory.SDF, complexity = 3,
                conceptIntro = "Tile space with fract(uv * n), then place an animated SDF in each cell.",
                agslSource = SOURCE,
                controls = persistentListOf(LessonControl.FloatRange("Cells", "cells", 4f, 24f, 10f)),
            )
        )
    }
}

object Isolines {
    val id = "sdf-06-isolines"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float density;
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution - 0.5;
            uv.x *= resolution.x / resolution.y;
            float r = length(uv);
            float bands = abs(fract(r * density) - 0.5);
            float a = smoothstep(0.45, 0.5, bands);
            half3 col = mix(half3(0.10, 0.05, 0.20), half3(0.30, 0.95, 0.85), a);
            return half4(col, 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Isolines", category = LessonCategory.SDF, complexity = 2,
                conceptIntro = "fract(distance * n) produces concentric rings — a contour-line visualization.",
                agslSource = SOURCE,
                controls = persistentListOf(LessonControl.FloatRange("Density", "density", 4f, 40f, 16f)),
            )
        )
    }
}

object Heartbeat {
    val id = "sdf-07-heartbeat"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float time;
        uniform float bpm;
        uniform float glow;
        // iq's analytic heart: two lobe circles plus a V-shaped bottom edge.
        float sdHeart(float2 p) {
            p.x = abs(p.x);
            if (p.y + p.x > 1.0) {
                return sqrt(dot(p - float2(0.25, 0.75), p - float2(0.25, 0.75))) - 0.3536;
            }
            return sqrt(min(dot(p - float2(0.0, 1.0), p - float2(0.0, 1.0)),
                            dot(p - 0.5 * max(p.x + p.y, 0.0), p - 0.5 * max(p.x + p.y, 0.0)))) * sign(p.x - p.y);
        }
        half4 main(float2 fragCoord) {
            float2 uv = (fragCoord - 0.5 * resolution) / resolution.y;
            // Lub-dub envelope: strong beat at ~12% of the cycle, softer echo at ~42%.
            float bt = fract(time * bpm / 60.0);
            float pulse = exp(-pow((bt - 0.12) * 7.0, 2.0)) + 0.55 * exp(-pow((bt - 0.42) * 7.0, 2.0));

            // sdHeart space is y-up with the tip at the origin; flip y so the
            // tip points down-screen, scale about the tip so each beat pushes
            // the lobes outward.
            float2 hp = float2(uv.x, -uv.y) * (2.0 - pulse * 0.20) + float2(0.0, 0.65);
            float d = sdHeart(hp);
            float aa = 2.0 / resolution.y * 2.15;
            float fillAmt = smoothstep(aa, -aa, d);

            half3 col = mix(half3(0.050, 0.012, 0.040), half3(0.016, 0.006, 0.024), half(smoothstep(0.2, 1.4, length(uv))));
            half3 heart = mix(half3(0.72, 0.04, 0.18), half3(1.0, 0.34, 0.48), half(clamp(hp.y * 0.8 + 0.15, 0.0, 1.0)));
            // Fake top-left sheen inside the silhouette.
            heart += half3(0.22, 0.10, 0.14) * half(smoothstep(0.6, -0.4, hp.x + hp.y));
            col = mix(col, heart, half(fillAmt));

            // Rim glow hugging the outside edge, swelling on each beat.
            float rimAmt = exp(-max(d, 0.0) * 9.0) * (0.20 + 0.80 * pulse) * glow;
            col += half3(1.0, 0.25, 0.35) * half(rimAmt) * (1.0 - half(fillAmt));

            // An expanding pressure ring rides out on every lub.
            float r = length(uv);
            float ring = exp(-abs(r - bt * 2.4) * 26.0) * exp(-bt * 3.2) * 0.35 * glow;
            col += half3(1.0, 0.30, 0.45) * half(ring);
            return half4(col, 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Heartbeat", category = LessonCategory.SDF, complexity = 4,
                conceptIntro = "An analytic heart SDF driven by a two-bump lub-dub envelope — scale, rim glow and a pressure ring all read the same pulse.",
                agslSource = SOURCE,
                controls = persistentListOf(
                    LessonControl.FloatRange("BPM", "bpm", 40f, 160f, 76f),
                    LessonControl.FloatRange("Glow", "glow", 0f, 2f, 1f),
                ),
                screenRecordingHint = "Push BPM to 140 and Glow to 1.6 — the ring reads as a shockwave.",
            )
        )
    }
}
