package com.dantech.dreams.ui.feature.showcase

import androidx.compose.runtime.Immutable
import com.dantech.dreams.data.lesson.LessonModel

@Immutable
data class ShowcaseUiState(
    val lesson: LessonModel? = null,
)
