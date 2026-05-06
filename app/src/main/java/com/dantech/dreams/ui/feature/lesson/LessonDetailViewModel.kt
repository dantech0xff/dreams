package com.dantech.dreams.ui.feature.lesson

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dantech.dreams.data.prefs.UserPrefsRepository
import com.dantech.dreams.domain.lesson.LessonRepository
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Stable
class LessonDetailViewModel(
    private val repo: LessonRepository,
    private val prefs: UserPrefsRepository,
    private val lessonId: String,
) : ViewModel() {

    private val _ui = MutableStateFlow(LessonDetailUiState(lesson = repo.byId(lessonId)))
    val uiState: StateFlow<LessonDetailUiState> = _ui.asStateFlow()

    // 200ms debounce: every slider event lands here, only the latest within the
    // window is persisted. Keeps DataStore commits sane during drag.
    private val pendingPersist = MutableSharedFlow<Pair<String, Float>>(extraBufferCapacity = 64)

    @OptIn(FlowPreview::class)
    private val persistJob = viewModelScope.launch {
        pendingPersist
            .debounce(PERSIST_DEBOUNCE_MS)
            .collect { (uniform, value) ->
                prefs.setParamOverride(lessonId, uniform, value)
            }
    }

    init {
        viewModelScope.launch {
            val saved = prefs.prefsFlow.first().paramOverrides[lessonId] ?: emptyMap()
            if (saved.isNotEmpty()) {
                _ui.update { it.copy(paramOverrides = saved.toPersistentMap()) }
            }
            prefs.setLastLessonId(lessonId)
        }
    }

    fun setFloat(uniform: String, value: Float) {
        _ui.update { it.copy(paramOverrides = it.paramOverrides.put(uniform, value)) }
        pendingPersist.tryEmit(uniform to value)
    }

    fun resetOverrides() {
        _ui.update { it.copy(paramOverrides = persistentMapOf()) }
        viewModelScope.launch { prefs.clearLessonOverrides(lessonId) }
    }

    private companion object {
        const val PERSIST_DEBOUNCE_MS = 200L
    }
}
