package com.dantech.dreams.data.prefs

import kotlinx.coroutines.flow.Flow

interface UserPrefsRepository {
    val prefsFlow: Flow<UserPrefs>
    suspend fun setLastLessonId(id: String)
    suspend fun toggleFavorite(id: String): Boolean
    suspend fun setParamOverride(lessonId: String, uniform: String, value: Float)
    suspend fun clearLessonOverrides(lessonId: String)
    suspend fun setReducedMotion(enabled: Boolean)
}
