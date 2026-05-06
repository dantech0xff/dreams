package com.dantech.dreams.ui.feature.common

import android.graphics.RuntimeShader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.dantech.dreams.data.lesson.LessonControl
import com.dantech.dreams.data.lesson.LessonModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

// Compiled once per process. Previously these were rebuilt every time the
// preview composable re-keyed on agslSource — needless work.
private val TIME_UNIFORM_RX = Regex("""uniform\s+float\s+time\s*;""")
private val TOUCH_POS_RX = Regex("""uniform\s+float2\s+touchPos\s*;""")
private val TOUCH_TIME_RX = Regex("""uniform\s+float\s+touchTime\s*;""")

private fun floatUniformRx(name: String) =
    Regex("""uniform\s+float\s+$name\s*;""")

private fun colorUniformRx(name: String) =
    Regex("""(?:layout\s*\(\s*color\s*\)\s+)?uniform\s+half4\s+$name\s*;""")

/**
 * Pre-detected shader uniform shape for a lesson. Filtering controls down to
 * those *actually declared in the shader source* avoids `setFloatUniform`
 * calls that would throw at runtime under CheckJNI.
 */
@Immutable
data class ShaderBindings(
    val hasTime: Boolean,
    val hasTouch: Boolean,
    val floats: ImmutableList<LessonControl.FloatRange>,
    val colors: ImmutableList<LessonControl.ColorPicker>,
)

@Composable
fun rememberShaderBindings(lesson: LessonModel): ShaderBindings =
    remember(lesson.id, lesson.agslSource) {
        val src = lesson.agslSource
        ShaderBindings(
            hasTime = TIME_UNIFORM_RX.containsMatchIn(src),
            hasTouch = TOUCH_POS_RX.containsMatchIn(src) &&
                TOUCH_TIME_RX.containsMatchIn(src),
            floats = lesson.controls
                .filterIsInstance<LessonControl.FloatRange>()
                .filter { floatUniformRx(it.uniformName).containsMatchIn(src) }
                .toImmutableList(),
            colors = lesson.controls
                .filterIsInstance<LessonControl.ColorPicker>()
                .filter { colorUniformRx(it.uniformName).containsMatchIn(src) }
                .toImmutableList(),
        )
    }

/**
 * Writes all bindings the shader actually declares. Caller passes typed maps,
 * so no `Any` casting on the hot per-frame path.
 */
fun ShaderBindings.applyUniforms(
    shader: RuntimeShader,
    time: Float,
    touchPosX: Float,
    touchPosY: Float,
    touchTime: Float,
    floatValues: Map<String, Float>,
    colorValues: Map<String, Color>,
) {
    if (hasTime) shader.setFloatUniform("time", time)
    if (hasTouch) {
        shader.setFloatUniform("touchPos", touchPosX, touchPosY)
        shader.setFloatUniform("touchTime", touchTime)
    }
    floats.forEach { c ->
        shader.setFloatUniform(c.uniformName, floatValues[c.uniformName] ?: c.default)
    }
    colors.forEach { c ->
        shader.setColorUniform(
            c.uniformName,
            (colorValues[c.uniformName] ?: c.default).toArgb(),
        )
    }
}
