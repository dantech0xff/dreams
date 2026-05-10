package com.dantech.dreams.ui.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dantech.dreams.data.prefs.ThemeMode

@Composable
internal fun DisplaySettingsSection(
    themeMode: ThemeMode,
    reducedMotion: Boolean,
    useDynamicColor: Boolean,
    onThemeMode: (ThemeMode) -> Unit,
    onReducedMotion: (Boolean) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
) {
    SectionHeader("Display")
    DarkThemeRow(
        checked = themeMode == ThemeMode.DARK,
        onChecked = { enabled ->
            onThemeMode(if (enabled) ThemeMode.DARK else ThemeMode.LIGHT)
        },
    )
    SwitchRow(
        title = "Reduce motion",
        subtitle = "Skip transitions and shared element morphs.",
        checked = reducedMotion,
        onChecked = onReducedMotion,
    )
    SwitchRow(
        title = "Material You theme",
        subtitle = "Tint UI from your wallpaper. Off keeps the brand palette.",
        checked = useDynamicColor,
        onChecked = onDynamicColor,
    )
}

@Composable
internal fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
    )
}

@Composable
private fun DarkThemeRow(checked: Boolean, onChecked: (Boolean) -> Unit) {
    SwitchRow(
        title = "Dark theme",
        subtitle = "Use the dark Shader Lab palette.",
        checked = checked,
        onChecked = onChecked,
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
