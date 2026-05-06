package com.dantech.dreams.ui.feature.showcase

import androidx.lifecycle.ViewModel
import com.dantech.dreams.domain.lesson.LessonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ShowcaseViewModel(
    private val repo: LessonRepository,
    private val lessonId: String,
) : ViewModel() {

    private val _ui = MutableStateFlow(ShowcaseUiState(lesson = repo.byId(lessonId)))
    val uiState: StateFlow<ShowcaseUiState> = _ui.asStateFlow()

    fun toggleUi() {
        _ui.update { it.copy(hideUi = !it.hideUi) }
    }
}
