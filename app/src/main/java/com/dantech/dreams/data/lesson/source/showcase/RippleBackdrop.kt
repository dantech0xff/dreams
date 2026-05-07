package com.dantech.dreams.data.lesson.source.showcase

/**
 * Created by dan on 6/5/26
 *
 * Copyright © 2026 Dan Tech. All rights reserved.
 */

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

private const val TAU = (2.0 * PI).toFloat()
private const val BREATH_PERIOD_MS = 7_000

private val BOT_SIZE = 240.dp
private val HALO_SIZE = 320.dp
private val ANDROID_GREEN = Color(0xFF3DDC84)
private val NEBULA_INDIGO = Color(0x14382C7A)  // ~8% deep indigo at corners

// The surface RippleOnTap distorts. Stays mostly black so the shader's
// additive specular streaks (white) and foam pop maximally; chromatic
// refraction picks up high-contrast points (stars) as tiny prism splits
// when ripples pass.
//
// Layers, back to front:
//   1. Black base
//   2. Faint indigo nebula (transparent center → ~8% indigo at corners)
//   3. Parallax starfield (3 depth layers, slow upward drift)
//   4. Additive Android-green halo behind the bot (breath-pulsed)
//   5. AndroidBot — eyes track active touch, alpha breathes
//   6. Wide-tracked caption with counter-phase breath pulse
//
// `activeTouch` is plumbed down from RippleTapDemo (window-root coords) so
// the bot can convert touch position → bot-local UV without a redundant
// pointerInput layer that would compete with the ripple gesture loop.
//
// One slow breath loop (7s) drives alpha pulses on bot/halo/caption.
// All read inside graphicsLayer / drawBehind so the layer invalidates
// without recomposition.
@Composable
internal fun RippleBackdrop(
    activeTouch: State<Offset?>,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "ripple-backdrop")
    val breath = transition.animateFloat(
        initialValue = 0f,
        targetValue = TAU,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = BREATH_PERIOD_MS, easing = LinearEasing),
        ),
        label = "breath",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .drawBehind {
                // Nebula gradient — center stays fully transparent so the bot
                // region preserves max shader contrast; only edges/corners
                // pick up indigo. Static brush; cheap to rebuild per frame.
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1.0f to NEBULA_INDIGO,
                        ),
                        center = center,
                        radius = max(size.width, size.height) * 0.85f,
                    ),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Starfield(Modifier.fillMaxSize())

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(HALO_SIZE)
                    .drawBehind {
                        // Additive radial halo. BlendMode.Plus so stars under
                        // the halo brighten rather than fade. Peak alpha capped
                        // low so halo never competes with shader specular.
                        val haloAlpha = 0.10f + 0.08f * sin(breath.value)
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ANDROID_GREEN.copy(alpha = haloAlpha),
                                    Color.Transparent,
                                ),
                                center = center,
                                radius = size.minDimension / 2f,
                            ),
                            blendMode = BlendMode.Plus,
                        )
                    },
            ) {
                AndroidBot(
                    activeTouch = activeTouch,
                    modifier = Modifier
                        .size(BOT_SIZE)
                        .graphicsLayer {
                            alpha = 0.88f + 0.12f * sin(breath.value)
                        },
                )
            }
            Spacer(Modifier.height(28.dp))
            Text(
                text = "TAP TO DISTURB THE SURFACE",
                color = Color.White,
                fontWeight = FontWeight.Light,
                fontSize = 11.sp,
                letterSpacing = 6.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = 0.55f + 0.20f * sin(breath.value + (PI * 0.5f).toFloat())
                },
            )
        }
    }
}
