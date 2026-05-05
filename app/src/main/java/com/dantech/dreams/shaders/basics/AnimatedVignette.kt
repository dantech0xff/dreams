package com.dantech.dreams.shaders.basics

import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRegistry

object AnimatedVignette {
    val id = "basics-06-vignette"

    private val SOURCE = """
        uniform float2 resolution;
        uniform float time;

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution - 0.5;
            uv.x *= resolution.x / resolution.y;
            float r = length(uv);
            float pulse = 0.35 + 0.10 * sin(time * 2.0);
            float v = 1.0 - smoothstep(pulse, pulse + 0.15, r);
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
                agslSource = SOURCE,
            )
        )
    }
}
