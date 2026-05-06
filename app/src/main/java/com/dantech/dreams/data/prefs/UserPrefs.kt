package com.dantech.dreams.data.prefs

import androidx.compose.runtime.Immutable

@Immutable
data class UserPrefs(
    val lastLessonId: String? = null,
    val favorites: Set<String> = emptySet(),
    val paramOverrides: Map<String, Map<String, Float>> = emptyMap(),
    val reducedMotionOverride: Boolean = false,
) {
    companion object {
        val DEFAULT = UserPrefs()
    }
}
