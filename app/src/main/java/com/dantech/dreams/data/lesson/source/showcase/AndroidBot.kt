package com.dantech.dreams.data.lesson.source.showcase

/**
 * Created by dan on 6/5/26
 *
 * Copyright © 2026 Dan Tech. All rights reserved.
 */

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val ANDROID_GREEN = 0xFF3DDC84L

// Idle saccade timing — boring-person glances every 1.5–3.5s.
private const val IDLE_HOLD_MIN_MS = 1500
private const val IDLE_HOLD_RAND_MS = 2000

// Blink — quick close/open every 3–6s when not engaged.
private const val BLINK_HOLD_MIN_MS = 3000
private const val BLINK_HOLD_RAND_MS = 3000
private const val BLINK_CLOSE_MS = 80
private const val BLINK_OPEN_MS = 120

// Gaze spring profiles — stiff when locked onto a finger, lazy when drifting.
private const val GAZE_STIFFNESS_FOCUSED = 1500f
private const val GAZE_STIFFNESS_IDLE = 200f
private const val GAZE_DAMPING = 0.85f
private const val PUPIL_DILATION_MS = 220

// Hand-drawn Android mascot. Eyes track active touch; idle into casual
// saccades + occasional blinks when no finger is down.
//
// `activeTouch` is in window-root coordinates (the same space the upstream
// `pointerInput` reports positions in). The bot reads its own bounds via
// `onGloballyPositioned` and converts touch → bot-local UV, clamped to a
// unit circle so far-away touches still hit the sclera edge.
@Composable
internal fun AndroidBot(
    activeTouch: State<Offset?>,
    modifier: Modifier = Modifier,
) {
    var botCenter by remember { mutableStateOf(Offset.Zero) }
    var botHalfSize by remember { mutableFloatStateOf(0f) }

    // Idle gaze target — re-rolled at random intervals while no finger is
    // down. Vertical bias × 0.6 keeps glances low-amplitude (boring person).
    var idleTargetX by remember { mutableFloatStateOf(0f) }
    var idleTargetY by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay((IDLE_HOLD_MIN_MS + Random.nextInt(IDLE_HOLD_RAND_MS)).toLong())
            val angle = Random.nextFloat() * (2f * PI.toFloat())
            val r = Random.nextFloat() * 0.55f
            idleTargetX = cos(angle) * r
            idleTargetY = sin(angle) * r * 0.6f
        }
    }

    // Blinks fire only while idle — engaged eyes don't blink.
    val blink = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay((BLINK_HOLD_MIN_MS + Random.nextInt(BLINK_HOLD_RAND_MS)).toLong())
            if (activeTouch.value == null) {
                blink.animateTo(1f, tween(BLINK_CLOSE_MS))
                blink.animateTo(0f, tween(BLINK_OPEN_MS))
            }
        }
    }

    val touch = activeTouch.value
    val isFocused = touch != null
    val (rawX, rawY) = if (touch != null && botHalfSize > 0f) {
        val dx = (touch.x - botCenter.x) / botHalfSize
        val dy = (touch.y - botCenter.y) / botHalfSize
        val mag = sqrt(dx * dx + dy * dy)
        if (mag > 1f) (dx / mag) to (dy / mag) else dx to dy
    } else {
        idleTargetX to idleTargetY
    }

    val stiffness = if (isFocused) GAZE_STIFFNESS_FOCUSED else GAZE_STIFFNESS_IDLE
    val gazeX by animateFloatAsState(
        targetValue = rawX,
        animationSpec = spring(stiffness = stiffness, dampingRatio = GAZE_DAMPING),
        label = "gaze-x",
    )
    val gazeY by animateFloatAsState(
        targetValue = rawY,
        animationSpec = spring(stiffness = stiffness, dampingRatio = GAZE_DAMPING),
        label = "gaze-y",
    )
    val dilation by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(PUPIL_DILATION_MS),
        label = "dilation",
    )

    Canvas(
        modifier = modifier.onGloballyPositioned { coords ->
            val pos = coords.positionInRoot()
            val w = coords.size.width.toFloat()
            val h = coords.size.height.toFloat()
            botCenter = Offset(pos.x + w / 2f, pos.y + h / 2f)
            botHalfSize = minOf(w, h) / 2f
        },
    ) {
        drawAndroidBot(
            color = Color(ANDROID_GREEN),
            gazeX = gazeX,
            gazeY = gazeY,
            blink = blink.value,
            dilation = dilation,
        )
    }
}

