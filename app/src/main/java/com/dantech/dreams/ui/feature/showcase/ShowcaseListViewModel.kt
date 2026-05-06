package com.dantech.dreams.ui.feature.showcase

import androidx.lifecycle.ViewModel
import com.dantech.dreams.domain.lesson.LessonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ShowcaseListViewModel(
    repo: LessonRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(ShowcaseListUiState(showcases = repo.showcases()))
    val uiState: StateFlow<ShowcaseListUiState> = _ui.asStateFlow()
}
