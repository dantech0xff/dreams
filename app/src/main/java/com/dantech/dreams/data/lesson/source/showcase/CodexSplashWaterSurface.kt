package com.dantech.dreams.data.lesson.source.showcase

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import com.dantech.dreams.core.agsl.rememberRuntimeShader
import com.dantech.dreams.core.agsl.runtimeShaderEffect

private const val CODEX_WATER_MAX_RIPPLES = 16
private const val CODEX_WATER_SLOT_FLOATS = 4
private const val CODEX_WATER_DRAG_EMIT_INTERVAL_SEC = 0.055f
private const val CODEX_WATER_MIN_STRENGTH = 0.45f
private const val CODEX_WATER_MAX_STRENGTH = 1.9f

internal const val CODEX_SPLASH_WATER_SRC = """
const int SLOTS = $CODEX_WATER_MAX_RIPPLES;

uniform shader content;
uniform float2 iResolution;
uniform float iTime;
uniform float rip[${CODEX_WATER_MAX_RIPPLES * CODEX_WATER_SLOT_FLOATS}];

half3 sampleSurface(float2 coord) {
    float2 c = clamp(coord, float2(0.0), iResolution - float2(1.0));
    return content.eval(c).rgb;
}

half4 main(float2 fragCoord) {
    float speed = 410.0;
    float k = 0.046;
    float W = 36.0;
    float amp = 22.0;
    float decay = 1.15;

    float2 totalOff = float2(0.0);
    float2 totalGrad = float2(0.0);
    float totalHeight = 0.0;

    for (int i = 0; i < SLOTS; i++) {
        float rx = rip[i * 4];
        float ry = rip[i * 4 + 1];
        float t0 = rip[i * 4 + 2];
        float st = rip[i * 4 + 3];

        float elapsed = iTime - t0;
        float2 toC = fragCoord - float2(rx, ry);
        float dist = length(toC);
        float2 dir = dist > 0.001 ? toC / dist : float2(0.0);

        float front = elapsed * speed;
        float frontEnv = 1.0 - smoothstep(front - W, front + W, dist);
        float timeEnv = exp(-elapsed * decay);
        float spaceEnv = 1.0 / sqrt(max(dist, 16.0)) * 7.5;
        float env = frontEnv * timeEnv * spaceEnv;

        float kEff = k / pow(max(st, 0.1), 0.35);
        float phase = kEff * dist - speed * kEff * elapsed;
        float wave = sin(phase) * env * amp * st;
        totalOff += dir * wave;
        totalHeight += wave;

        float dWave = cos(phase) * kEff * env * amp * st;
        totalGrad += dir * dWave;
    }

    float3 normal = normalize(float3(-totalGrad * 0.62, 1.0));
    float3 view = float3(0.0, 0.0, 1.0);
    float3 light = normalize(float3(0.48, -0.62, 0.62));
    float3 halfVec = normalize(light + view);

    float ndh = max(dot(normal, halfVec), 0.0);
    float ndv = max(dot(normal, view), 0.0);
    float ndl = max(dot(normal, light), 0.0);

    float2 grad = totalGrad;
    float gradLen = length(grad);
    float2 crestT = gradLen > 0.001 ? float2(-grad.y, grad.x) / gradLen : float2(1.0, 0.0);
    float tdh = dot(crestT, halfVec.xy);
    float aniso = pow(sqrt(max(1.0 - tdh * tdh, 0.0)), 22.0);
    float spec = pow(ndh, 88.0) * aniso * 2.25;
    float fresnel = pow(1.0 - ndv, 5.0);
    float caustic = pow(ndl, 9.0) * 0.78;

    half3 sampleR = sampleSurface(fragCoord - totalOff * 1.10);
    half3 sampleG = sampleSurface(fragCoord - totalOff);
    half3 sampleB = sampleSurface(fragCoord - totalOff * 0.90);
    half3 baseRgb = half3(sampleR.r, sampleG.g, sampleB.b);
    baseRgb *= half(1.0 + caustic);

    half3 sky = half3(0.50, 0.74, 1.0);
    half3 col = mix(baseRgb, sky, half(fresnel * 0.50));
    col += half3(spec);

    float foamAmt = smoothstep(amp * 0.70, amp * 1.15, totalHeight);
    col += half3(foamAmt * 0.58);

    return half4(col, 1.0);
}
"""

private fun codexWaterPressureToStrength(pressure: Float): Float =
    (pressure * 1.25f).coerceIn(CODEX_WATER_MIN_STRENGTH, CODEX_WATER_MAX_STRENGTH)

@Composable
internal fun CodexSplashWaterSurface(
    time: State<Float>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shader = rememberRuntimeShader(CODEX_SPLASH_WATER_SRC)
    val currentTime by rememberUpdatedState(time.value)
    val ripples = remember {
        FloatArray(CODEX_WATER_MAX_RIPPLES * CODEX_WATER_SLOT_FLOATS).also { arr ->
            for (i in 0 until CODEX_WATER_MAX_RIPPLES) {
                arr[i * CODEX_WATER_SLOT_FLOATS + 2] = -1000f
                arr[i * CODEX_WATER_SLOT_FLOATS + 3] = 0f
            }
        }
    }
    val slot = remember { mutableIntStateOf(0) }

    fun emit(pos: Offset, t: Float, strength: Float) {
        val s = slot.intValue
        val base = s * CODEX_WATER_SLOT_FLOATS
        ripples[base] = pos.x
        ripples[base + 1] = pos.y
        ripples[base + 2] = t
        ripples[base + 3] = strength
        slot.intValue = (s + 1) % CODEX_WATER_MAX_RIPPLES
    }

    Box(
        modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    val lastEmits = HashMap<PointerId, Float>()
                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                    emit(firstDown.position, currentTime, codexWaterPressureToStrength(firstDown.pressure))
                    lastEmits[firstDown.id] = currentTime

                    while (lastEmits.isNotEmpty()) {
                        val event = awaitPointerEvent()
                        val now = currentTime
                        for (change in event.changes) {
                            val id = change.id
                            if (change.pressed) {
                                val last = lastEmits[id]
                                if (last == null) {
                                    emit(change.position, now, codexWaterPressureToStrength(change.pressure))
                                    lastEmits[id] = now
                                } else if (change.positionChanged() &&
                                    now - last >= CODEX_WATER_DRAG_EMIT_INTERVAL_SEC
                                ) {
                                    emit(change.position, now, codexWaterPressureToStrength(change.pressure))
                                    lastEmits[id] = now
                                }
                            } else {
                                lastEmits.remove(id)
                            }
                        }
                    }
                }
            }
            .runtimeShaderEffect(shader) { layerSize ->
                shader.setFloatUniform("iResolution", layerSize.width, layerSize.height)
                shader.setFloatUniform("iTime", currentTime)
                shader.setFloatUniform("rip", ripples)
            },
    ) {
        content()
    }
}
