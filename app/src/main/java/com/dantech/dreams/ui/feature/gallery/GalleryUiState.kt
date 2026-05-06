package com.dantech.dreams.ui.feature.gallery

import androidx.compose.runtime.Immutable
import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList

@Immutable
data class GalleryUiState(
    val categories: ImmutableList<LessonCategory> = LessonCategory.entries.toImmutableList(),
    val selectedTabIndex: Int = 0,
    val lessons: ImmutableList<LessonModel> = persistentListOf(),
    val favorites: PersistentSet<String> = persistentSetOf(),
    val lastLessonId: String? = null,
)
