package com.dantech.dreams.ui.feature.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Bottom-bar tab descriptor: visual identity only. Tabs are content slots inside the
 * Main shell, NOT navigation destinations — they do not appear on the outer back stack.
 */
enum class TabKey(
    val label: String,
    val icon: ImageVector,
) {
    LESSON("Lesson", Icons.AutoMirrored.Filled.List),
    SHOWCASE("Showcase", Icons.Filled.Star),
    SETTINGS("Settings", Icons.Filled.Settings),
}
