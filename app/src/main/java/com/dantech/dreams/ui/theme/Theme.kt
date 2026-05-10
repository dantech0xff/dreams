package com.dantech.dreams.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dantech.dreams.data.prefs.ThemeMode
import com.dantech.dreams.data.prefs.UserPrefs
import com.dantech.dreams.data.prefs.UserPrefsRepository
import org.koin.compose.koinInject

// dynamicColor (Material You wallpaper-tinted) is opt-in via SettingsScreen so
// the Oscilloscope Workbench identity stays the default.

private val DarkBrandScheme = darkColorScheme(
    primary = PhosphorGreen,
    onPrimary = WorkbenchBg,
    primaryContainer = PhosphorGreenContainerDark,
    onPrimaryContainer = PhosphorGreenContainer,
    secondary = CrtCyan,
    onSecondary = WorkbenchBg,
    secondaryContainer = CrtCyanContainerDark,
    onSecondaryContainer = CrtCyanContainer,
    tertiary = CalibrationAmber,
    onTertiary = WorkbenchBg,
    tertiaryContainer = CalibrationAmberContainerDark,
    onTertiaryContainer = CalibrationAmberContainer,
    background = WorkbenchBg,
    onBackground = WorkbenchInk,
    surface = WorkbenchSurface,
    onSurface = WorkbenchInk,
    surfaceVariant = WorkbenchContainerLow,
    onSurfaceVariant = WorkbenchMute,
    surfaceContainer = WorkbenchContainerLow,
    surfaceContainerLow = WorkbenchSurface,
    surfaceContainerLowest = WorkbenchBg,
    surfaceContainerHigh = WorkbenchContainerHigh,
    surfaceContainerHighest = WorkbenchContainerHighest,
    outline = WorkbenchLine,
    outlineVariant = WorkbenchLine,
    error = CompileRed,
    onError = WorkbenchBg,
    errorContainer = CompileRedContainerDark,
    onErrorContainer = CompileRedContainer,
)

private val LightBrandScheme = lightColorScheme(
    primary = PhosphorGreenDark,
    onPrimary = BlueprintSurface,
    primaryContainer = PhosphorGreenContainer,
    onPrimaryContainer = Color(0xFF053712),
    secondary = CrtCyanDark,
    onSecondary = BlueprintSurface,
    secondaryContainer = CrtCyanContainer,
    onSecondaryContainer = Color(0xFF003A3F),
    tertiary = CalibrationAmberDark,
    onTertiary = BlueprintSurface,
    tertiaryContainer = CalibrationAmberContainer,
    onTertiaryContainer = Color(0xFF3B2700),
    background = BlueprintBg,
    onBackground = BlueprintInk,
    surface = BlueprintSurface,
    onSurface = BlueprintInk,
    surfaceVariant = BlueprintContainerLow,
    onSurfaceVariant = BlueprintMute,
    surfaceContainer = BlueprintContainerLow,
    surfaceContainerLow = BlueprintBg,
    surfaceContainerLowest = BlueprintSurface,
    surfaceContainerHigh = BlueprintContainerHigh,
    surfaceContainerHighest = BlueprintContainerHighest,
    outline = BlueprintLine,
    outlineVariant = BlueprintLine,
    error = CompileRed,
    onError = BlueprintSurface,
    errorContainer = CompileRedContainer,
    onErrorContainer = Color(0xFF410002),
)

@Composable
fun DreamsTheme(
    content: @Composable () -> Unit,
) {
    val prefsRepo: UserPrefsRepository = koinInject()
    val prefs by prefsRepo.prefsFlow.collectAsStateWithLifecycle(initialValue = UserPrefs.DEFAULT)
    val useDynamicColor = prefs.useDynamicColor
    val resolvedDarkTheme = when (prefs.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        useDynamicColor -> {
            val context = LocalContext.current
            if (resolvedDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        resolvedDarkTheme -> DarkBrandScheme
        else -> LightBrandScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
