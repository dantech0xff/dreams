package com.dantech.dreams.ui.feature.lesson

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
    private val pendingPersist = MutableSharedFlow<PendingFloatPersist>(extraBufferCapacity = 64)
    private var persistGeneration = 0L

    @OptIn(FlowPreview::class)
    private val persistJob = viewModelScope.launch {
        pendingPersist
            .debounce(PERSIST_DEBOUNCE_MS)
            .collect { pending ->
                if (pending.generation == persistGeneration) {
                    prefs.setParamOverride(lessonId, pending.uniform, pending.value)
                }
            }
    }

    init {
        viewModelScope.launch {
            val snapshot = prefs.prefsFlow.first()
            val saved = snapshot.paramOverrides[lessonId] ?: emptyMap()
            val savedColors = snapshot.colorOverrides[lessonId] ?: emptyMap()
            if (saved.isNotEmpty() || savedColors.isNotEmpty()) {
                _ui.update {
                    it.copy(
                        paramOverrides = saved.toPersistentMap(),
                        colorOverrides = savedColors.toPersistentMap(),
                    )
                }
            }
            prefs.setLastLessonId(lessonId)
        }
    }

    fun setFloat(uniform: String, value: Float) {
        _ui.update { it.copy(paramOverrides = it.paramOverrides.put(uniform, value)) }
        pendingPersist.tryEmit(PendingFloatPersist(uniform, value, persistGeneration))
    }

    fun setColor(uniform: String, color: Color) {
        val argb = color.toArgb()
        _ui.update { it.copy(colorOverrides = it.colorOverrides.put(uniform, argb)) }
        viewModelScope.launch { prefs.setColorOverride(lessonId, uniform, argb) }
    }

    fun resetOverrides() {
        persistGeneration += 1
        _ui.update { it.copy(paramOverrides = persistentMapOf(), colorOverrides = persistentMapOf()) }
        viewModelScope.launch { prefs.clearLessonOverrides(lessonId) }
    }

    private data class PendingFloatPersist(
        val uniform: String,
        val value: Float,
        val generation: Long,
    )

    private companion object {
        const val PERSIST_DEBOUNCE_MS = 200L
    }
}
