package com.dantech.dreams.ui.feature.lesson

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.dantech.dreams.core.motion.LocalSharedTransitionScope
import com.dantech.dreams.ui.feature.common.lessonSharedKey
import com.dantech.dreams.ui.feature.common.rememberShaderBindings
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
    val colorValues = rememberColorControlValues(lesson)
    val bindings = rememberShaderBindings(lesson)

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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(horizontal = 16.dp)
                    .then(heroSharedMod),
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
                onFloatChange = vm::setFloat,
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
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}
