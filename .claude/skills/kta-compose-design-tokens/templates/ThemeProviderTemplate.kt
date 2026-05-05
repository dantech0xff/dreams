package com.example.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.example.theme.tokens.{Prefix}ColorTokens
import com.example.theme.tokens.Local{Prefix}Colors
import com.example.theme.tokens.Local{Prefix}Spacing
import com.example.theme.tokens.Local{Prefix}Shapes
import com.example.theme.tokens.Local{Prefix}Elevation
import com.example.theme.tokens.Local{Prefix}Typography
import com.example.theme.tokens.Local{Prefix}Motion
import com.example.theme.tokens.Local{Prefix}Opacity
import com.example.theme.tokens.{Prefix}SpacingTokens
import com.example.theme.tokens.{Prefix}ShapeTokens
import com.example.theme.tokens.{Prefix}ElevationTokens
import com.example.theme.tokens.{Prefix}TypographyTokens
import com.example.theme.tokens.{Prefix}MotionTokens
import com.example.theme.tokens.{Prefix}OpacityTokens
import com.example.theme.tokens.dark{Prefix}Colors
import com.example.theme.tokens.light{Prefix}Colors
import com.example.theme.tokens.default{Prefix}Spacing
import com.example.theme.tokens.default{Prefix}Shapes
import com.example.theme.tokens.default{Prefix}Elevation
import com.example.theme.tokens.default{Prefix}Typography
import com.example.theme.tokens.default{Prefix}Motion
import com.example.theme.tokens.default{Prefix}Opacity

// ---------------------------------------------------------------------------
// M3 bridge — maps semantic tokens to Material 3 ColorScheme
// ---------------------------------------------------------------------------
fun {Prefix}ColorTokens.toMaterial3ColorScheme(isDark: Boolean): ColorScheme =
    if (isDark) darkColorScheme(
        primary            = primary,
        onPrimary          = onPrimary,
        primaryContainer   = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary          = secondary,
        onSecondary        = onSecondary,
        tertiary           = tertiary,
        onTertiary         = onTertiary,
        background         = background,
        onBackground       = onBackground,
        surface            = surface,
        onSurface          = onSurface,
        surfaceVariant     = surfaceVariant,
        onSurfaceVariant   = onSurfaceVariant,
        outline            = outline,
        outlineVariant     = outlineVariant,
        scrim              = scrim,
        error              = error,
        onError            = onError,
    ) else lightColorScheme(
        primary            = primary,
        onPrimary          = onPrimary,
        primaryContainer   = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary          = secondary,
        onSecondary        = onSecondary,
        tertiary           = tertiary,
        onTertiary         = onTertiary,
        background         = background,
        onBackground       = onBackground,
        surface            = surface,
        onSurface          = onSurface,
        surfaceVariant     = surfaceVariant,
        onSurfaceVariant   = onSurfaceVariant,
        outline            = outline,
        outlineVariant     = outlineVariant,
        scrim              = scrim,
        error              = error,
        onError            = onError,
    )

// ---------------------------------------------------------------------------
// Theme composable — wraps MaterialTheme with all semantic locals
// ---------------------------------------------------------------------------
@Composable
fun {Prefix}Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) dark{Prefix}Colors else light{Prefix}Colors
    CompositionLocalProvider(
        Local{Prefix}Colors     provides colors,
        Local{Prefix}Spacing    provides default{Prefix}Spacing,
        Local{Prefix}Shapes     provides default{Prefix}Shapes,
        Local{Prefix}Elevation  provides default{Prefix}Elevation,
        Local{Prefix}Typography provides default{Prefix}Typography,
        Local{Prefix}Motion     provides default{Prefix}Motion,
        Local{Prefix}Opacity    provides default{Prefix}Opacity,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterial3ColorScheme(darkTheme),
            content = content,
        )
    }
}

// ---------------------------------------------------------------------------
// Theme object — @Composable getters, NO @ReadOnlyComposable
// (locals may be overridden in subtrees via CompositionLocalProvider)
// ---------------------------------------------------------------------------
object {Prefix}Theme {
    val colors: {Prefix}ColorTokens
        @Composable get() = Local{Prefix}Colors.current

    val spacing: {Prefix}SpacingTokens
        @Composable get() = Local{Prefix}Spacing.current

    val shapes: {Prefix}ShapeTokens
        @Composable get() = Local{Prefix}Shapes.current

    val elevation: {Prefix}ElevationTokens
        @Composable get() = Local{Prefix}Elevation.current

    val typography: {Prefix}TypographyTokens
        @Composable get() = Local{Prefix}Typography.current

    val motion: {Prefix}MotionTokens
        @Composable get() = Local{Prefix}Motion.current

    val opacity: {Prefix}OpacityTokens
        @Composable get() = Local{Prefix}Opacity.current
}
