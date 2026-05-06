package com.dantech.dreams.ui.feature.lesson

import androidx.compose.runtime.Immutable
import com.dantech.dreams.data.lesson.LessonModel
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

@Immutable
data class LessonDetailUiState(
    val lesson: LessonModel? = null,
    val paramOverrides: PersistentMap<String, Float> = persistentMapOf(),
)
