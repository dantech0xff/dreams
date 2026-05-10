package com.dantech.dreams.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet
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
        val COLOR_OVERRIDES = stringPreferencesKey("color_overrides")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    override val prefsFlow: Flow<UserPrefs> = dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { p ->
            UserPrefs(
                lastLessonId = p[Keys.LAST_LESSON],
                favorites = (p[Keys.FAVORITES] ?: emptySet()).toImmutableSet(),
                paramOverrides = decodeOverrides(p[Keys.OVERRIDES] ?: "").map {
                    it.key to it.value.toImmutableMap()
                }.toMap().toImmutableMap(),
                colorOverrides = decodeColorOverrides(p[Keys.COLOR_OVERRIDES] ?: "").map {
                    it.key to it.value.toImmutableMap()
                }.toMap().toImmutableMap(),
                reducedMotionOverride = p[Keys.REDUCED_MOTION] ?: false,
                useDynamicColor = p[Keys.USE_DYNAMIC_COLOR] ?: false,
                themeMode = ThemeMode.fromStorageValue(p[Keys.THEME_MODE]),
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

    override suspend fun setColorOverride(lessonId: String, uniform: String, argb: Int) {
        dataStore.edit { p ->
            val current = decodeColorOverrides(p[Keys.COLOR_OVERRIDES] ?: "").toMutableMap()
            val inner = current[lessonId].orEmpty().toMutableMap().apply { put(uniform, argb) }
            current[lessonId] = inner
            p[Keys.COLOR_OVERRIDES] = encodeColorOverrides(current)
        }
    }

    override suspend fun clearLessonOverrides(lessonId: String) {
        dataStore.edit { p ->
            val floatOverrides = decodeOverrides(p[Keys.OVERRIDES] ?: "").toMutableMap()
            val colorOverrides = decodeColorOverrides(p[Keys.COLOR_OVERRIDES] ?: "").toMutableMap()
            floatOverrides.remove(lessonId)
            colorOverrides.remove(lessonId)
            p[Keys.OVERRIDES] = encodeOverrides(floatOverrides)
            p[Keys.COLOR_OVERRIDES] = encodeColorOverrides(colorOverrides)
        }
    }

    override suspend fun setReducedMotion(enabled: Boolean) {
        dataStore.edit { it[Keys.REDUCED_MOTION] = enabled }
    }

    override suspend fun setUseDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.USE_DYNAMIC_COLOR] = enabled }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.storageValue }
    }
}
