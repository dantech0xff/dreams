package com.dantech.dreams.shaders.basics

import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonControl
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRegistry
import kotlinx.collections.immutable.persistentListOf

object PolarCoords {
    val id = "basics-05-polar-coords"

    private val SOURCE = """
        uniform float2 resolution;
        uniform float time;
        uniform float speed;

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution - 0.5;
            uv.x *= resolution.x / resolution.y;
            float r = length(uv);
            float a = atan(uv.y, uv.x);
            float swirl = a + r * 8.0 - time * speed;
            float v = 0.5 + 0.5 * sin(swirl * 4.0);
            half3 col = mix(half3(0.10, 0.05, 0.30), half3(1.0, 0.45, 0.85), v);
            return half4(col, 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id,
                title = "Polar Coordinates",
                category = LessonCategory.BASICS,
                complexity = 3,
                conceptIntro = "atan(p.y, p.x) gives the angle from origin; combine with length() for swirling polar effects.",
                agslSource = SOURCE,
                controls = persistentListOf(
                    LessonControl.FloatRange("Speed", "speed", 0f, 4f, 1.5f),
                ),
                screenRecordingHint = "Sweep the speed slider 0 → 4 for a hypnotic swirl.",
            )
        )
    }
}
