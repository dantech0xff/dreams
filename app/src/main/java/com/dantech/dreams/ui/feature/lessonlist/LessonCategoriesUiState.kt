package com.dantech.dreams.ui.feature.lessonlist

import androidx.compose.runtime.Immutable
import com.dantech.dreams.data.lesson.LessonCategory
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class LessonCategoryItem(
    val category: LessonCategory,
    val count: Int,
)

@Immutable
data class LessonCategoriesUiState(
    val categories: ImmutableList<LessonCategoryItem> = persistentListOf(),
)
