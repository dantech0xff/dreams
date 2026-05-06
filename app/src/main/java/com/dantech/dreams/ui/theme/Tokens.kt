package com.dantech.dreams.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

// Semantic aliases that don't have an M3 colorScheme slot or a typography slot.
// Add an entry here ONLY if it's used in 2+ places.
object Tokens {
    // 80% white, used as foreground on shader backdrops where colorScheme.onSurface
    // would clash with a dynamic gradient.
    val translucentLightOnDark = Color(0xCCFFFFFF)

    // Spacing scale (4dp grid). Use these instead of magic numbers.
    val spaceXs = 4.dp
    val spaceSm = 8.dp
    val spaceMd = 12.dp
    val spaceLg = 16.dp
    val spaceXl = 24.dp
    val space2xl = 32.dp

    // Legacy aliases — kept so existing call sites compile. Prefer spaceSm/Lg above.
    val spaceCompact = spaceSm
    val spaceComfortable = spaceLg

    // Mono family for parameter readouts, AGSL source, and numeric counts.
    // Backed by the system's monospace face (Roboto Mono on Android).
    val mono: FontFamily = FontFamily.Monospace
}
