package com.dantech.dreams.ui.feature.showcase

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.ui.theme.Tokens
import com.dantech.dreams.ui.theme.accent

private const val MAX_COMPLEXITY = 5
private val GlassFill = Color(0x66000000)
private val ChipFill = Color(0x80000000)
private const val MutedAlpha = 0.62f

// Soft top-down vignette so the back button + title pill stay legible on
// any backdrop the lesson chooses to render — bright shaders, white peaks,
// or otherwise. Pure overlay; no pointer cost.
@Composable
fun ShowcaseTopScrim(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(
                Brush.verticalGradient(
                    0f to Color(0x88000000),
                    0.6f to Color(0x33000000),
                    1f to Color.Transparent,
                ),
            ),
    )
}

@Composable
fun ShowcaseTopBar(
    lesson: LessonModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        GlassBackButton(onBack = onBack)
        Spacer(Modifier.width(Tokens.spaceSm))
        LessonInfoPill(lesson = lesson, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun GlassBackButton(onBack: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .shadow(4.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(GlassFill)
            .clickable(onClick = onBack),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun LessonInfoPill(lesson: LessonModel, modifier: Modifier = Modifier) {
    val accent = lesson.category.accent
    val shape = RoundedCornerShape(22.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .shadow(4.dp, shape, clip = false)
            .clip(shape)
            .background(GlassFill)
            .padding(start = 12.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = lesson.category.label(),
                style = MaterialTheme.typography.labelSmall,
                color = accent,
            )
            Text(
                text = lesson.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        ComplexityDots(level = lesson.complexity, accent = accent)
    }
}

@Composable
private fun ComplexityDots(level: Int, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(MAX_COMPLEXITY) { index ->
            val on = index < level
            Box(
                Modifier
                    .size(if (on) 6.dp else 5.dp)
                    .clip(CircleShape)
                    .background(if (on) accent else Color.White.copy(alpha = 0.18f)),
            )
            if (index < MAX_COMPLEXITY - 1) Spacer(Modifier.width(4.dp))
        }
    }
}

@Composable
fun ShowcaseHintPill(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .shadow(4.dp, shape, clip = false)
            .clip(shape)
            .background(ChipFill)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        PulsingDot(color = accent)
        Spacer(Modifier.width(10.dp))
        Text(
            text = "REC",
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = MutedAlpha + 0.18f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PulsingDot(color: Color) {
    val transition = rememberInfiniteTransition(label = "rec-pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "rec-pulse-alpha",
    )
    Box(
        Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha)),
    )
}

private fun com.dantech.dreams.data.lesson.LessonCategory.label(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }
