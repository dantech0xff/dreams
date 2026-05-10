package com.dantech.dreams.ui.feature.lesson

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dantech.dreams.data.lesson.LessonControl

@Composable
fun ParameterSlider(
    control: LessonControl.FloatRange,
    value: Float,
    accent: Color,
    onValue: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = control.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            // tabular figures keep the readout from jittering horizontally as
            // the user drags the slider — same brand face, fixed-width digits.
            Text(
                text = "%.2f".format(value),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFeatureSettings = "tnum",
                ),
                color = accent,
            )
        }
        Slider(
            value = value,
            onValueChange = onValue,
            valueRange = control.min..control.max,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                activeTickColor = accent,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.26f),
                inactiveTickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.26f),
            ),
        )
    }
}
