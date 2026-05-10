package com.dantech.dreams.ui.feature.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.dantech.dreams.data.lesson.LessonControl
import com.dantech.dreams.data.lesson.LessonModel
import kotlinx.collections.immutable.ImmutableMap

private val DEFAULT_COLOR_SWATCHES = listOf(
    Color(0xFFE91E63),
    Color(0xFF2196F3),
    Color(0xFFFACC15),
    Color(0xFF22C55E),
    Color(0xFFA855F7),
    Color(0xFFF97316),
    Color(0xFFF8FAFC),
    Color(0xFF0F172A),
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
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Controls")
            TextButton(
                onClick = {
                    resetControlValues(lesson, floatValues, colorValues)
                    onReset()
                },
            ) {
                Text("Reset")
            }
        }
        lesson.controls.forEach { control ->
            when (control) {
                is LessonControl.FloatRange -> ParameterSlider(
                    control = control,
                    value = floatValues[control.uniformName] ?: control.default,
                    onValue = { v ->
                        floatValues[control.uniformName] = v
                        onFloatChange(control.uniformName, v)
                    },
                )

                is LessonControl.ColorPicker -> ColorSwatchControl(
                    control = control,
                    value = colorValues[control.uniformName] ?: control.default,
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
                    onClick = { onValue(swatch) },
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(color)
            .border(if (selected) 3.dp else 1.dp, borderColor, CircleShape)
            .clickable(onClick = onClick),
    )
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
