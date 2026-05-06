package com.dantech.dreams.core.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dantech.dreams.data.prefs.UserPrefs
import com.dantech.dreams.data.prefs.UserPrefsRepository
import org.koin.compose.koinInject

@Immutable
data class AppMotionState(val reducedMotion: Boolean) {
    val transitionDurationMs: Int get() = if (reducedMotion) 0 else DEFAULT_DURATION_MS

    private companion object {
        const val DEFAULT_DURATION_MS = 500
    }
}

@Composable
fun rememberAppMotionState(
    prefsRepo: UserPrefsRepository = koinInject(),
): AppMotionState {
    val ctx = LocalContext.current
    val prefs by prefsRepo.prefsFlow.collectAsStateWithLifecycle(initialValue = UserPrefs.DEFAULT)
    val sysEnabled = remember(ctx) { systemAnimatorEnabled(ctx) }
    return AppMotionState(reducedMotion = !sysEnabled || prefs.reducedMotionOverride)
}
