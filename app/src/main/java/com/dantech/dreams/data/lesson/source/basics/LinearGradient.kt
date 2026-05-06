package com.dantech.dreams.data.lesson.source.basics

import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRegistry

object LinearGradient {
    val id = "basics-03-linear-gradient"

    private val SOURCE = """
        uniform float2 resolution;

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            half3 a = half3(0.05, 0.05, 0.20);
            half3 b = half3(0.95, 0.55, 0.30);
            return half4(mix(a, b, uv.x), 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id,
                title = "Linear Gradient",
                category = LessonCategory.BASICS,
                complexity = 2,
                conceptIntro = "uv = fragCoord / resolution gives normalized 0..1 coordinates. mix(a, b, uv.x) interpolates left→right.",
                agslSource = SOURCE,
            )
        )
    }
}
