package com.dantech.dreams.data.lesson.source.patterns

import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonControl
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRegistry
import kotlinx.collections.immutable.persistentListOf

object PlaidWeave {
    val id = "patterns-07-plaid-weave"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float density;
        uniform float threadWidth;
        uniform float contrast;
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            uv.x *= resolution.x / resolution.y;
            float2 cell = fract(uv * density);
            float vertical = 1.0 - smoothstep(threadWidth, threadWidth + 0.03, abs(cell.x - 0.5));
            float horizontal = 1.0 - smoothstep(threadWidth, threadWidth + 0.03, abs(cell.y - 0.5));
            float weave = clamp(vertical + horizontal * contrast, 0.0, 1.0);
            float overUnder = step(0.5, fract(floor(uv.x * density) + floor(uv.y * density) * 0.5));
            half3 base = half3(0.07, 0.09, 0.14);
            half3 warp = half3(0.85, 0.18, 0.34);
            half3 weft = half3(0.16, 0.72, 0.95);
            half3 thread = mix(warp, weft, half(overUnder));
            return half4(mix(base, thread, half(weave)), 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Plaid Weave", category = LessonCategory.PATTERNS, complexity = 2,
                conceptIntro = "Cross two repeated stripe masks. Alternating cell parity sells the over-under weave.",
                learningNotes = persistentListOf(
                    "The vertical and horizontal thread masks use the same centered cell coordinate.",
                    "floor() gives stable tile indices that can flip color from cell to cell.",
                    "Adding masks creates plaid, while clamp() keeps the blend in display range.",
                ),
                agslSource = SOURCE,
                controls = persistentListOf(
                    LessonControl.FloatRange("Density", "density", 4f, 32f, 12f),
                    LessonControl.FloatRange("Thread Width", "threadWidth", 0.05f, 0.45f, 0.16f),
                    LessonControl.FloatRange("Contrast", "contrast", 0.5f, 2f, 1f),
                ),
            )
        )
    }
}
