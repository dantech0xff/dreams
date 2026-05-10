package com.example.theme.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class {Prefix}ColorTokens(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val error: Color,
    val onError: Color,
    val info: Color,
    val onInfo: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val outline: Color,
    val outlineVariant: Color,
    val scrim: Color,
    val disabled: Color,
    val onDisabled: Color,
)

// ---------------------------------------------------------------------------
// Dark defaults — replace Color(...) values with your primitive palette
// ---------------------------------------------------------------------------
val dark{Prefix}Colors = {Prefix}ColorTokens(
    primary            = Color(0xFF9C27B0),
    onPrimary          = Color(0xFFFFFFFF),
    primaryContainer   = Color(0xFF6A0080),
    onPrimaryContainer = Color(0xFFEFB8FF),
    secondary          = Color(0xFF26A69A),
    onSecondary        = Color(0xFFFFFFFF),
    tertiary           = Color(0xFF42A5F5),
    onTertiary         = Color(0xFFFFFFFF),
    success            = Color(0xFF34D399),
    onSuccess          = Color(0xFF064E3B),
    warning            = Color(0xFFFBBF24),
    onWarning          = Color(0xFF78350F),
    error              = Color(0xFFF87171),
    onError            = Color(0xFF7F1D1D),
    info               = Color(0xFF60A5FA),
    onInfo             = Color(0xFF1E3A5F),
    background         = Color(0xFF121212),
    onBackground       = Color(0xFFE0E0E0),
    surface            = Color(0xFF1E1E1E),
    onSurface          = Color(0xFFE0E0E0),
    surfaceVariant     = Color(0xFF2C2C2C),
    onSurfaceVariant   = Color(0xFFBDBDBD),
    surfaceContainer   = Color(0xFF242424),
    surfaceContainerHigh = Color(0xFF2A2A2A),
    outline            = Color(0xFF616161),
    outlineVariant     = Color(0xFF424242),
    scrim              = Color(0xFF000000),
    disabled           = Color(0xFF3D3D3D),
    onDisabled         = Color(0xFF757575),
)

// ---------------------------------------------------------------------------
// Light defaults — replace Color(...) values with your primitive palette
// ---------------------------------------------------------------------------
val light{Prefix}Colors = {Prefix}ColorTokens(
    primary            = Color(0xFF7B1FA2),
    onPrimary          = Color(0xFFFFFFFF),
    primaryContainer   = Color(0xFFE1BEE7),
    onPrimaryContainer = Color(0xFF4A0072),
    secondary          = Color(0xFF00897B),
    onSecondary        = Color(0xFFFFFFFF),
    tertiary           = Color(0xFF1976D2),
    onTertiary         = Color(0xFFFFFFFF),
    success            = Color(0xFF059669),
    onSuccess          = Color(0xFFFFFFFF),
    warning            = Color(0xFFD97706),
    onWarning          = Color(0xFFFFFFFF),
    error              = Color(0xFFDC2626),
    onError            = Color(0xFFFFFFFF),
    info               = Color(0xFF2563EB),
    onInfo             = Color(0xFFFFFFFF),
    background         = Color(0xFFFAFAFA),
    onBackground       = Color(0xFF121212),
    surface            = Color(0xFFFFFFFF),
    onSurface          = Color(0xFF121212),
    surfaceVariant     = Color(0xFFF5F5F5),
    onSurfaceVariant   = Color(0xFF424242),
    surfaceContainer   = Color(0xFFEEEEEE),
    surfaceContainerHigh = Color(0xFFE0E0E0),
    outline            = Color(0xFF9E9E9E),
    outlineVariant     = Color(0xFFBDBDBD),
    scrim              = Color(0xFF000000),
    disabled           = Color(0xFFE0E0E0),
    onDisabled         = Color(0xFF9E9E9E),
)

// ---------------------------------------------------------------------------
// CompositionLocal
// ---------------------------------------------------------------------------
val Local{Prefix}Colors = staticCompositionLocalOf { dark{Prefix}Colors }
