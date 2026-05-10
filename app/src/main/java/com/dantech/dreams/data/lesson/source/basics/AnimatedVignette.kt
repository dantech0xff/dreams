package com.dantech.dreams.data.lesson.source.basics

import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonControl
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRegistry
import kotlinx.collections.immutable.persistentListOf

object AnimatedVignette {
    val id = "basics-06-vignette"

    private val SOURCE = """
        uniform float2 resolution;
        uniform float time;
        uniform float radius;
        uniform float softness;

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution - 0.5;
            uv.x *= resolution.x / resolution.y;
            float r = length(uv);
            float pulse = radius + 0.10 * sin(time * 2.0);
            float v = 1.0 - smoothstep(pulse, pulse + softness, r);
            half3 col = mix(half3(0.0), half3(0.95, 0.55, 0.30), v);
            return half4(col, 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id,
                title = "Animated Vignette",
                category = LessonCategory.BASICS,
                complexity = 2,
                conceptIntro = "smoothstep(edge0, edge1, x) gives a soft 0→1 transition. Pair with length() for an antialiased vignette.",
                learningNotes = persistentListOf(
                    "length(uv) turns centered coordinates into distance from the middle.",
                    "radius moves the first smoothstep edge, changing the size of the lit area.",
                    "softness controls the gap between smoothstep edges, widening or sharpening the fade.",
                ),
                agslSource = SOURCE,
                controls = persistentListOf(
                    LessonControl.FloatRange("Radius", "radius", 0.10f, 0.70f, 0.35f),
                    LessonControl.FloatRange("Softness", "softness", 0.02f, 0.40f, 0.15f),
                ),
            )
        )
    }
}
