package com.dantech.dreams.shaders.agsl

/**
 * Created by dan on 6/5/26
 *
 * Copyright © 2026 Dan Tech. All rights reserved.
 */

import android.graphics.RenderEffect as NativeRenderEffect
import android.graphics.RuntimeShader
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Apply [shader] as a RenderEffect over this composable's rendered content. The content is
 * bound as the named input shader (default `content`) so the AGSL can `eval(coord)` it.
 *
 * [onFrame] runs inside the graphicsLayer block each frame the layer is recomputed, with
 * the layer size in pixels. Compose State reads inside it (e.g. animated time) drive
 * per-frame redraws automatically.
 *
 * `compositingStrategy = Offscreen` is mandatory: RenderEffect needs an offscreen
 * texture to sample for `content.eval(...)`. With the default Auto strategy Compose may
 * skip allocating that texture and the effect renders to nothing (blank preview).
 *
 * The runtime shader effect snapshots uniforms at construction time (Skia's
 * SkRuntimeImageFilter holds a frozen builder), so we rebuild the effect inside the
 * layer block — every frame the block re-runs ships the latest uniform values.
 */
fun Modifier.runtimeShaderEffect(
    shader: RuntimeShader,
    inputName: String = "content",
    onFrame: (Size) -> Unit = {},
): Modifier = graphicsLayer {
    onFrame(size)
    clip = true
    compositingStrategy = CompositingStrategy.Offscreen
    renderEffect = NativeRenderEffect
        .createRuntimeShaderEffect(shader, inputName)
        .asComposeRenderEffect()
}