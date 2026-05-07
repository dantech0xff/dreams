package com.dantech.dreams.data.lesson.source.showcase

/**
 * Created by dan on 7/5/26
 *
 * Copyright © 2026 Dan Tech. All rights reserved.
 */

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.dantech.dreams.core.agsl.rememberShaderTime
import kotlin.random.Random

private const val STAR_SEED = 12345L
private val WARM_TINT = Color(0xFFFFE9C4)
private const val WARM_TINT_PROBABILITY = 0.06f

private data class Star(
    val xNorm: Float,        // 0..1, mapped to width at draw time
    val yNorm0: Float,       // 0..1, initial y before drift
    val radius: Float,       // px
    val alpha: Float,
    val color: Color,
    val speedPxPerSec: Float,
)

private data class StarLayer(
    val count: Int,
    val minRadius: Float,
    val maxRadius: Float,
    val minAlpha: Float,
    val maxAlpha: Float,
    val speedPxPerSec: Float,
)

// Three depth layers — far/mid/near. Slower + dimmer + smaller reads as
// further away. ~100 stars total, generated once with a fixed seed so the
// constellation is stable across recompositions and config changes.
private val STAR_LAYERS = listOf(
    StarLayer(50, 0.5f, 1.2f, 0.30f, 0.55f, 8f),
    StarLayer(35, 1.0f, 1.8f, 0.55f, 0.80f, 15f),
    StarLayer(15, 1.5f, 2.6f, 0.80f, 1.00f, 24f),
)

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

private fun generateStars(): List<Star> {
    val rand = Random(STAR_SEED)
    val out = ArrayList<Star>(STAR_LAYERS.sumOf { it.count })
    for (layer in STAR_LAYERS) {
        repeat(layer.count) {
            val warm = rand.nextFloat() < WARM_TINT_PROBABILITY
            out += Star(
                xNorm = rand.nextFloat(),
                yNorm0 = rand.nextFloat(),
                radius = lerp(layer.minRadius, layer.maxRadius, rand.nextFloat()),
                alpha = lerp(layer.minAlpha, layer.maxAlpha, rand.nextFloat()),
                color = if (warm) WARM_TINT else Color.White,
                speedPxPerSec = layer.speedPxPerSec,
            )
        }
    }
    return out
}

// Continuous upward parallax drift — no twinkle, stars are static points of
// light. Uses rememberShaderTime as the monotonic clock so positions never
// snap on a phase wrap; per-frame y is the initial y minus drift, wrapped
// modulo height. Reading `time` inside Canvas's draw lambda invalidates the
// draw layer without recomposition (stars list is allocation-stable).
@Composable
internal fun Starfield(modifier: Modifier = Modifier) {
    val time by rememberShaderTime()
    val stars = remember { generateStars() }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        for (s in stars) {
            val baseY = s.yNorm0 * h
            val rawY = baseY - s.speedPxPerSec * time
            // Wrap into [0, h). `((x % h) + h) % h` covers negatives after drift.
            val y = ((rawY % h) + h) % h
            drawCircle(
                color = s.color.copy(alpha = s.alpha),
                radius = s.radius,
                center = Offset(s.xNorm * w, y),
            )
        }
    }
}
