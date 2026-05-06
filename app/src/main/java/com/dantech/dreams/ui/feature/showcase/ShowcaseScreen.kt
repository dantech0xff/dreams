package com.dantech.dreams.ui.feature.showcase

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dantech.dreams.ui.theme.Tokens
import com.dantech.dreams.ui.theme.accent
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

private const val HINT_AUTO_HIDE_MS = 4500L
private const val CHROME_ENTRY_DELAY_MS = 80L
private const val CHROME_ENTRY_MS = 320
private const val CHROME_EXIT_MS = 220

@Composable
fun ShowcaseScreen(
    onBack: () -> Unit,
    vm: ShowcaseViewModel = koinViewModel(),
) {
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val lesson = ui.lesson
    if (lesson == null) {
        ShowcaseMissing()
        return
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Lesson owns its shader, time, gestures, and backdrop. The chrome
        // floats on top with edge-only hit zones so multi-touch lessons
        // (e.g. ripple drag) keep their full canvas.
        lesson.customPreview?.invoke()

        ShowcaseTopScrim(modifier = Modifier.align(Alignment.TopStart))

        ChromeEntry {
            ShowcaseTopBar(
                lesson = lesson,
                onBack = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(
                        horizontal = Tokens.spaceMd,
                        vertical = Tokens.spaceSm,
                    ),
            )
        }

        lesson.screenRecordingHint?.let { hint ->
            AutoHidingHint(
                text = hint,
                accent = lesson.category.accent,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(Tokens.spaceLg),
            )
        }
    }
}

// Slide-and-fade entry, delayed a frame so the lesson backdrop renders first
// — avoids the "chrome flashes before content" pop on cold navigation.
@Composable
private fun ChromeEntry(content: @Composable () -> Unit) {
    var visible by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(CHROME_ENTRY_DELAY_MS)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(CHROME_ENTRY_MS)) +
            slideInVertically(tween(CHROME_ENTRY_MS)) { -it / 3 },
        exit = fadeOut(tween(CHROME_EXIT_MS)),
    ) {
        content()
    }
}

@Composable
private fun AutoHidingHint(
    text: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    var visible by rememberSaveable(text) { mutableStateOf(true) }
    LaunchedEffect(text) {
        delay(HINT_AUTO_HIDE_MS)
        visible = false
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(CHROME_ENTRY_MS)) +
            slideInVertically(tween(CHROME_ENTRY_MS)) { it / 3 },
        exit = fadeOut(tween(CHROME_EXIT_MS)) +
            slideOutVertically(tween(CHROME_EXIT_MS)) { it / 4 },
        modifier = modifier,
    ) {
        ShowcaseHintPill(text = text, accent = accent)
    }
}

@Composable
private fun ShowcaseMissing() {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Showcase not found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
