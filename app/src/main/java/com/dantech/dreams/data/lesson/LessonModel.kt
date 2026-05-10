package com.dantech.dreams.data.lesson

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

enum class LessonRenderMode { BRUSH, RENDER_EFFECT, CUSTOM }

@Immutable
data class LessonModel(
    val id: String,
    val title: String,
    val category: LessonCategory,
    val complexity: Int,
    val conceptIntro: String,
    val learningNotes: ImmutableList<String> = persistentListOf(),
    val agslSource: String,
    val controls: ImmutableList<LessonControl> = persistentListOf(),
    val renderMode: LessonRenderMode = LessonRenderMode.BRUSH,
    val screenRecordingHint: String? = null,
    val postEffectContent: (@Composable () -> Unit)? = null,
    // Used when renderMode == CUSTOM. The lesson's preview area is replaced wholesale
    // with this composable — bypasses the auto-uniform pipeline and pointer wiring,
    // so the composable owns its shader, time, gestures, and backdrop.
    val customPreview: (@Composable () -> Unit)? = null,
)
