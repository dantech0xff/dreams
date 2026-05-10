package com.dantech.dreams.data.lesson.source.basics

import androidx.compose.ui.graphics.Color
import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonControl
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRegistry
import kotlinx.collections.immutable.persistentListOf

object LinearGradient {
    val id = "basics-03-linear-gradient"

    private val SOURCE = """
        uniform float2 resolution;
        layout(color) uniform half4 startColor;
        layout(color) uniform half4 endColor;
        uniform float angle;

        half4 main(float2 fragCoord) {
            float2 centered = fragCoord / resolution - 0.5;
            float radians = angle * 0.01745329252;
            float2 direction = float2(cos(radians), sin(radians));
            float span = abs(direction.x) + abs(direction.y);
            float t = dot(centered, direction) / span + 0.5;
            half3 color = mix(startColor.rgb, endColor.rgb, clamp(t, 0.0, 1.0));
            return half4(color, 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id,
                title = "Linear Gradient",
                category = LessonCategory.BASICS,
                complexity = 2,
                conceptIntro = "Normalize fragCoord, project it onto a direction vector, then mix two color uniforms along that axis.",
                learningNotes = persistentListOf(
                    "fragCoord is the current pixel position in device pixels.",
                    "startColor and endColor are uniforms, so the gradient palette can change live.",
                    "angle rotates the direction vector; dot(centered, direction) becomes the blend amount.",
                ),
                agslSource = SOURCE,
                controls = persistentListOf(
                    LessonControl.ColorPicker("Start color", "startColor", Color(0xFF0D0D33)),
                    LessonControl.ColorPicker("End color", "endColor", Color(0xFFF38C4D)),
                    LessonControl.FloatRange("Direction", "angle", 0f, 360f, 0f),
                ),
            )
        )
    }
}
