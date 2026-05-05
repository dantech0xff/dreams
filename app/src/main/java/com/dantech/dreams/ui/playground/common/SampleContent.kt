package com.dantech.dreams.ui.playground.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Reusable sample composable consumed by every Post-FX lesson.
 * Avoids bundling a third-party image: a gradient + text card produces a rich
 * enough input for shader effects without licensing concerns.
 */
@Composable
fun SampleContent(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0F2027),
                        Color(0xFF203A43),
                        Color(0xFF2C5364),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xCC101418))
                .padding(24.dp),
        ) {
            Text(
                text = "Sample Card",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = "AGSL post-effect applied via RenderEffect on this composable.",
                color = Color(0xCCFFFFFF),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
