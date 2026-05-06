package com.dantech.dreams.ui.feature.lessonlist

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.prefs.UserPrefsRepository
import com.dantech.dreams.domain.lesson.LessonRepository
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Stable
class LessonListViewModel(
    private val repo: LessonRepository,
    private val prefs: UserPrefsRepository,
    private val categoryName: String,
) : ViewModel() {

    private val _ui = MutableStateFlow(buildInitial())
    val uiState: StateFlow<LessonListUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.prefsFlow.collect { snapshot ->
                _ui.update {
                    it.copy(
                        favorites = snapshot.favorites.toPersistentSet(),
                        lastLessonId = snapshot.lastLessonId,
                    )
                }
            }
        }
    }

    fun toggleFavorite(lessonId: String) {
        viewModelScope.launch { prefs.toggleFavorite(lessonId) }
    }

    private fun buildInitial(): LessonListUiState {
        val cat = runCatching { LessonCategory.valueOf(categoryName) }.getOrNull()
            ?: return LessonListUiState(error = "Unknown category: $categoryName", lessons = persistentListOf())
        return LessonListUiState(category = cat, lessons = repo.byCategory(cat))
    }
}
