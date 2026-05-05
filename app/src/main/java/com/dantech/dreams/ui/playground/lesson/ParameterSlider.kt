package com.dantech.dreams.ui.playground.lesson

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dantech.dreams.data.lesson.LessonControl

@Composable
fun ParameterSlider(
    control: LessonControl.FloatRange,
    value: Float,
    onValue: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = "${control.name}: ${"%.2f".format(value)}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = value,
            onValueChange = onValue,
            valueRange = control.min..control.max,
        )
    }
}
