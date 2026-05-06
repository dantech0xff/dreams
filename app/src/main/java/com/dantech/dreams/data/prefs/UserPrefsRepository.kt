package com.dantech.dreams.data.prefs

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.Flow

@Stable
interface UserPrefsRepository {
    val prefsFlow: Flow<UserPrefs>
    suspend fun setLastLessonId(id: String)
    suspend fun toggleFavorite(id: String): Boolean
    suspend fun setParamOverride(lessonId: String, uniform: String, value: Float)
    suspend fun clearLessonOverrides(lessonId: String)
    suspend fun setReducedMotion(enabled: Boolean)
    suspend fun setUseDynamicColor(enabled: Boolean)
}
