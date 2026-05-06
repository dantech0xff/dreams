package com.dantech.dreams.data.lesson.source.patterns

import androidx.compose.ui.graphics.Color
import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonControl
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRegistry
import com.dantech.dreams.data.lesson.source.noise.NOISE_HELPERS
import kotlinx.collections.immutable.persistentListOf

object DiagonalStripes {
    val id = "patterns-01-diagonal-stripes"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float count;
        uniform float skew;
        layout(color) uniform half4 inkA;
        layout(color) uniform half4 inkB;
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            float v = step(0.5, fract((uv.x + uv.y * skew) * count));
            return mix(inkA, inkB, half(v));
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Diagonal Stripes", category = LessonCategory.PATTERNS, complexity = 1,
                conceptIntro = "step(0.5, fract(x)) snaps continuous coords to a binary mask. Adding a sheared y term tilts the stripes.",
                agslSource = SOURCE,
                controls = persistentListOf(
                    LessonControl.FloatRange("Count", "count", 4f, 60f, 18f),
                    LessonControl.FloatRange("Skew", "skew", -1.5f, 1.5f, 0.6f),
                    LessonControl.ColorPicker("Ink A", "inkA", Color(0xFF0F172A)),
                    LessonControl.ColorPicker("Ink B", "inkB", Color(0xFFFACC15)),
                ),
            )
        )
    }
}

object PolkaDots {
    val id = "patterns-02-polka-dots"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float cells;
        uniform float radius;
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            uv.x *= resolution.x / resolution.y;
            float2 g = fract(uv * cells) - 0.5;
            float d = length(g) - radius;
            float a = 1.0 - smoothstep(0.0, 0.01, d);
            half3 bg = half3(0.95, 0.92, 0.88);
            half3 dot = half3(0.95, 0.30, 0.55);
            return half4(mix(bg, dot, a), 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Polka Dots", category = LessonCategory.PATTERNS, complexity = 2,
                conceptIntro = "Tile space with fract(uv * n), recenter to [-0.5, 0.5], drop an SDF circle in each cell.",
                agslSource = SOURCE,
                controls = persistentListOf(
                    LessonControl.FloatRange("Cells", "cells", 4f, 32f, 12f),
                    LessonControl.FloatRange("Radius", "radius", 0.05f, 0.45f, 0.22f),
                ),
            )
        )
    }
}

object HexGrid {
    val id = "patterns-03-hex-grid"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float scale;
        half4 main(float2 fragCoord) {
            float2 uv = (fragCoord - 0.5 * resolution) / resolution.y * scale;
            // Two interleaved rectangular lattices form a hex tiling.
            float2 a = float2(1.0, 1.732);
            float2 r1 = mod(uv, a) - a * 0.5;
            float2 r2 = mod(uv + a * 0.5, a) - a * 0.5;
            float2 c = (dot(r1, r1) < dot(r2, r2)) ? r1 : r2;
            // Distance to hex center, with edge AA.
            float d = length(c);
            float edge = smoothstep(0.42, 0.46, d);
            half3 fill = half3(0.05, 0.07, 0.16);
            half3 line = half3(0.95, 0.75, 0.20);
            return half4(mix(fill, line, half(edge)), 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Hex Grid", category = LessonCategory.PATTERNS, complexity = 3,
                conceptIntro = "Two offset rectangular lattices, take the closer center per pixel — gives a perfect hex tiling.",
                agslSource = SOURCE,
                controls = persistentListOf(LessonControl.FloatRange("Scale", "scale", 4f, 24f, 10f)),
            )
        )
    }
}

object TruchetTiles {
    val id = "patterns-04-truchet"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float cells;
        uniform float thickness;
        $NOISE_HELPERS
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution * cells;
            float2 i = floor(uv);
            float2 f = fract(uv);
            // Randomly mirror half the tiles → arcs join across cell borders.
            if (hash21(i) > 0.5) f.x = 1.0 - f.x;
            // Two quarter-arcs per tile centered at opposite corners.
            float d = min(
                abs(distance(f, float2(0.0, 0.0)) - 0.5),
                abs(distance(f, float2(1.0, 1.0)) - 0.5)
            );
            float a = 1.0 - smoothstep(thickness, thickness + 0.02, d);
            half3 bg = half3(0.05, 0.10, 0.15);
            half3 fg = half3(0.95, 0.65, 0.20);
            return half4(mix(bg, fg, half(a)), 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Truchet Tiles", category = LessonCategory.PATTERNS, complexity = 4,
                conceptIntro = "Per-cell hash decides arc orientation. Two quarter-circle SDFs in each tile yield a continuous maze-like weave.",
                agslSource = SOURCE,
                controls = persistentListOf(
                    LessonControl.FloatRange("Cells", "cells", 4f, 24f, 10f),
                    LessonControl.FloatRange("Thickness", "thickness", 0.04f, 0.30f, 0.12f),
                ),
                screenRecordingHint = "Sweep cells 6 → 18 for a satisfying density bloom.",
            )
        )
    }
}
