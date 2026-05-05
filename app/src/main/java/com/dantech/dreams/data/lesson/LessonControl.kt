package com.dantech.dreams.data.lesson

import androidx.compose.ui.graphics.Color

sealed interface LessonControl {
    val name: String
    val uniformName: String

    data class FloatRange(
        override val name: String,
        override val uniformName: String,
        val min: Float,
        val max: Float,
        val default: Float,
    ) : LessonControl

    data class ColorPicker(
        override val name: String,
        override val uniformName: String,
        val default: Color,
    ) : LessonControl
}
