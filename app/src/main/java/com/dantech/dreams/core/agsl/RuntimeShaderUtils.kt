package com.dantech.dreams.core.agsl

import android.graphics.RuntimeShader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos

/**
 * Created by dan on 6/5/26
 *
 * Copyright © 2026 Dan Tech. All rights reserved.
 */

/** Build & remember a [RuntimeShader] from AGSL source. Recreated only when source changes. */
@Composable
fun rememberRuntimeShader(source: String): RuntimeShader =
    remember(source) { RuntimeShader(source) }

/**
 * Seconds-since-first-frame, ticking every frame. Read this State inside a draw or
 * graphicsLayer block to drive shader animation — any read forces redraw on the next frame.
 */
@Composable
fun rememberShaderTime(): State<Float> = produceState(initialValue = 0f) {
    var start = 0L
    while (true) {
        val now = withFrameNanos { it }
        if (start == 0L) start = now
        value = (now - start) / 1_000_000_000f
    }
}