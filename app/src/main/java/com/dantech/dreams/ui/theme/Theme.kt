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
// the Shader Lab brand identity is the default.

private val DarkBrandScheme = darkColorScheme(
    primary = SignalCyan,
    onPrimary = GraphiteBg,
    primaryContainer = SignalCyanContainerDark,
    onPrimaryContainer = SignalCyanContainer,
    secondary = FluxRose,
    onSecondary = GraphiteBg,
    secondaryContainer = FluxRoseContainerDark,
    onSecondaryContainer = FluxRoseContainer,
    tertiary = PhotonAmber,
    onTertiary = GraphiteBg,
    tertiaryContainer = PhotonAmberContainerDark,
    onTertiaryContainer = PhotonAmberContainer,
    background = GraphiteBg,
    onBackground = ShaderInk,
    surface = GraphiteSurface,
    onSurface = ShaderInk,
    surfaceVariant = GraphiteContainerLow,
    onSurfaceVariant = ShaderMute,
    surfaceContainer = GraphiteContainerLow,
    surfaceContainerLow = GraphiteSurface,
    surfaceContainerLowest = GraphiteBg,
    surfaceContainerHigh = GraphiteContainerHigh,
    surfaceContainerHighest = GraphiteContainerHighest,
    outline = ShaderLine,
    outlineVariant = ShaderLine,
    error = CompileRed,
    onError = GraphiteBg,
    errorContainer = CompileRedContainerDark,
    onErrorContainer = CompileRedContainer,
)

private val LightBrandScheme = lightColorScheme(
    primary = SignalCyanDark,
    onPrimary = PaperSurface,
    primaryContainer = SignalCyanContainer,
    onPrimaryContainer = Color(0xFF00363D),
    secondary = FluxRoseDark,
    onSecondary = PaperSurface,
    secondaryContainer = FluxRoseContainer,
    onSecondaryContainer = Color(0xFF4C0031),
    tertiary = PhotonAmberDark,
    onTertiary = PaperSurface,
    tertiaryContainer = PhotonAmberContainer,
    onTertiaryContainer = Color(0xFF3E2600),
    background = InkBg,
    onBackground = DayInk,
    surface = PaperSurface,
    onSurface = DayInk,
    surfaceVariant = InkContainerLow,
    onSurfaceVariant = DayMute,
    surfaceContainer = InkContainerLow,
    surfaceContainerLow = InkBg,
    surfaceContainerLowest = PaperSurface,
    surfaceContainerHigh = InkContainerHigh,
    surfaceContainerHighest = InkContainerHighest,
    outline = DayLine,
    outlineVariant = DayLine,
    error = CompileRed,
    onError = PaperSurface,
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
