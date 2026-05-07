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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

private const val TAU = (2.0 * PI).toFloat()
private const val BREATH_PERIOD_MS = 7_000

// The surface RippleOnTap distorts. Deliberately minimal: pure black so
// specular streaks (additive white) and foam pop maximally, with a hand-
// drawn Android mascot at center whose eyes track the user's active touch.
//
// `activeTouch` is plumbed down from RippleTapDemo (window-root coords) so
// the bot can convert touch position → bot-local UV without a redundant
// pointerInput layer that would compete with the ripple gesture loop.
//
// One slow breath loop (7s) drives a low-amplitude alpha pulse on the bot
// and a counter-phase pulse on the caption. Read inside graphicsLayer so
// the layer invalidates without recomposition.
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
        modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AndroidBot(
                activeTouch = activeTouch,
                modifier = Modifier
                    .size(240.dp)
                    .graphicsLayer {
                        alpha = 0.88f + 0.12f * sin(breath.value)
                    },
            )
            Spacer(Modifier.height(28.dp))
            Text(
                text = "TAO TO DISTURB THE SURFACE",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                letterSpacing = 4.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = 0.55f + 0.20f * sin(breath.value + (PI * 0.5f).toFloat())
                },
            )
        }
    }
}
