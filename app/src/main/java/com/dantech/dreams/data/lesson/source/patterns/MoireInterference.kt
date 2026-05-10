package com.dantech.dreams.data.lesson.source.patterns

import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonControl
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRegistry
import kotlinx.collections.immutable.persistentListOf

object MoireInterference {
    val id = "patterns-05-moire-interference"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float density;
        uniform float angle;
        uniform float contrast;
        half4 main(float2 fragCoord) {
            float2 uv = (fragCoord - 0.5 * resolution) / resolution.y;
            float theta = angle * 0.0174532925;
            float2 dirA = float2(1.0, 0.0);
            float2 dirB = float2(cos(theta), sin(theta));
            float stripeA = 0.5 + 0.5 * sin(dot(uv, dirA) * density);
            float stripeB = 0.5 + 0.5 * sin(dot(uv, dirB) * density);
            float beat = abs(stripeA - stripeB);
            float width = 0.25 / max(contrast, 0.1);
            float mask = smoothstep(0.5 - width, 0.5 + width, beat);
            float glow = smoothstep(0.72, 0.95, beat) * 0.35;
            half3 base = half3(0.05, 0.08, 0.16);
            half3 ink = half3(0.98, 0.78, 0.20);
            half3 accent = half3(0.18, 0.82, 0.95);
            half3 col = mix(base, ink, mask);
            col = mix(col, accent, glow);
            return half4(col, 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Moire Interference", category = LessonCategory.PATTERNS, complexity = 3,
                conceptIntro = "Overlay two nearly aligned stripe waves. Tiny angular differences create large beat patterns.",
                learningNotes = persistentListOf(
                    "Each stripe field is just sin(dot(uv, direction) * density).",
                    "Taking abs(stripeA - stripeB) reveals where the two waves disagree.",
                    "Contrast narrows the smoothstep band, making the interference sharper.",
                ),
                agslSource = SOURCE,
                controls = persistentListOf(
                    LessonControl.FloatRange("Density", "density", 12f, 120f, 54f),
                    LessonControl.FloatRange("Angle", "angle", -45f, 45f, 12f),
                    LessonControl.FloatRange("Contrast", "contrast", 0.5f, 5f, 2f),
                ),
                screenRecordingHint = "Sweep angle 2 -> 24 to show broad bands collapsing into tight ripples.",
            )
        )
    }
}
