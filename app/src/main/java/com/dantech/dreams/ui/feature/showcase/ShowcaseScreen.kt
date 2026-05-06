package com.dantech.dreams.ui.feature.showcase

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dantech.dreams.data.lesson.LessonRenderMode
import com.dantech.dreams.ui.feature.common.AgslBrushCanvas
import com.dantech.dreams.ui.feature.common.rememberShaderTime
import org.koin.androidx.compose.koinViewModel

@Composable
fun ShowcaseScreen(
    onBack: () -> Unit,
    vm: ShowcaseViewModel = koinViewModel(),
) {
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val lesson = ui.lesson
    if (lesson == null) {
        Text("Showcase not found", Modifier.padding(24.dp))
        return
    }

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

    Box(
        Modifier
            .fillMaxSize()
            .clickable { vm.toggleUi() },
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
        if (!ui.hideUi) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                RecordHintBanner(text = lesson.screenRecordingHint ?: "Tap to hide UI")
            }
            Box(
                Modifier.fillMaxSize(),
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
