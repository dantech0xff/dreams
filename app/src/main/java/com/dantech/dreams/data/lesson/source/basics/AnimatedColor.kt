package com.dantech.dreams.data.lesson.source.basics

import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonControl
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRegistry
import kotlinx.collections.immutable.persistentListOf

object AnimatedColor {
    val id = "basics-02-animated-color"

    private val SOURCE = """
        uniform float2 resolution;
        uniform float time;
        uniform float speed;

        half4 main(float2 fragCoord) {
            float t = 0.5 + 0.5 * sin(time * speed);
            half3 a = half3(0.10, 0.40, 0.90);
            half3 b = half3(0.95, 0.35, 0.20);
            return half4(mix(a, b, t), 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id,
                title = "Animated Color",
                category = LessonCategory.BASICS,
                complexity = 1,
                conceptIntro = "Drive the output color from a time uniform written every frame via withFrameNanos. The uniform `time` is a float in seconds.",
                agslSource = SOURCE,
                controls = persistentListOf(
                    LessonControl.FloatRange("Speed", "speed", 0.1f, 4f, 1f),
                ),
            )
        )
    }
}
