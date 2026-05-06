package com.dantech.dreams.ui.theme

import androidx.compose.ui.graphics.Color

// Brand palette — "graphics engineer's playground". Dark-first, OLED-friendly,
// near-black surfaces with a violet/cyan accent that echoes typical shader output.
// Light scheme is a derived inverse for daytime use; dark is the canonical brand.

// Surface scale (dark)
val Midnight       = Color(0xFF0B0B1A)  // background — deepest
val MidnightLow    = Color(0xFF12122A)  // surface
val MidnightMid    = Color(0xFF17172E)  // surfaceContainerLow
val MidnightHigh   = Color(0xFF1E1C35)  // surfaceContainerHigh — cards
val MidnightHigher = Color(0xFF272447)  // surfaceContainerHighest

// Foreground (dark)
val EngineerInk    = Color(0xFFE8EAF2)  // onSurface — 91% white tint
val EngineerMute   = Color(0xFF94A3B8)  // onSurfaceVariant — slate-400
val EngineerLine   = Color(0xFF2E2B4A)  // outline / outlineVariant

// Accents
val NeonViolet     = Color(0xFF7C3AED)  // primary
val NeonViolet80   = Color(0xFFA78BFA)  // primary on dark
val NeonViolet20   = Color(0xFF2A1A55)  // primaryContainer on dark
val SignalCyan     = Color(0xFF22D3EE)  // secondary — "running"/active uniform
val SignalCyan80   = Color(0xFF67E8F9)
val SignalCyan20   = Color(0xFF154A52)
val FluxRose       = Color(0xFFF43F5E)  // tertiary — "wow" / favorite

// Status
val RunGreen       = Color(0xFF22C55E)  // compiled OK
val CompileRed     = Color(0xFFEF4444)  // shader error / destructive

// Light-scheme surfaces (inverted, neutral cool)
val DayBg          = Color(0xFFFAFAFC)
val DaySurface     = Color(0xFFFFFFFF)
val DayContainerLo = Color(0xFFF1F2F7)
val DayContainerHi = Color(0xFFE7E8F0)
val DayInk         = Color(0xFF101129)
val DayMute        = Color(0xFF5B6072)
val DayLine        = Color(0xFFD8DAE5)

// Per-category accents — small dot/stripe on cards, helps users
// scan a long list and gives each category a visual identity.
val AccentBasics   = SignalCyan          // understanding the basics
val AccentSdf      = NeonViolet          // geometry/math
val AccentNoise    = Color(0xFF14B8A6)   // teal — organic
val AccentPostFx   = FluxRose            // expressive
val AccentShowcase = Color(0xFFF59E0B)   // amber — cinematic / hero
