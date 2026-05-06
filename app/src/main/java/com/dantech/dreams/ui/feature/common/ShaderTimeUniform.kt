package com.dantech.dreams.ui.feature.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos

/**
 * Drives a Compose-observable shader time. Returns a State<Float> that ticks each frame.
 * Reading this state inside a `drawBehind`/`graphicsLayer` block invalidates that block
 * every frame — which is what makes the shader actually animate. Writing the uniform
 * directly on the RuntimeShader does NOT invalidate Compose drawing, so prefer this
 * over manual `setFloatUniform("time", ...)`.
 *
 * If the shader source does not declare a `time` uniform, the state stays at 0
 * (so callers can blindly read it without paying for unused recomposition).
 *
 * Set `paused = true` to freeze the clock — useful during nav transitions
 * (especially predictive back's scale-down) where the GPU needs to be free for
 * the system's window animation. Resume is seamless: the next unpaused frame
 * picks up from the paused value via delta accumulation rather than wall-clock.
 *
 * AGSL playback is graphical content, not a UI animation — the clock is intentionally
 * NOT gated by AccessibilityManager.isAnimatorDurationScaleNonZero or the
 * Settings.Global.ANIMATOR_DURATION_SCALE developer-options flag. Gating froze the
 * showcases (and the touch-driven ripple, where touchTime - time stays 0) on devs who
 * had animator scale disabled.
 */
@Composable
fun rememberShaderTime(
    shaderSource: String,
    uniformName: String = "time",
    paused: Boolean = false,
): State<Float> {
    val declared = remember(shaderSource, uniformName) {
        Regex("""uniform\s+float\s+$uniformName\s*;""").containsMatchIn(shaderSource)
    }
    val pausedState = rememberUpdatedState(paused)
    val state = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(declared) {
        if (!declared) {
            state.floatValue = 0f
            return@LaunchedEffect
        }
        var accumulatedNanos = 0L
        var lastFrameNanos = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val delta = now - lastFrameNanos
                lastFrameNanos = now
                if (!pausedState.value) {
                    accumulatedNanos += delta
                    state.floatValue = (accumulatedNanos / 1_000_000_000f) % 1000f
                }
            }
        }
    }
    return state
}
