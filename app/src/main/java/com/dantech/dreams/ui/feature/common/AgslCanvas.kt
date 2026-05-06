package com.dantech.dreams.ui.feature.common

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

/**
 * Writing a uniform that the shader does not declare throws and — under CheckJNI —
 * aborts the process with a Modified-UTF-8 error before our try/catch can catch.
 * So we statically detect the `resolution` declaration before writing.
 */
private fun declaresResolution(src: String): Boolean =
    Regex("""uniform\s+float2\s+resolution\s*;""").containsMatchIn(src)

/**
 * AGSL Brush canvas: shader fills the box.
 *
 * `setUniforms` runs INSIDE `drawBehind` — Compose tracks snapshot state reads there,
 * so reading control values / animated time inside the lambda will invalidate drawing
 * when those change. Setting uniforms outside the draw block does NOT redraw.
 */
@Composable
fun AgslBrushCanvas(
    shaderSrc: String,
    modifier: Modifier = Modifier,
    setUniforms: (RuntimeShader) -> Unit = {},
) {
    val (shader, error) = remember(shaderSrc) {
        try {
            RuntimeShader(shaderSrc) to null
        } catch (t: Throwable) {
            null to (t.message ?: "compile error")
        }
    }
    if (error != null || shader == null) {
        AgslErrorCard(message = error ?: "unknown", modifier = modifier)
        return
    }

    val hasResolution = remember(shaderSrc) { declaresResolution(shaderSrc) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                if (it != size) {
                    size = it
                    if (hasResolution) {
                        shader.setFloatUniform("resolution", it.width.toFloat(), it.height.toFloat())
                    }
                }
            }
            .drawBehind {
                setUniforms(shader)
                drawRect(brush = ShaderBrush(shader))
            },
    )
}

/**
 * AGSL RenderEffect canvas: shader applied as post-effect to the supplied content.
 *
 * `setUniforms` runs INSIDE `graphicsLayer` — Compose tracks snapshot state reads there,
 * so reading control values / animated time inside the lambda will invalidate the layer
 * when those change. Setting uniforms outside the layer block does NOT redraw.
 */
@Composable
fun AgslRenderEffectCanvas(
    shaderSrc: String,
    modifier: Modifier = Modifier,
    inputName: String = "content",
    setUniforms: (RuntimeShader) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val (shader, error) = remember(shaderSrc) {
        try {
            RuntimeShader(shaderSrc) to null
        } catch (t: Throwable) {
            null to (t.message ?: "compile error")
        }
    }
    if (error != null || shader == null) {
        AgslErrorCard(message = error ?: "unknown", modifier = modifier)
        return
    }

    val hasResolution = remember(shaderSrc) { declaresResolution(shaderSrc) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    Box(modifier = modifier) {
        Box(
            Modifier
                .matchParentSize()
                .onSizeChanged {
                    if (it != size) {
                        size = it
                        if (hasResolution) {
                            shader.setFloatUniform("resolution", it.width.toFloat(), it.height.toFloat())
                        }
                    }
                }
                .graphicsLayer {
                    setUniforms(shader)
                    clip = true
                    compositingStrategy = CompositingStrategy.Offscreen
                    // RuntimeShaderEffect snapshots uniform values at construction time
                    // (Skia's SkRuntimeImageFilter holds a frozen builder). Rebuild the
                    // effect whenever the layer block re-runs so fresh uniform values
                    // (radius, time, …) actually reach the GPU.
                    renderEffect = RenderEffect
                        .createRuntimeShaderEffect(shader, inputName)
                        .asComposeRenderEffect()
                },
        ) {
            content()
        }
    }
}
