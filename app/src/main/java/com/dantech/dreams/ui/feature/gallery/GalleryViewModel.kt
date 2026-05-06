package com.dantech.dreams.ui.feature.gallery

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.prefs.UserPrefsRepository
import com.dantech.dreams.domain.lesson.LessonRepository
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GalleryViewModel(
    private val repo: LessonRepository,
    private val prefs: UserPrefsRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val tabKey = "selectedTabIndex"

    private val _ui = MutableStateFlow(buildInitial())
    val uiState: StateFlow<GalleryUiState> = _ui.asStateFlow()

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

    fun selectTab(index: Int) {
        val cats = _ui.value.categories
        if (index !in cats.indices || index == _ui.value.selectedTabIndex) return
        savedState[tabKey] = index
        _ui.update { it.copy(selectedTabIndex = index, lessons = repo.byCategory(cats[index])) }
    }

    fun toggleFavorite(lessonId: String) {
        viewModelScope.launch { prefs.toggleFavorite(lessonId) }
    }

    private fun buildInitial(): GalleryUiState {
        val cats = LessonCategory.entries.toImmutableList()
        val saved = savedState.get<Int>(tabKey) ?: 0
        val idx = saved.coerceIn(cats.indices)
        return GalleryUiState(
            categories = cats,
            selectedTabIndex = idx,
            lessons = repo.byCategory(cats[idx]),
        )
    }
}
