package com.dantech.dreams.ui.theme

import androidx.compose.ui.graphics.Color

// Oscilloscope Workbench palette. Dark mode is an OLED instrument surface;
// light mode reads as blueprint paper without losing the technical accent set.

// Surface scale (dark)
val WorkbenchBg = Color(0xFF04100B)
val WorkbenchSurface = Color(0xFF0B1812)
val WorkbenchContainerLow = Color(0xFF102219)
val WorkbenchContainerHigh = Color(0xFF183428)
val WorkbenchContainerHighest = Color(0xFF244A39)

// Foreground (dark)
val WorkbenchInk = Color(0xFFE7FFF1)
val WorkbenchMute = Color(0xFF8FB8A1)
val WorkbenchLine = Color(0xFF2F5F49)

// Accents
val PhosphorGreen = Color(0xFF8CFF80)
val PhosphorGreenDark = Color(0xFF116329)
val PhosphorGreenContainer = Color(0xFFD9FFD2)
val PhosphorGreenContainerDark = Color(0xFF12381D)
val CrtCyan = Color(0xFF35F6FF)
val CrtCyanDark = Color(0xFF006B73)
val CrtCyanContainer = Color(0xFFB9FBFF)
val CrtCyanContainerDark = Color(0xFF063A41)
val CalibrationAmber = Color(0xFFFFC857)
val CalibrationAmberDark = Color(0xFF7A5200)
val CalibrationAmberContainer = Color(0xFFFFE39A)
val CalibrationAmberContainerDark = Color(0xFF4D3500)
val HotPixelMagenta = Color(0xFFFF4FD8)
val HotPixelMagentaDark = Color(0xFF9D007D)
val HotPixelMagentaContainer = Color(0xFFFFD7F4)
val HotPixelMagentaContainerDark = Color(0xFF5F004B)

// Status
val RunGreen = Color(0xFF37F67A)  // compiled OK
val CompileRed = Color(0xFFFF5A5F)  // shader error / destructive
val CompileRedContainer = Color(0xFFFFDAD6)
val CompileRedContainerDark = Color(0xFF5F1414)

// Surface scale (light)
val BlueprintBg = Color(0xFFF4F7EC)
val BlueprintSurface = Color(0xFFFFFFF6)
val BlueprintContainerLow = Color(0xFFE8F0DF)
val BlueprintContainerHigh = Color(0xFFDCE8D1)
val BlueprintContainerHighest = Color(0xFFCFDDC2)
val BlueprintInk = Color(0xFF102018)
val BlueprintMute = Color(0xFF4D6658)
val BlueprintLine = Color(0xFFB7C8B7)

// Per-category accents — small dot/stripe on cards, helps users
// scan a long list and gives each category a visual identity.
val AccentBasics = PhosphorGreen           // fundamentals
val AccentSdf = CrtCyan                    // geometry/math
val AccentNoise = Color(0xFF72F6B1)        // organic signal
val AccentPostFx = HotPixelMagenta         // expressive
val AccentShowcase = CalibrationAmber      // cinematic / hero
val AccentPatterns = Color(0xFFCFFF5E)     // rhythm and repetition
val AccentColor = HotPixelMagenta          // chromatic work
val AccentMotion = Color(0xFFB7FF6A)       // kinetic trace
val AccentFractals = Color(0xFF9AB6FF)     // recursive depth
val AccentLighting = Color(0xFFFFB000)     // illumination
val AccentInteractive = CrtCyan            // touch / responsive
