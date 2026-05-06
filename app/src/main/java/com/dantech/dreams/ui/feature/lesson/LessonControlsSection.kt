package com.dantech.dreams.ui.feature.lesson

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.dantech.dreams.data.lesson.LessonControl
import com.dantech.dreams.data.lesson.LessonModel
import kotlinx.collections.immutable.ImmutableMap

/**
 * Per-frame snapshot store for float-uniform values. Seeded from VM persistence
 * (debounced 200ms in `LessonDetailViewModel`); the VM remains the canonical
 * source of truth, this map drives the Compose-thread per-frame writes.
 */
@Composable
fun rememberFloatControlValues(
    lesson: LessonModel,
    overrides: ImmutableMap<String, Float>,
): SnapshotStateMap<String, Float> = remember(lesson.id) {
    mutableStateMapOf<String, Float>().apply {
        lesson.controls
            .filterIsInstance<LessonControl.FloatRange>()
            .forEach { c -> this[c.uniformName] = overrides[c.uniformName] ?: c.default }
    }
}

@Composable
fun rememberColorControlValues(
    lesson: LessonModel,
): SnapshotStateMap<String, Color> = remember(lesson.id) {
    mutableStateMapOf<String, Color>().apply {
        lesson.controls
            .filterIsInstance<LessonControl.ColorPicker>()
            .forEach { c -> this[c.uniformName] = c.default }
    }
}

@Composable
fun LessonControlsSection(
    lesson: LessonModel,
    floatValues: SnapshotStateMap<String, Float>,
    onFloatChange: (uniform: String, value: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (lesson.controls.isEmpty()) return
    Column(modifier) {
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

                is LessonControl.ColorPicker -> Text(
                    "${control.name} (color picker not yet implemented)",
                )
            }
        }
    }
}
