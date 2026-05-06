package com.dantech.dreams.ui.feature.lessonlist

import androidx.lifecycle.ViewModel
import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.domain.lesson.LessonRepository
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LessonCategoriesViewModel(
    private val repo: LessonRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(buildInitial())
    val uiState: StateFlow<LessonCategoriesUiState> = _ui.asStateFlow()

    private fun buildInitial(): LessonCategoriesUiState {
        val items = LessonCategory.lessonOnly().map { cat ->
            LessonCategoryItem(category = cat, count = repo.byCategory(cat).size)
        }
        return LessonCategoriesUiState(categories = items.toImmutableList())
    }
}
