package com.dantech.dreams.ui.feature.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.dantech.dreams.data.lesson.LessonControl
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.ui.theme.CalibrationAmber
import com.dantech.dreams.ui.theme.CrtCyan
import com.dantech.dreams.ui.theme.HotPixelMagenta
import com.dantech.dreams.ui.theme.PhosphorGreen
import com.dantech.dreams.ui.theme.accent
import kotlinx.collections.immutable.ImmutableMap

private val DEFAULT_COLOR_SWATCHES = listOf(
    HotPixelMagenta,
    CrtCyan,
    CalibrationAmber,
    PhosphorGreen,
    Color(0xFF9AB6FF),
    Color(0xFFFFB000),
    Color(0xFFE7FFF1),
    Color(0xFF04100B),
)

/**
 * Per-frame snapshot store for float-uniform values. Seeded from VM persistence
 * (debounced 200ms in `LessonDetailViewModel`); the VM remains the canonical
 * source of truth, this map drives the Compose-thread per-frame writes.
 */
@Composable
fun rememberFloatControlValues(
    lesson: LessonModel,
    overrides: ImmutableMap<String, Float>,
): SnapshotStateMap<String, Float> = remember(lesson.id, overrides) {
    mutableStateMapOf<String, Float>().apply {
        lesson.controls
            .filterIsInstance<LessonControl.FloatRange>()
            .forEach { c -> this[c.uniformName] = overrides[c.uniformName] ?: c.default }
    }
}

@Composable
fun rememberColorControlValues(
    lesson: LessonModel,
    overrides: ImmutableMap<String, Int>,
): SnapshotStateMap<String, Color> = remember(lesson.id, overrides) {
    mutableStateMapOf<String, Color>().apply {
        lesson.controls
            .filterIsInstance<LessonControl.ColorPicker>()
            .forEach { c ->
                this[c.uniformName] = overrides[c.uniformName]?.let(::Color) ?: c.default
            }
    }
}

@Composable
fun LessonControlsSection(
    lesson: LessonModel,
    floatValues: SnapshotStateMap<String, Float>,
    colorValues: SnapshotStateMap<String, Color>,
    onFloatChange: (uniform: String, value: Float) -> Unit,
    onColorChange: (uniform: String, color: Color) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (lesson.controls.isEmpty()) return
    val accent = lesson.category.accent
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "Controls",
                color = accent,
                style = MaterialTheme.typography.titleSmall,
            )
            TextButton(
                onClick = {
                    resetControlValues(lesson, floatValues, colorValues)
                    onReset()
                },
            ) {
                Text("Reset", color = accent)
            }
        }
        lesson.controls.forEach { control ->
            when (control) {
                is LessonControl.FloatRange -> ParameterSlider(
                    control = control,
                    value = floatValues[control.uniformName] ?: control.default,
                    accent = accent,
                    onValue = { v ->
                        floatValues[control.uniformName] = v
                        onFloatChange(control.uniformName, v)
                    },
                )

                is LessonControl.ColorPicker -> ColorSwatchControl(
                    control = control,
                    value = colorValues[control.uniformName] ?: control.default,
                    accent = accent,
                    onValue = { color ->
                        colorValues[control.uniformName] = color
                        onColorChange(control.uniformName, color)
                    },
                )
            }
        }
    }
}

@Composable
private fun ColorSwatchControl(
    control: LessonControl.ColorPicker,
    value: Color,
    accent: Color,
    onValue: (Color) -> Unit,
) {
    val palette = remember(control.default, value) {
        (listOf(control.default, value) + DEFAULT_COLOR_SWATCHES).distinctBy { it.toArgb() }
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(control.name)
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            palette.forEach { swatch ->
                val selected = swatch.toArgb() == value.toArgb()
                ColorSwatch(
                    color = swatch,
                    selected = selected,
                    accent = accent,
                    onClick = { onValue(swatch) },
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (selected) accent.copy(alpha = 0.18f) else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(if (selected) 38.dp else 34.dp)
                .shadow(if (selected) 6.dp else 0.dp, CircleShape, clip = false)
                .clip(CircleShape)
                .background(color)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(Modifier.fillMaxSize().background(accent.copy(alpha = 0.28f)))
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

private fun resetControlValues(
    lesson: LessonModel,
    floatValues: SnapshotStateMap<String, Float>,
    colorValues: SnapshotStateMap<String, Color>,
) {
    lesson.controls.forEach { control ->
        when (control) {
            is LessonControl.FloatRange -> floatValues[control.uniformName] = control.default
            is LessonControl.ColorPicker -> colorValues[control.uniformName] = control.default
        }
    }
}
