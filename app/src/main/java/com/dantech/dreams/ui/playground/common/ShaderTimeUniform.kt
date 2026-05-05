package com.dantech.dreams.ui.playground.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
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
 * AGSL playback is graphical content, not a UI animation — the clock is intentionally
 * NOT gated by AccessibilityManager.isAnimatorDurationScaleNonZero or the
 * Settings.Global.ANIMATOR_DURATION_SCALE developer-options flag. Gating froze the
 * showcases (and the touch-driven ripple, where touchTime - time stays 0) on devs who
 * had animator scale disabled.
 */
@Composable
fun rememberShaderTime(shaderSource: String, uniformName: String = "time"): State<Float> {
    val declared = remember(shaderSource, uniformName) {
        Regex("""uniform\s+float\s+$uniformName\s*;""").containsMatchIn(shaderSource)
    }
    val state = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(declared) {
        if (!declared) {
            state.floatValue = 0f
            return@LaunchedEffect
        }
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                state.floatValue = ((now - start) / 1_000_000_000f) % 1000f
            }
        }
    }
    return state
}
