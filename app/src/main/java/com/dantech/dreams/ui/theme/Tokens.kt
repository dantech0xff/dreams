package com.dantech.dreams.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Semantic aliases for color/spacing values that don't have an M3 colorScheme slot.
// Add a token here ONLY if it's used in 2+ places.
object Tokens {
    // 80% white, used as foreground on shader backdrops where colorScheme.onSurface
    // would clash with a dynamic gradient.
    val translucentLightOnDark = Color(0xCCFFFFFF)

    val spaceCompact = 8.dp
    val spaceComfortable = 16.dp
}
