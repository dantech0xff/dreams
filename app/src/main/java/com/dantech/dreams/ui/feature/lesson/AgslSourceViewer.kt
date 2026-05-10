package com.dantech.dreams.ui.feature.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
fun AgslSourceViewer(
    source: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
) {
    var expanded by remember(source, initiallyExpanded) { mutableStateOf(initiallyExpanded) }
    val lineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
    val codeColor = MaterialTheme.colorScheme.onSurface
    val numberedSource = remember(source, lineColor, codeColor) {
        source.withScopeLineNumbers(lineColor = lineColor, codeColor = codeColor)
    }
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(elevation = 3.dp, shape = shape, clip = false)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = "SCOPE READOUT / AGSL",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
            )
            IconButton(onClick = { expanded = !expanded }) {
                Text(
                    text = if (expanded) "▲" else "▼",
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (expanded) {
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                Text(
                    text = numberedSource,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}

private fun String.withScopeLineNumbers(lineColor: Color, codeColor: Color) = buildAnnotatedString {
    val lines = lines()
    val width = lines.size.toString().length
    lines.forEachIndexed { index, line ->
        withStyle(SpanStyle(color = lineColor, fontWeight = FontWeight.Medium)) {
            append((index + 1).toString().padStart(width))
            append("  ")
        }
        withStyle(SpanStyle(color = codeColor)) {
            append(line)
        }
        if (index < lines.lastIndex) append("\n")
    }
}
