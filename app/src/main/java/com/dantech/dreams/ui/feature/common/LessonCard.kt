package com.dantech.dreams.ui.feature.common

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.dantech.dreams.core.motion.LocalSharedTransitionScope
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.ui.theme.HotPixelMagenta
import com.dantech.dreams.ui.theme.LessonIcons
import com.dantech.dreams.ui.theme.Tokens
import com.dantech.dreams.ui.theme.accent

private const val MAX_COMPLEXITY = 5

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
        "${lesson.title}, ${lesson.category.displayName}, complexity ${lesson.complexity} of $MAX_COMPLEXITY, $favoriteLabel"

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

    val accent = lesson.category.accent
    val panelShape = RoundedCornerShape(20.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(sharedMod)
            .semantics { contentDescription = a11yLabel }
            .clickable(onClick = onClick),
        shape = panelShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            TitleRow(
                title = lesson.title,
                accent = accent,
                isFavorite = isFavorite,
                onToggleFavorite = onToggleFavorite,
            )
            Spacer(Modifier.height(Tokens.spaceSm))
            Text(
                text = lesson.conceptIntro,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
            Spacer(Modifier.height(Tokens.spaceMd))
            FooterRow(
                category = lesson.category.displayName,
                complexity = lesson.complexity,
                accent = accent,
            )
        }
    }
}

@Composable
private fun TitleRow(
    title: String,
    accent: Color,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Spacer(Modifier.width(Tokens.spaceSm))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            // Cap to two lines so 2-col grid cells stay visually balanced;
            // long shader names ellipsize rather than blow the layout.
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
                tint = if (isFavorite) HotPixelMagenta else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FooterRow(category: String, complexity: Int, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = category.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = accent.copy(alpha = 0.88f),
            modifier = Modifier.weight(1f),
        )
        ComplexityBolts(level = complexity, accent = accent)
    }
}

/**
 * Lesson "intensity" indicator. Bolts (lightning) reads as voltage/load — better
 * semantic match for shader complexity than stars (which imply a quality rating).
 */
@Composable
fun ComplexityBolts(
    level: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val mute = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        repeat(MAX_COMPLEXITY) { i ->
            Icon(
                imageVector = LessonIcons.Bolt,
                contentDescription = null,
                tint = if (i < level) accent else mute,
                modifier = Modifier.size(14.dp),
            )
            if (i < MAX_COMPLEXITY - 1) Spacer(Modifier.width(2.dp))
        }
    }
}
