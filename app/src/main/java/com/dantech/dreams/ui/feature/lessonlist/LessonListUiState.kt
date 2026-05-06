package com.dantech.dreams.ui.feature.lessonlist

import androidx.compose.runtime.Immutable
import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Immutable
data class LessonListUiState(
    val category: LessonCategory? = null,
    val lessons: ImmutableList<LessonModel> = persistentListOf(),
    val favorites: PersistentSet<String> = persistentSetOf(),
    val lastLessonId: String? = null,
    val error: String? = null,
)
