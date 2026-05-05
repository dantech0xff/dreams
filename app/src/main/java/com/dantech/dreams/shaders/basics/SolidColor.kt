package com.dantech.dreams.shaders.basics

import androidx.compose.ui.graphics.Color
import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonControl
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRegistry
import kotlinx.collections.immutable.persistentListOf

object SolidColor {
    val id = "basics-01-solid"

    private val SOURCE = """
        uniform float2 resolution;
        layout(color) uniform half4 baseColor;

        half4 main(float2 fragCoord) {
            return baseColor;
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id,
                title = "Solid Color",
                category = LessonCategory.BASICS,
                complexity = 1,
                conceptIntro = "The simplest shader: every pixel returns the same uniform color. Introduces the half4/uniform basics.",
                agslSource = SOURCE,
                controls = persistentListOf(
                    LessonControl.ColorPicker(
                        name = "Base color",
                        uniformName = "baseColor",
                        default = Color(0xFFE91E63),
                    ),
                ),
            )
        )
    }
}
