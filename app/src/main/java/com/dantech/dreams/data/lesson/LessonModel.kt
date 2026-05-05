package com.dantech.dreams.data.lesson

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

enum class LessonRenderMode { BRUSH, RENDER_EFFECT }

@Immutable
data class LessonModel(
    val id: String,
    val title: String,
    val category: LessonCategory,
    val complexity: Int,
    val conceptIntro: String,
    val agslSource: String,
    val controls: ImmutableList<LessonControl> = persistentListOf(),
    val renderMode: LessonRenderMode = LessonRenderMode.BRUSH,
    val screenRecordingHint: String? = null,
    val postEffectContent: (@Composable () -> Unit)? = null,
)
