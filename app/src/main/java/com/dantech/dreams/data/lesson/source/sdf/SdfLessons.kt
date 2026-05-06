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
