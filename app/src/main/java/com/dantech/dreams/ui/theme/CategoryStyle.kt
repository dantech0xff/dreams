package com.dantech.dreams.ui.theme

import androidx.compose.ui.graphics.Color
import com.dantech.dreams.data.lesson.LessonCategory

// Visual identity per lesson category. Keeps the data-layer enum free of UI
// concerns — UI consumers reach for `category.accent` via this extension.
val LessonCategory.accent: Color
    get() = when (this) {
        LessonCategory.BASICS -> AccentBasics
        LessonCategory.PATTERNS -> AccentPatterns
        LessonCategory.COLOR -> AccentColor
        LessonCategory.SDF -> AccentSdf
        LessonCategory.NOISE -> AccentNoise
        LessonCategory.MOTION -> AccentMotion
        LessonCategory.FRACTALS -> AccentFractals
        LessonCategory.LIGHTING -> AccentLighting
        LessonCategory.INTERACTIVE -> AccentInteractive
        LessonCategory.POSTFX -> AccentPostFx
        LessonCategory.SHOWCASE -> AccentShowcase
    }