// Layout (5u × 6u grid):
//   0u           antennae top
//   0.5u         antennae meet head
//   0.5u → 2.5u  head semicircle (radius 2u, center x=2.5u, flat at y=2.5u)
//   2.55u → 5.05u body (sharp top corners, rounded bottom)
//   2.7u → 4.7u  arm capsules flanking body
//   5.0u → 6.2u  leg capsules
private fun DrawScope.drawAndroidBot(
    color: Color,
    gazeX: Float,
    gazeY: Float,
    blink: Float,
    dilation: Float,
) {
    val w = size.width
    val h = size.height
    // 0.95 multiplier leaves a small margin so antenna tips and leg ends
    // don't kiss the layer edge — keeps the ripple-shader's clamp-on-edge
    // behaviour from clipping body parts.
    val unit = minOf(w / 5f, h / 6.5f) * 0.95f
    val originX = (w - 5f * unit) / 2f
    val originY = (h - 6f * unit) / 2f
    val cx = originX + 2.5f * unit

    // Antennae
    val antennaStroke = unit * 0.12f
    val antennaTopY = originY + 0.05f * unit
    val antennaBotY = originY + 0.5f * unit
    drawLine(
        color = color,
        start = Offset(originX + 1.85f * unit, antennaTopY),
        end = Offset(originX + 1.6f * unit, antennaBotY),
        strokeWidth = antennaStroke,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color,
        start = Offset(originX + 3.15f * unit, antennaTopY),
        end = Offset(originX + 3.4f * unit, antennaBotY),
        strokeWidth = antennaStroke,
        cap = StrokeCap.Round,
    )

    // Head — top semicircle. drawArc(start=180°, sweep=180°, useCenter=true)
    // fills the top half of the inscribed circle.
    val headRadius = 2f * unit
    val headFlatY = originY + 2.5f * unit
    drawArc(
        color = color,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(cx - headRadius, headFlatY - headRadius),
        size = Size(headRadius * 2, headRadius * 2),
    )

    // Body — sharp top corners, rounded bottom corners. Path required because
    // drawRoundRect rounds all four corners equally.
    val bodyTop = headFlatY + 0.05f * unit
    val bodyBottom = bodyTop + 2.5f * unit
    val bodyLeft = originX + 1f * unit
    val bodyRight = originX + 4f * unit
    val bodyCorner = 0.45f * unit
    drawPath(
        color = color,
        path = Path().apply {
            moveTo(bodyLeft, bodyTop)
            lineTo(bodyRight, bodyTop)
            lineTo(bodyRight, bodyBottom - bodyCorner)
            arcTo(
                rect = Rect(
                    bodyRight - bodyCorner * 2, bodyBottom - bodyCorner * 2,
                    bodyRight, bodyBottom,
                ),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false,
            )
            lineTo(bodyLeft + bodyCorner, bodyBottom)
            arcTo(
                rect = Rect(
                    bodyLeft, bodyBottom - bodyCorner * 2,
                    bodyLeft + bodyCorner * 2, bodyBottom,
                ),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false,
            )
            close()
        },
    )

    // Arms — capsule rectangles flanking body.
    val armWidth = 0.7f * unit
    val armHeight = 2f * unit
    val armTop = bodyTop + 0.15f * unit
    val armGap = 0.18f * unit
    val armRadius = CornerRadius(armWidth / 2f)
    drawRoundRect(
        color = color,
        topLeft = Offset(bodyLeft - armGap - armWidth, armTop),
        size = Size(armWidth, armHeight),
        cornerRadius = armRadius,
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(bodyRight + armGap, armTop),
        size = Size(armWidth, armHeight),
        cornerRadius = armRadius,
    )

    // Legs — short capsules below body.
    val legWidth = 0.7f * unit
    val legHeight = 1.2f * unit
    val legTop = bodyBottom - 0.05f * unit
    val legInset = 0.6f * unit
    val legRadius = CornerRadius(legWidth / 2f)
    drawRoundRect(
        color = color,
        topLeft = Offset(bodyLeft + legInset, legTop),
        size = Size(legWidth, legHeight),
        cornerRadius = legRadius,
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(bodyRight - legInset - legWidth, legTop),
        size = Size(legWidth, legHeight),
        cornerRadius = legRadius,
    )

    // Eyes — white sclera + dark pupil that tracks gaze. Sclera + pupil
    // squash vertically together for blinks so the pupil never escapes.
    val sclera = 0.32f * unit
    val pupilBase = sclera * 0.55f
    val pupil = pupilBase * (1f + 0.3f * dilation)
    val maxPupilOff = sclera - pupil
    val eyeOff = 0.95f * unit
    val eyeY = headFlatY - 0.85f * unit
    val scaleY = 1f - 0.92f * blink

    drawEye(cx - eyeOff, eyeY, sclera, pupil, maxPupilOff, gazeX, gazeY, scaleY)
    drawEye(cx + eyeOff, eyeY, sclera, pupil, maxPupilOff, gazeX, gazeY, scaleY)
}

private fun DrawScope.drawEye(
    eyeX: Float,
    eyeY: Float,
    sclera: Float,
    pupil: Float,
    maxPupilOff: Float,
    gazeX: Float,
    gazeY: Float,
    scaleY: Float,
) {
    drawOval(
        color = Color.White,
        topLeft = Offset(eyeX - sclera, eyeY - sclera * scaleY),
        size = Size(sclera * 2f, sclera * 2f * scaleY),
    )
    if (scaleY > 0.18f) {
        drawCircle(
            color = Color(0xFF0E0E12),
            radius = pupil * scaleY,
            center = Offset(
                eyeX + maxPupilOff * gazeX,
                eyeY + maxPupilOff * gazeY * scaleY,
            ),
        )
    }
}
