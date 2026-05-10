package com.dantech.dreams.ui.feature.lesson

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.dantech.dreams.core.motion.LocalSharedTransitionScope
import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.ui.feature.common.lessonSharedKey
import com.dantech.dreams.ui.feature.common.rememberShaderBindings
import kotlinx.collections.immutable.ImmutableList
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun LessonDetailScreen(
    onBack: () -> Unit,
    vm: LessonDetailViewModel = koinViewModel(),
) {
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val lesson = ui.lesson
    if (lesson == null) {
        Text("Lesson not found", Modifier.padding(24.dp))
        return
    }

    val sharedScope = LocalSharedTransitionScope.current
    val animScope = LocalNavAnimatedContentScope.current
    val heroSharedMod = if (sharedScope != null) {
        with(sharedScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(lessonSharedKey(lesson.id)),
                animatedVisibilityScope = animScope,
            )
        }
    } else Modifier

    // SnapshotStateMap drives per-frame uniform writes on the Compose thread; the VM
    // owns the canonical paramOverrides map for persistence (phase-05).
    val floatValues = rememberFloatControlValues(lesson, ui.paramOverrides)
    val colorValues = rememberColorControlValues(lesson, ui.colorOverrides)
    val bindings = rememberShaderBindings(lesson)
    val previewShape = RoundedCornerShape(24.dp)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(lesson.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(16.dp)
                    .then(heroSharedMod)
                    .shadow(elevation = 18.dp, shape = previewShape, clip = false)
                    .clip(previewShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceContainerHighest,
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                LessonPreview(
                    lesson = lesson,
                    bindings = bindings,
                    floatValues = floatValues,
                    colorValues = colorValues,
                )
            }

            Text(
                text = lesson.conceptIntro,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            LessonControlsSection(
                lesson = lesson,
                floatValues = floatValues,
                colorValues = colorValues,
                onFloatChange = vm::setFloat,
                onColorChange = vm::setColor,
                onReset = vm::resetOverrides,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            LearningNotesSection(
                notes = lesson.learningNotes,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            lesson.screenRecordingHint?.let {
                Text(
                    text = "Recording hint: $it",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            AgslSourceViewer(
                source = lesson.agslSource,
                initiallyExpanded = lesson.category == LessonCategory.BASICS,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun LearningNotesSection(notes: ImmutableList<String>, modifier: Modifier = Modifier) {
    if (notes.isEmpty()) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "What to notice",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            notes.forEach { note ->
                Text(
                    text = "- $note",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
