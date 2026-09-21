package com.dantech.dreams.data.lesson.source.showcase

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dantech.dreams.core.agsl.rememberRuntimeShader
import com.dantech.dreams.core.agsl.rememberShaderTime
import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRegistry
import com.dantech.dreams.data.lesson.LessonRenderMode
import com.dantech.dreams.data.lesson.source.noise.NOISE_HELPERS
import kotlin.math.exp

private const val FOCUS_MIN_X = 0.30f
private const val FOCUS_MAX_X = 0.70f
private const val FOCUS_MIN_Y = 0.22f
private const val FOCUS_MAX_Y = 0.68f
private const val FOCUS_LERP = 0.10f

// Hyperspace tunnel. Stars live on (sector, ring) cells in a log-polar domain:
// a level-set of the ring phase moves outward at constant visual speed, and
// pow(phase, k) shapes each star into a comet streak aimed at the rim.
// touchPos steers the tunnel's focal point; touchTime feeds a speed boost
// that decays after the finger lifts.
private const val WARP_VOYAGE_SRC = """
uniform float2 resolution;
uniform float time;
uniform float2 touchPos;
uniform float touchTime;
uniform float warpPhase;
$NOISE_HELPERS

half4 main(float2 fragCoord) {
    float2 focus = (touchPos.x < 0.0) ? float2(0.5, 0.44) : touchPos;
    float2 uv = (fragCoord - focus * resolution) / resolution.y;
    float r = length(uv) + 1e-4;
    float a = atan(uv.y, uv.x);

    float age = time - touchTime;
    float boost = 1.0 + ((touchTime < 0.0) ? 0.0 : 1.8 * exp(-max(age, 0.0) * 1.4));

    // Nebula bed: two domain-warped fbm layers, indigo to teal.
    float2 np = uv * 1.6;
    float2 q = float2(fbm(np + float2(0.0, time * 0.020)),
                      fbm(np + float2(4.7, 1.3) - time * 0.016));
    float neb = fbm(np + 2.4 * q + float2(0.0, time * 0.035));
    half3 col = mix(half3(0.010, 0.014, 0.050), half3(0.17, 0.07, 0.36), half(smoothstep(0.28, 0.85, neb)));
    col = mix(col, half3(0.05, 0.34, 0.42), half(smoothstep(0.62, 0.95, fbm(np * 1.7 - q))) * 0.55);

    // Three parallax star layers. `spiral` folds the angle by log(r) so
    // streaks wind slightly; z's log term makes rings expand outward.
    float3 stars = float3(0.0);
    for (int L = 0; L < 3; L++) {
        float fL = float(L);
        float spiral = a / 6.2831853 + log(r) * (0.24 + fL * 0.07) + fL * 0.37;
        float sector = floor(spiral * (52.0 + fL * 26.0));
        // warpPhase is integrated on the Compose side (dt * boost per frame),
        // so drags accelerate the tunnel without teleporting the star field.
        float z = log(r) * (2.6 + fL * 0.4) - warpPhase * (0.55 + fL * 0.30) + fL * 9.1;
        float ring = floor(z);
        float s = fract(z);
        float h = hash21(float2(sector + fL * 131.0, ring - fL * 57.0));
        float on = step(0.90 - fL * 0.02, h);
        float streak = pow(s, 7.0) * (0.55 + h);
        // Keep the core clear and fade the far rim.
        float gate = smoothstep(0.015, 0.20, r) * smoothstep(3.2, 1.1, r);
        float3 tint = 0.60 + 0.40 * cos(6.2831 * (h + fL * 0.19 + float3(0.0, 0.25, 0.55)));
        stars += tint * (on * streak * gate);
    }
    col += half3(stars) * half(0.9 + 0.5 * (boost - 1.0));

    // Hyperspace core glow + gentle vignette.
    col += half3(0.62, 0.85, 1.0) * half(exp(-r * 3.2) * (0.50 + 0.30 * boost));
    col *= half(1.0 - 0.35 * smoothstep(1.4, 2.6, r));
    return half4(col, 1.0);
}
"""

@Composable
fun WarpVoyageDemo() {
    val shader = rememberRuntimeShader(WARP_VOYAGE_SRC)
    val brush = remember(shader) { ShaderBrush(shader) }
    val time by rememberShaderTime()

    // Plain FloatArrays instead of MutableState: the draw block already runs
    // every frame (it reads `time`), so smoothing can live on the draw path
    // without scheduling extra invalidations.
    val target = remember { floatArrayOf(0.5f, 0.44f) }
    val focus = remember { floatArrayOf(0.5f, 0.44f) }
    val lastSteer = remember { floatArrayOf(-1f) }
    val warpPhase = remember { floatArrayOf(0f) }
    val prevTime = remember { floatArrayOf(-1f) }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    fun steer(change: PointerInputChange) {
                        target[0] = (change.position.x / size.width).coerceIn(FOCUS_MIN_X, FOCUS_MAX_X)
                        target[1] = (change.position.y / size.height).coerceIn(FOCUS_MIN_Y, FOCUS_MAX_Y)
                        lastSteer[0] = time
                    }
                    steer(awaitFirstDown(requireUnconsumed = false))
                    var pressed = true
                    while (pressed) {
                        val event = awaitPointerEvent()
                        pressed = false
                        for (change in event.changes) {
                            if (change.pressed) {
                                pressed = true
                                if (change.positionChanged()) steer(change)
                            }
                        }
                    }
                }
            }
            .drawBehind {
                focus[0] += (target[0] - focus[0]) * FOCUS_LERP
                focus[1] += (target[1] - focus[1]) * FOCUS_LERP
                val dt = if (prevTime[0] < 0f) 0f else (time - prevTime[0]).coerceIn(0f, 0.1f)
                prevTime[0] = time
                val boost =
                    if (lastSteer[0] < 0f) 1f
                    else 1f + 1.8f * exp(-(time - lastSteer[0]).coerceAtLeast(0f) * 1.4f)
                warpPhase[0] += dt * boost
                shader.setFloatUniform("resolution", size.width, size.height)
                shader.setFloatUniform("time", time)
                shader.setFloatUniform("touchPos", focus[0], focus[1])
                shader.setFloatUniform("touchTime", lastSteer[0])
                shader.setFloatUniform("warpPhase", warpPhase[0])
                drawRect(brush)
            },
    ) {
        Text(
            text = "WARP VOYAGE",
            color = Color.White.copy(alpha = 0.75f),
            fontWeight = FontWeight.Light,
            fontSize = 13.sp,
            letterSpacing = 8.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 18.dp),
        )
        Text(
            text = "DRAG TO STEER",
            color = Color.White.copy(alpha = 0.50f),
            fontWeight = FontWeight.Light,
            fontSize = 10.sp,
            letterSpacing = 5.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 110.dp),
        )
    }
}

object WarpVoyage {
    val id = "showcase-07-warp-voyage"

    init {
        LessonRegistry.register(
            LessonModel(
                id = id,
                title = "Warp Voyage",
                category = LessonCategory.SHOWCASE,
                complexity = 5,
                conceptIntro = "A steerable hyperspace tunnel: log-polar star cells become radial comet streaks over a domain-warped fbm nebula, with a decaying speed boost after every drag.",
                agslSource = WARP_VOYAGE_SRC.trimIndent(),
                renderMode = LessonRenderMode.CUSTOM,
                customPreview = { WarpVoyageDemo() },
                screenRecordingHint = "Drag slowly in a circle — the tunnel bends and surges behind your finger.",
            )
        )
    }
}
