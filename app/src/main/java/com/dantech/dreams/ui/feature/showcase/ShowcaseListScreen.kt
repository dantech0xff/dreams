package com.dantech.dreams.ui.feature.showcase

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.ui.feature.common.ComplexityBolts
import com.dantech.dreams.ui.theme.Tokens
import com.dantech.dreams.ui.theme.accent
import org.koin.androidx.compose.koinViewModel

@Composable
fun ShowcaseListScreen(
    onShowcaseClick: (String) -> Unit,
    vm: ShowcaseListViewModel = koinViewModel(),
) {
    val ui by vm.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "header") { ScreenHeader(count = ui.showcases.size) }
        if (ui.showcases.isEmpty()) {
            item(key = "empty") { EmptyState() }
        } else {
            items(ui.showcases, key = { it.id }) { lesson ->
                ShowcaseCard(
                    lesson = lesson,
                    onClick = { onShowcaseClick(lesson.id) },
                )
            }
        }
    }
}

// Inline hero — scrolls with the list (matches LessonCategoriesScreen). The
// tagline pulls from the category enum so the copy stays in one place.
@Composable
private fun ScreenHeader(count: Int) {
    Column(Modifier.padding(top = 12.dp, bottom = 4.dp)) {
        Text(
            text = "Showcases",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = LessonCategory.SHOWCASE.tagline,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = pluralCount(count),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}

private fun pluralCount(count: Int): String = when (count) {
    0 -> "No showcases yet"
    1 -> "1 showcase"
    else -> "$count showcases"
}

@Composable
private fun ShowcaseCard(
    lesson: LessonModel,
    onClick: () -> Unit,
) {
    val accent = lesson.category.accent
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "showcase-card-press",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        AccentBanner(accent = accent, title = lesson.title, complexity = lesson.complexity)
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                text = lesson.conceptIntro,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            lesson.screenRecordingHint?.let { hint ->
                Spacer(Modifier.height(Tokens.spaceMd))
                RecChip(hint = hint, accent = accent)
            }
            Spacer(Modifier.height(Tokens.spaceMd))
            OpenAffordance(accent = accent)
        }
    }
}

// 16:9 calibrated header: flat accent wash without linework.
@Composable
private fun AccentBanner(accent: Color, title: String, complexity: Int) {
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
    ) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val base = MaterialTheme.colorScheme.surfaceContainerHighest
        val lower = MaterialTheme.colorScheme.surfaceContainerLow
        Box(
            Modifier
                .fillMaxSize()
                .background(base)
                .background(
                    Brush.linearGradient(
                        colors = listOf(lower, accent.copy(alpha = 0.28f), base),
                        start = Offset(0f, h),
                        end = Offset(w, 0f),
                    ),
                )
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xCC000000), Color.Transparent, Color(0x66000000)),
                        start = Offset(0f, h),
                        end = Offset(w, 0f),
                    ),
                ),
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accent),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = LessonCategory.SHOWCASE.displayName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            ComplexityBolts(level = complexity, accent = accent)
        }
    }
}

// Compact REC chip — same vocabulary as ShowcaseHintPill in-lesson, but
// scaled down for list density so it sits next to body copy without dominating.
@Composable
private fun RecChip(hint: String, accent: Color) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(shape)
            .background(accent.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(accent))
        Spacer(Modifier.width(8.dp))
        Text(
            text = "REC",
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun OpenAffordance(accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "OPEN",
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No showcases yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Cinema-mode shaders will appear here once they're registered.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
