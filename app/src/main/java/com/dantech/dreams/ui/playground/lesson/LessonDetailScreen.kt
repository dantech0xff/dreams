package com.dantech.dreams.ui.playground.lesson

import android.graphics.RuntimeShader
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.dantech.dreams.data.lesson.LessonControl
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRegistry
import com.dantech.dreams.data.lesson.LessonRenderMode
import com.dantech.dreams.ui.playground.common.AgslBrushCanvas
import com.dantech.dreams.ui.playground.common.AgslRenderEffectCanvas
import com.dantech.dreams.ui.playground.common.rememberShaderTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonDetailScreen(lessonId: String, onBack: () -> Unit) {
    val lesson = remember(lessonId) { LessonRegistry.byId(lessonId) }
    if (lesson == null) {
        Text("Lesson not found: $lessonId", Modifier.padding(24.dp))
        return
    }

    val controlValues = rememberControlValues(lesson)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(lesson.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                LessonPreview(lesson = lesson, controlValues = controlValues)
            }

            Text(
                text = lesson.conceptIntro,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            if (lesson.controls.isNotEmpty()) {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    lesson.controls.forEach { control ->
                        when (control) {
                            is LessonControl.FloatRange -> {
                                ParameterSlider(
                                    control = control,
                                    value = controlValues[control.uniformName] as? Float ?: control.default,
                                    onValue = { controlValues[control.uniformName] = it },
                                )
                            }

                            is LessonControl.ColorPicker -> {
                                Text("${control.name} (color picker not yet implemented)")
                            }
                        }
                    }
                }
            }

            lesson.screenRecordingHint?.let {
                Text(
                    text = "Recording hint: $it",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            AgslSourceViewer(
                source = lesson.agslSource,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun rememberControlValues(lesson: LessonModel): SnapshotStateMap<String, Any> {
    return remember(lesson.id) {
        val m = mutableStateMapOf<String, Any>()
        lesson.controls.forEach { c ->
            when (c) {
                is LessonControl.FloatRange -> m[c.uniformName] = c.default
                is LessonControl.ColorPicker -> m[c.uniformName] = c.default
            }
        }
        m
    }
}

@Composable
private fun LessonPreview(
    lesson: LessonModel,
    controlValues: SnapshotStateMap<String, Any>,
) {
    val timeState = rememberShaderTime(lesson.agslSource)
    val timeDeclared = remember(lesson.agslSource) {
        Regex("""uniform\s+float\s+time\s*;""").containsMatchIn(lesson.agslSource)
    }
    // Pre-compute which control uniforms are actually declared in the shader
    // to avoid the CheckJNI-fatal write-undeclared-uniform path.
    val declaredFloats = remember(lesson.agslSource) {
        lesson.controls
            .filterIsInstance<LessonControl.FloatRange>()
            .filter {
                Regex("""uniform\s+float\s+${it.uniformName}\s*;""")
                    .containsMatchIn(lesson.agslSource)
            }
    }
    val declaredColors = remember(lesson.agslSource) {
        lesson.controls
            .filterIsInstance<LessonControl.ColorPicker>()
            .filter {
                Regex("""(?:layout\s*\(\s*color\s*\)\s+)?uniform\s+half4\s+${it.uniformName}\s*;""")
                    .containsMatchIn(lesson.agslSource)
            }
    }

    val touchDeclared = remember(lesson.agslSource) {
        Regex("""uniform\s+float2\s+touchPos\s*;""").containsMatchIn(lesson.agslSource) &&
            Regex("""uniform\s+float\s+touchTime\s*;""").containsMatchIn(lesson.agslSource)
    }
    val touchPosUv = remember(lesson.id) { mutableStateOf(Offset(-1f, -1f)) }
    val touchTime = remember(lesson.id) { mutableFloatStateOf(-1f) }

    val applyUniforms: (RuntimeShader) -> Unit = { shader ->
        if (timeDeclared) shader.setFloatUniform("time", timeState.value)
        if (touchDeclared) {
            shader.setFloatUniform("touchPos", touchPosUv.value.x, touchPosUv.value.y)
            shader.setFloatUniform("touchTime", touchTime.floatValue)
        }
        declaredFloats.forEach { c ->
            val v = controlValues[c.uniformName] as? Float ?: c.default
            shader.setFloatUniform(c.uniformName, v)
        }
        declaredColors.forEach { c ->
            val v = (controlValues[c.uniformName] as? androidx.compose.ui.graphics.Color) ?: c.default
            shader.setColorUniform(c.uniformName, v.toArgb())
        }
    }

    val canvasModifier = if (touchDeclared) {
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
        LessonRenderMode.BRUSH -> {
            AgslBrushCanvas(
                shaderSrc = lesson.agslSource,
                modifier = canvasModifier,
                setUniforms = applyUniforms,
            )
        }

        LessonRenderMode.RENDER_EFFECT -> {
            AgslRenderEffectCanvas(
                shaderSrc = lesson.agslSource,
                modifier = canvasModifier,
                setUniforms = applyUniforms,
            ) {
                lesson.postEffectContent?.invoke()
            }
        }
    }
}

