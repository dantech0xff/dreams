package com.dantech.dreams.ui.feature.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Bottom-bar tab descriptor: maps a tab to its root [Route], display label, and icon.
 * Each [TabKey.root] is the entry that lives at depth 0 of that tab's per-tab back stack.
 */
enum class TabKey(
    val root: Route,
    val label: String,
    val icon: ImageVector,
) {
    LESSON(Route.LessonRoot, "Lesson", Icons.AutoMirrored.Filled.List),
    SHOWCASE(Route.ShowcaseRoot, "Showcase", Icons.Filled.Star),
    SETTINGS(Route.SettingsRoot, "Settings", Icons.Filled.Settings),
    ;

    companion object {
        fun forRoot(root: Route): TabKey? = entries.firstOrNull { it.root == root }
    }
}
