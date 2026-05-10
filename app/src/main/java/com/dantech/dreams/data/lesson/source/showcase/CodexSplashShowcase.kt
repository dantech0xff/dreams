package com.dantech.dreams.data.lesson.source.showcase

import android.graphics.RuntimeShader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.unit.dp
import com.dantech.dreams.core.agsl.rememberShaderTime
import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRegistry
import com.dantech.dreams.data.lesson.LessonRenderMode
import com.dantech.dreams.ui.feature.common.AgslErrorCard
import kotlinx.collections.immutable.persistentListOf

@Composable
fun CodexSplashDemo() {
    val time = rememberShaderTime()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth < 360.dp || maxHeight < 560.dp
        val iconSize = if (compact) 164.dp else 210.dp

        AgslShaderLayer(
            shaderSrc = CODEX_SPLASH_BACKGROUND_SRC,
            time = time,
            modifier = Modifier.fillMaxSize(),
        )

        AgslShaderLayer(
            shaderSrc = CODEX_SPLASH_ICON_SRC,
            time = time,
            modifier = Modifier
                .align(Alignment.Center)
                .size(iconSize),
        )
    }
}

@Composable
private fun AgslShaderLayer(
    shaderSrc: String,
    time: State<Float>,
    modifier: Modifier = Modifier,
) {
    val (shader, error) = remember(shaderSrc) {
        try {
            RuntimeShader(shaderSrc) to null
        } catch (t: Throwable) {
            null to (t.message ?: "compile error")
        }
    }
    if (shader == null || error != null) {
        AgslErrorCard(message = error ?: "unknown", modifier = modifier)
        return
    }
    val brush = remember(shader) { ShaderBrush(shader) }
    Box(
        modifier = modifier.drawBehind {
            shader.setFloatUniform("resolution", size.width, size.height)
            shader.setFloatUniform("time", time.value)
            drawRect(brush = brush)
        },
    )
}

object CodexSplashShowcase {
    val id = "showcase-06-codex-splash"

    init {
        LessonRegistry.register(
            LessonModel(
                id = id,
                title = "Codex Splash",
                category = LessonCategory.SHOWCASE,
                complexity = 4,
                conceptIntro = "A procedural Codex-style splash screen: layered AGSL atmosphere, SDF icon tile, cloud glyph, and terminal marks. Compose only hosts and positions the shader layers.",
                agslSource = CODEX_SPLASH_BACKGROUND_SRC.trimIndent(),
                extraAgslSources = persistentListOf(CODEX_SPLASH_ICON_SRC.trimIndent()),
                renderMode = LessonRenderMode.CUSTOM,
                customPreview = { CodexSplashDemo() },
                screenRecordingHint = "Hold on the logo as the prismatic glass edge sweeps across the tile and glyph.",
            )
        )
    }
}
