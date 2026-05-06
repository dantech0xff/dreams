package com.dantech.dreams.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.dantech.dreams.data.prefs.UserPrefs
import com.dantech.dreams.data.prefs.UserPrefsRepository
import org.koin.compose.koinInject

// Brand chrome is dark-first — see Color.kt rationale. Light scheme derived for
// daytime/AOD use. dynamicColor (Material You wallpaper-tinted) is opt-in via
// SettingsScreen so the brand identity is the default; users who want wallpaper
// chrome can enable it.

private val DarkBrandScheme = darkColorScheme(
    primary = NeonViolet80,
    onPrimary = Midnight,
    primaryContainer = NeonViolet20,
    onPrimaryContainer = NeonViolet80,
    secondary = SignalCyan80,
    onSecondary = Midnight,
    secondaryContainer = SignalCyan20,
    onSecondaryContainer = SignalCyan80,
    tertiary = FluxRose,
    onTertiary = Midnight,
    background = Midnight,
    onBackground = EngineerInk,
    surface = MidnightLow,
    onSurface = EngineerInk,
    surfaceVariant = MidnightMid,
    onSurfaceVariant = EngineerMute,
    surfaceContainer = MidnightMid,
    surfaceContainerLow = MidnightLow,
    surfaceContainerLowest = Midnight,
    surfaceContainerHigh = MidnightHigh,
    surfaceContainerHighest = MidnightHigher,
    outline = EngineerLine,
    outlineVariant = EngineerLine,
    error = CompileRed,
    onError = Midnight,
)

private val LightBrandScheme = lightColorScheme(
    primary = NeonViolet,
    onPrimary = DaySurface,
    primaryContainer = Color(0xFFEDE3FF),
    onPrimaryContainer = Color(0xFF2A1A55),
    secondary = Color(0xFF0891B2),       // saturated cyan for light bg contrast
    onSecondary = DaySurface,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF154A52),
    tertiary = Color(0xFFE11D48),
    onTertiary = DaySurface,
    background = DayBg,
    onBackground = DayInk,
    surface = DaySurface,
    onSurface = DayInk,
    surfaceVariant = DayContainerLo,
    onSurfaceVariant = DayMute,
    surfaceContainer = DayContainerLo,
    surfaceContainerLow = DayBg,
    surfaceContainerLowest = DaySurface,
    surfaceContainerHigh = DayContainerHi,
    surfaceContainerHighest = DayContainerHi,
    outline = DayLine,
    outlineVariant = DayLine,
    error = CompileRed,
    onError = DaySurface,
)

// Avoid blocking startup if Koin isn't ready yet — fall back to brand defaults.
@Composable
fun DreamsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val prefsRepo: UserPrefsRepository = koinInject()
    val prefs by prefsRepo.prefsFlow.collectAsStateWithLifecycle(initialValue = UserPrefs.DEFAULT)
    val useDynamicColor = prefs.useDynamicColor

    val colorScheme = when {
        useDynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkBrandScheme
        else -> LightBrandScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
