package com.dantech.dreams.support

import com.dantech.dreams.data.prefs.ThemeMode
import com.dantech.dreams.data.prefs.UserPrefs
import com.dantech.dreams.data.prefs.UserPrefsRepository
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.collections.immutable.toPersistentSet
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
            val next = if (nowFavorite) it.favorites + id else it.favorites - id
            it.copy(favorites = next.toPersistentSet())
        }
        return nowFavorite
    }

    override suspend fun setParamOverride(lessonId: String, uniform: String, value: Float) {
        state.update { p ->
            val current = p.paramOverrides.toMutableMap()
            val inner = (current[lessonId].orEmpty()).toMutableMap().apply { put(uniform, value) }
            current[lessonId] = inner.toPersistentMap()
            p.copy(paramOverrides = current.toPersistentMap())
        }
    }

    override suspend fun setColorOverride(lessonId: String, uniform: String, argb: Int) {
        state.update { p ->
            val current = p.colorOverrides.toMutableMap()
            val inner = (current[lessonId].orEmpty()).toMutableMap().apply { put(uniform, argb) }
            current[lessonId] = inner.toPersistentMap()
            p.copy(colorOverrides = current.toPersistentMap())
        }
    }

    override suspend fun clearLessonOverrides(lessonId: String) {
        state.update {
            it.copy(
                paramOverrides = (it.paramOverrides - lessonId).toPersistentMap(),
                colorOverrides = (it.colorOverrides - lessonId).toPersistentMap(),
            )
        }
    }

    override suspend fun setReducedMotion(enabled: Boolean) {
        state.update { it.copy(reducedMotionOverride = enabled) }
    }

    override suspend fun setUseDynamicColor(enabled: Boolean) {
        state.update { it.copy(useDynamicColor = enabled) }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        state.update { it.copy(themeMode = mode) }
    }
}
