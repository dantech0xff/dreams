package com.dantech.dreams.data.prefs

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf

@Immutable
data class UserPrefs(
    val lastLessonId: String? = null,
    val favorites: ImmutableSet<String> = persistentSetOf(),
    val paramOverrides: ImmutableMap<String, ImmutableMap<String, Float>> = persistentMapOf(),
    val reducedMotionOverride: Boolean = false,
    val useDynamicColor: Boolean = false,
) {
    companion object {
        val DEFAULT = UserPrefs()
    }
}
