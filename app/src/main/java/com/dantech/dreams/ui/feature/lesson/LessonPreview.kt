package com.dantech.dreams.ui.feature.lesson

import android.graphics.RuntimeShader
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRenderMode
import com.dantech.dreams.ui.feature.common.AgslBrushCanvas
import com.dantech.dreams.ui.feature.common.AgslRenderEffectCanvas
import com.dantech.dreams.ui.feature.common.ShaderBindings
import com.dantech.dreams.ui.feature.common.applyUniforms
import com.dantech.dreams.ui.feature.common.rememberShaderTime

@Composable
fun LessonPreview(
    lesson: LessonModel,
    bindings: ShaderBindings,
    floatValues: SnapshotStateMap<String, Float>,
    colorValues: SnapshotStateMap<String, Color>,
) {
    val timeState = rememberShaderTime(lesson.agslSource)
    val touchPosUv = remember(lesson.id) { mutableStateOf(Offset(-1f, -1f)) }
    val touchTime = remember(lesson.id) { mutableFloatStateOf(-1f) }

    val applyUniforms: (RuntimeShader) -> Unit = { shader ->
        bindings.applyUniforms(
            shader = shader,
            time = timeState.value,
            touchPosX = touchPosUv.value.x,
            touchPosY = touchPosUv.value.y,
            touchTime = touchTime.floatValue,
            floatValues = floatValues,
            colorValues = colorValues,
        )
    }

    val canvasModifier = if (bindings.hasTouch) {
        Modifier
            .fillMaxSize()
            .pointerInput(lesson.id) {
                detectTapGestures(
                    onPress = { offset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        if (w > 0f && h > 0f) {
                            touchPosUv.value = Offset(offset.x / w, offset.y / h)
                            touchTime.floatValue = timeState.value
                        }
                    },
                )
            }
    } else {
        Modifier.fillMaxSize()
    }

    when (lesson.renderMode) {
        LessonRenderMode.BRUSH -> AgslBrushCanvas(
            shaderSrc = lesson.agslSource,
            modifier = canvasModifier,
            setUniforms = applyUniforms,
        )

        LessonRenderMode.RENDER_EFFECT -> AgslRenderEffectCanvas(
            shaderSrc = lesson.agslSource,
            modifier = canvasModifier,
            setUniforms = applyUniforms,
        ) {
            lesson.postEffectContent?.invoke()
        }

        LessonRenderMode.CUSTOM -> lesson.customPreview?.invoke()
    }
}
