package com.dantech.dreams.ui.feature.showcase

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.dantech.dreams.domain.lesson.LessonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Stable
class ShowcaseViewModel(
    repo: LessonRepository,
    lessonId: String,
) : ViewModel() {

    private val _ui = MutableStateFlow(ShowcaseUiState(lesson = repo.byId(lessonId)))
    val uiState: StateFlow<ShowcaseUiState> = _ui.asStateFlow()
}
