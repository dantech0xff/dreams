package com.dantech.dreams.ui.feature.showcase

import androidx.compose.runtime.Immutable
import com.dantech.dreams.data.lesson.LessonModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class ShowcaseListUiState(
    val showcases: ImmutableList<LessonModel> = persistentListOf(),
)
