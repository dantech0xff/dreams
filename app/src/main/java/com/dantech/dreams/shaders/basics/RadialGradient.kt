package com.dantech.dreams.shaders.basics

import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonControl
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRegistry
import kotlinx.collections.immutable.persistentListOf

object RadialGradient {
    val id = "basics-04-radial-gradient"

    private val SOURCE = """
        uniform float2 resolution;
        uniform float radius;

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution - 0.5;
            uv.x *= resolution.x / resolution.y;
            float d = length(uv);
            float t = clamp(d / radius, 0.0, 1.0);
            half3 inner = half3(1.0, 0.85, 0.30);
            half3 outer = half3(0.10, 0.05, 0.25);
            return half4(mix(inner, outer, t), 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id,
                title = "Radial Gradient",
                category = LessonCategory.BASICS,
                complexity = 2,
                conceptIntro = "length(uv - 0.5) gives distance from screen center. Aspect-correct by multiplying x by width/height.",
                agslSource = SOURCE,
                controls = persistentListOf(
                    LessonControl.FloatRange("Radius", "radius", 0.05f, 1.2f, 0.6f),
                ),
            )
        )
    }
}
