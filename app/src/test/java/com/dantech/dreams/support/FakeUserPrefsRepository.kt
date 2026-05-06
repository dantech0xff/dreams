package com.dantech.dreams.support

import com.dantech.dreams.data.prefs.UserPrefs
import com.dantech.dreams.data.prefs.UserPrefsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeUserPrefsRepository(
    initial: UserPrefs = UserPrefs.DEFAULT,
) : UserPrefsRepository {

    private val state = MutableStateFlow(initial)
    override val prefsFlow: Flow<UserPrefs> = state.asStateFlow()

    override suspend fun setLastLessonId(id: String) {
        state.update { it.copy(lastLessonId = id) }
    }

    override suspend fun toggleFavorite(id: String): Boolean {
        var nowFavorite = false
        state.update {
            nowFavorite = id !in it.favorites
            it.copy(favorites = if (nowFavorite) it.favorites + id else it.favorites - id)
        }
        return nowFavorite
    }

    override suspend fun setParamOverride(lessonId: String, uniform: String, value: Float) {
        state.update { p ->
            val current = p.paramOverrides.toMutableMap()
            val inner = (current[lessonId].orEmpty()).toMutableMap().apply { put(uniform, value) }
            current[lessonId] = inner
            p.copy(paramOverrides = current)
        }
    }

    override suspend fun clearLessonOverrides(lessonId: String) {
        state.update { it.copy(paramOverrides = it.paramOverrides - lessonId) }
    }

    override suspend fun setReducedMotion(enabled: Boolean) {
        state.update { it.copy(reducedMotionOverride = enabled) }
    }

    override suspend fun setUseDynamicColor(enabled: Boolean) {
        state.update { it.copy(useDynamicColor = enabled) }
    }
}
