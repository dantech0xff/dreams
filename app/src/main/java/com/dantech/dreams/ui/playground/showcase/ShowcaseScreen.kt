package com.dantech.dreams.ui.playground.showcase

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dantech.dreams.data.lesson.LessonRegistry
import com.dantech.dreams.data.lesson.LessonRenderMode
import com.dantech.dreams.ui.playground.common.AgslBrushCanvas
import com.dantech.dreams.ui.playground.common.rememberShaderTime

@Composable
fun ShowcaseScreen(lessonId: String, onBack: () -> Unit) {
    val lesson = remember(lessonId) { LessonRegistry.byId(lessonId) }
    if (lesson == null) {
        Text("Showcase not found: $lessonId", Modifier.padding(24.dp))
        return
    }

    // CUSTOM-mode lessons own their gestures + uniforms + backdrop; render the
    // composable directly with NO root .clickable so taps flow into the lesson's
    // own pointerInput. Back button is a small TopStart overlay.
    if (lesson.renderMode == LessonRenderMode.CUSTOM) {
        Box(Modifier.fillMaxSize()) {
            lesson.customPreview?.invoke()
            Text(
                text = "← Back",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .clickable(onClick = onBack),
            )
        }
        return
    }

    var hideUi by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .clickable { hideUi = !hideUi },
    ) {
        val timeState = rememberShaderTime(lesson.agslSource)
        val timeDeclared = remember(lesson.agslSource) {
            Regex("""uniform\s+float\s+time\s*;""").containsMatchIn(lesson.agslSource)
        }
        AgslBrushCanvas(
            shaderSrc = lesson.agslSource,
            modifier = Modifier.fillMaxSize(),
            setUniforms = { shader ->
                if (timeDeclared) shader.setFloatUniform("time", timeState.value)
            },
        )
        if (!hideUi) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                RecordHintBanner(text = lesson.screenRecordingHint ?: "Tap to hide UI")
            }
            Box(
                Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.TopStart,
            ) {
                Text(
                    text = "← Back",
                    color = Color.White,
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable(onClick = onBack),
                )
            }
        }
    }
}
