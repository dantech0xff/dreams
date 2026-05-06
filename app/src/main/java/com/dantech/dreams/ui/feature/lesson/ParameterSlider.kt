package com.dantech.dreams.ui.feature.lesson

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dantech.dreams.data.lesson.LessonControl
import com.dantech.dreams.ui.theme.Tokens

@Composable
fun ParameterSlider(
    control: LessonControl.FloatRange,
    value: Float,
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
            // Mono + tabular figures: the readout sits in a fixed column and
            // doesn't jitter horizontally as the user drags the slider.
            Text(
                text = "%.2f".format(value),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = Tokens.mono,
                    fontFeatureSettings = "tnum",
                ),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value,
            onValueChange = onValue,
            valueRange = control.min..control.max,
        )
    }
}
