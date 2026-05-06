package com.dantech.dreams.ui.feature.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Route : NavKey {

    /** The 3-tab shell — root entry of the outer NavDisplay backStack. */
    @Serializable
    data object Main : Route

    /** Fullscreen list of lessons in a category, pushed on top of [Main]. */
    @Serializable
    data class LessonList(val categoryName: String) : Route

    /** Fullscreen lesson detail, pushed on top. */
    @Serializable
    data class LessonDetail(val lessonId: String) : Route

    /** Fullscreen showcase shader, pushed on top. */
    @Serializable
    data class Showcase(val lessonId: String) : Route
}

fun routeForLessonId(id: String): Route =
    if (id.startsWith("showcase-")) Route.Showcase(id) else Route.LessonDetail(id)
