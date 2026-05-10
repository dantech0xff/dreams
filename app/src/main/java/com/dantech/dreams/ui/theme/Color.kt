package com.dantech.dreams.ui.theme

import androidx.compose.ui.graphics.Color

// Shader Lab palette. Graphite dark mode, crisp ink light mode, and spectral
// accents tuned for AGSL demos without letting chrome compete with shader output.

// Surface scale (dark)
val GraphiteBg = Color(0xFF090A0F)
val GraphiteSurface = Color(0xFF11131A)
val GraphiteContainerLow = Color(0xFF171A23)
val GraphiteContainerHigh = Color(0xFF202432)
val GraphiteContainerHighest = Color(0xFF2A3040)

// Foreground (dark)
val ShaderInk = Color(0xFFF1F5F9)
val ShaderMute = Color(0xFFA8B3C7)
val ShaderLine = Color(0xFF394254)

// Accents
val SignalCyan = Color(0xFF22D3EE)
val SignalCyanDark = Color(0xFF006C7A)
val SignalCyanContainer = Color(0xFFC6F7FF)
val SignalCyanContainerDark = Color(0xFF004F5C)
val FluxRose = Color(0xFFEC4899)
val FluxRoseDark = Color(0xFFB51F73)
val FluxRoseContainer = Color(0xFFFFD8EC)
val FluxRoseContainerDark = Color(0xFF641447)
val PhotonAmber = Color(0xFFF59E0B)
val PhotonAmberDark = Color(0xFF8A5200)
val PhotonAmberContainer = Color(0xFFFFE3B3)
val PhotonAmberContainerDark = Color(0xFF5C3900)

// Status
val RunGreen = Color(0xFF22C55E)  // compiled OK
val CompileRed = Color(0xFFEF4444)  // shader error / destructive
val CompileRedContainer = Color(0xFFFFDAD6)
val CompileRedContainerDark = Color(0xFF5F1414)

// Light-scheme surfaces
val InkBg = Color(0xFFF7F9FC)
val PaperSurface = Color(0xFFFFFFFF)
val InkContainerLow = Color(0xFFEEF3F8)
val InkContainerHigh = Color(0xFFE4EBF3)
val InkContainerHighest = Color(0xFFD8E2ED)
val DayInk = Color(0xFF121826)
val DayMute = Color(0xFF526173)
val DayLine = Color(0xFFC7D2E0)

// Per-category accents — small dot/stripe on cards, helps users
// scan a long list and gives each category a visual identity.
val AccentBasics = SignalCyan              // understanding the basics
val AccentSdf = Color(0xFF8B5CF6)          // geometry/math
val AccentNoise = Color(0xFF14B8A6)        // teal — organic
val AccentPostFx = FluxRose                // expressive
val AccentShowcase = PhotonAmber           // amber — cinematic / hero
val AccentPatterns = Color(0xFFEAB308)     // gold — rhythm and repetition
val AccentColor = Color(0xFFEC4899)        // hot pink — chromatic
val AccentMotion = Color(0xFF10B981)       // emerald — kinetic
val AccentFractals = Color(0xFF6366F1)     // indigo — recursive depth
val AccentLighting = Color(0xFFF97316)     // tangerine — illumination
val AccentInteractive = Color(0xFF0EA5E9)  // sky — touch / responsive
