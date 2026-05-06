package com.dantech.dreams.ui.feature.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Route : NavKey {

    @Serializable
    data object Landing : Route

    @Serializable
    data object Gallery : Route

    @Serializable
    data class LessonDetail(val lessonId: String) : Route

    @Serializable
    data class Showcase(val lessonId: String) : Route
}

fun routeForLessonId(id: String): Route =
    if (id.startsWith("showcase-")) Route.Showcase(id) else Route.LessonDetail(id)
