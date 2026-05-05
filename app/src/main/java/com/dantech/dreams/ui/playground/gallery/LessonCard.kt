package com.dantech.dreams.ui.playground.gallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dantech.dreams.data.lesson.LessonModel

@Composable
fun LessonCard(
    lesson: LessonModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val a11yLabel =
        "${lesson.title}, ${lesson.category.displayName}, complexity ${lesson.complexity} of 5"
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .semantics { contentDescription = a11yLabel }
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row {
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "★".repeat(lesson.complexity) + "☆".repeat(5 - lesson.complexity),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Text(
                text = lesson.conceptIntro,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
            )
        }
    }
}
