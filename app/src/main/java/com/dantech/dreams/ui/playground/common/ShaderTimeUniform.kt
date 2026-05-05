package com.dantech.dreams.ui.playground.common

import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService

@Composable
fun rememberAnimationsEnabled(): Boolean {
    val ctx = LocalContext.current
    val am = remember(ctx) { ctx.getSystemService<AccessibilityManager>() }
    return remember(am) {
        try {
            val m = AccessibilityManager::class.java.getMethod("isAnimatorDurationScaleNonZero")
            m.invoke(am) as? Boolean ?: true
        } catch (_: Throwable) {
            android.provider.Settings.Global.getFloat(
                ctx.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) > 0f
        }
    }
}

/**
 * Drives a Compose-observable shader time. Returns a State<Float> that ticks each frame.
 * Reading this state inside a `drawBehind`/`graphicsLayer` block invalidates that block
 * every frame — which is what makes the shader actually animate. Writing the uniform
 * directly on the RuntimeShader does NOT invalidate Compose drawing, so prefer this
 * over manual `setFloatUniform("time", ...)`.
 *
 * If the shader source does not declare a `time` uniform, the state stays at 0
 * (so callers can blindly read it without paying for unused recomposition).
 */
@Composable
fun rememberShaderTime(shaderSource: String, uniformName: String = "time"): State<Float> {
    val declared = remember(shaderSource, uniformName) {
        Regex("""uniform\s+float\s+$uniformName\s*;""").containsMatchIn(shaderSource)
    }
    val animationsOn = rememberAnimationsEnabled()
    val state = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(declared, animationsOn) {
        if (!declared || !animationsOn) {
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
