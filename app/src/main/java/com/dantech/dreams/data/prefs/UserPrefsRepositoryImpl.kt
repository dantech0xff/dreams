package com.dantech.dreams.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

internal class UserPrefsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : UserPrefsRepository {

    private object Keys {
        val LAST_LESSON = stringPreferencesKey("last_lesson_id")
        val FAVORITES = stringSetPreferencesKey("favorites")
        val OVERRIDES = stringPreferencesKey("param_overrides")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
    }

    override val prefsFlow: Flow<UserPrefs> = dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { p ->
            UserPrefs(
                lastLessonId = p[Keys.LAST_LESSON],
                favorites = p[Keys.FAVORITES] ?: emptySet(),
                paramOverrides = decodeOverrides(p[Keys.OVERRIDES] ?: ""),
                reducedMotionOverride = p[Keys.REDUCED_MOTION] ?: false,
            )
        }

    override suspend fun setLastLessonId(id: String) {
        dataStore.edit { it[Keys.LAST_LESSON] = id }
    }

    override suspend fun toggleFavorite(id: String): Boolean {
        var nowFavorite = false
        dataStore.edit { p ->
            val current = p[Keys.FAVORITES] ?: emptySet()
            nowFavorite = id !in current
            p[Keys.FAVORITES] = if (nowFavorite) current + id else current - id
        }
        return nowFavorite
    }

    override suspend fun setParamOverride(lessonId: String, uniform: String, value: Float) {
        dataStore.edit { p ->
            val current = decodeOverrides(p[Keys.OVERRIDES] ?: "").toMutableMap()
            val inner = current[lessonId].orEmpty().toMutableMap().apply { put(uniform, value) }
            current[lessonId] = inner
            p[Keys.OVERRIDES] = encodeOverrides(current)
        }
    }

    override suspend fun clearLessonOverrides(lessonId: String) {
        dataStore.edit { p ->
            val current = decodeOverrides(p[Keys.OVERRIDES] ?: "").toMutableMap()
            current.remove(lessonId)
            p[Keys.OVERRIDES] = if (current.isEmpty()) "" else encodeOverrides(current)
        }
    }

    override suspend fun setReducedMotion(enabled: Boolean) {
        dataStore.edit { it[Keys.REDUCED_MOTION] = enabled }
    }
}
