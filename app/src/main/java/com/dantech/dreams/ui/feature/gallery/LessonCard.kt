package com.dantech.dreams.ui.feature.gallery

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.dantech.dreams.core.motion.LocalSharedTransitionScope
import com.dantech.dreams.data.lesson.LessonModel

fun lessonSharedKey(lessonId: String) = "lesson-card-$lessonId"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LessonCard(
    lesson: LessonModel,
    onClick: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val favoriteLabel = if (isFavorite) "favorited" else "not favorited"
    val a11yLabel =
        "${lesson.title}, ${lesson.category.displayName}, complexity ${lesson.complexity} of 5, $favoriteLabel"

    val sharedScope = LocalSharedTransitionScope.current
    val animScope = LocalNavAnimatedContentScope.current
    val sharedMod = if (sharedScope != null) {
        with(sharedScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(lessonSharedKey(lesson.id)),
                animatedVisibilityScope = animScope,
            )
        }
    } else Modifier

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .then(sharedMod)
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
                IconButton(onClick = onToggleFavorite) {
                    Text(
                        text = if (isFavorite) "♥" else "♡",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            Text(
                text = lesson.conceptIntro,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
            )
        }
    }
}
