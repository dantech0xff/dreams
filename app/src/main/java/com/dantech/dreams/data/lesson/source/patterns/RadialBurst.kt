package com.dantech.dreams.data.lesson.source.patterns

import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonControl
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRegistry
import kotlinx.collections.immutable.persistentListOf

object RadialBurst {
    val id = "patterns-09-radial-burst"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float rays;
        uniform float twist;
        uniform float softness;
        half4 main(float2 fragCoord) {
            float2 uv = (fragCoord - 0.5 * resolution) / resolution.y;
            float radius = length(uv);
            float angle = atan(uv.y, uv.x) + radius * twist;
            float sector = fract(angle / 6.28318530718 * rays);
            float spoke = abs(sector - 0.5);
            float burst = smoothstep(softness, 0.5, spoke);
            float rings = 0.5 + 0.5 * sin(radius * 38.0);
            float fade = 1.0 - smoothstep(0.55, 0.92, radius);
            half3 bg = half3(0.06, 0.03, 0.11);
            half3 gold = half3(1.0, 0.72, 0.16);
            half3 violet = half3(0.38, 0.18, 0.82);
            half3 col = mix(violet, gold, half(burst));
            col = mix(bg, col, half(fade * (0.65 + rings * 0.35)));
            return half4(col, 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Radial Burst", category = LessonCategory.PATTERNS, complexity = 3,
                conceptIntro = "Repeat angular space instead of UV space. Each repeated sector becomes one ray.",
                learningNotes = persistentListOf(
                    "atan() gives angle; dividing by tau maps it into a repeatable 0..1 coordinate.",
                    "fract(angle * rays) creates the same wedge ramp around the full circle.",
                    "Adding radius * twist bends the rays while keeping them anchored at center.",
                ),
                agslSource = SOURCE,
                controls = persistentListOf(
                    LessonControl.FloatRange("Rays", "rays", 4f, 64f, 24f),
                    LessonControl.FloatRange("Twist", "twist", -10f, 10f, 2f),
                    LessonControl.FloatRange("Softness", "softness", 0.02f, 0.45f, 0.18f),
                ),
                screenRecordingHint = "Sweep rays 8 -> 48 for a poster-like burst reveal.",
            )
        )
    }
}
