package com.dantech.dreams.data.lesson.source.patterns

import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonControl
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRegistry
import kotlinx.collections.immutable.persistentListOf

object KaleidoscopeFold {
    val id = "patterns-06-kaleidoscope-fold"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float segments;
        uniform float twist;
        uniform float scale;
        half4 main(float2 fragCoord) {
            float2 uv = (fragCoord - 0.5 * resolution) / resolution.y;
            float radius = length(uv);
            float angle = atan(uv.y, uv.x) + radius * twist;
            float wedge = 6.28318530718 / max(segments, 2.0);
            float folded = abs(mod(angle + wedge * 0.5, wedge) - wedge * 0.5);
            float2 p = float2(cos(folded), sin(folded)) * radius * scale;
            float ribbons = 0.5 + 0.5 * sin((p.x + p.y) * 18.0);
            float cells = abs(fract(p.x * 3.0) - 0.5);
            float spokes = smoothstep(0.18, 0.48, cells);
            float petals = 0.5 + 0.5 * sin(12.0 * length(p) + 4.0 * sin(p.y * 3.0));
            float vignette = 1.0 - smoothstep(0.62, 0.95, radius);
            half3 bg = half3(0.06, 0.04, 0.14);
            half3 magenta = half3(0.90, 0.25, 0.65);
            half3 cyan = half3(0.25, 0.90, 0.95);
            half3 col = mix(bg, magenta, ribbons);
            col = mix(col, cyan, petals * spokes);
            return half4(col * vignette, 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Kaleidoscope Fold", category = LessonCategory.PATTERNS, complexity = 4,
                conceptIntro = "Fold polar angle into repeated wedges, then shade the folded space as one mirrored slice.",
                learningNotes = persistentListOf(
                    "atan() converts the pixel into polar angle, which can be repeated like any coordinate.",
                    "mod(angle, wedge) repeats the slice; abs() mirrors it around the slice center.",
                    "Twist adds radius back into angle so the symmetry bends outward from the center.",
                ),
                agslSource = SOURCE,
                controls = persistentListOf(
                    LessonControl.FloatRange("Segments", "segments", 3f, 14f, 8f),
                    LessonControl.FloatRange("Twist", "twist", -8f, 8f, 2.5f),
                    LessonControl.FloatRange("Scale", "scale", 2f, 8f, 4.2f),
                ),
                screenRecordingHint = "Sweep twist -4 -> 6 while holding segments near 8 for a mirrored bloom.",
            )
        )
    }
}
