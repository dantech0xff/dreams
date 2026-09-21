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

private const val FOCUS_LERP = 0.12f

// Liquid chrome. A domain-warped fbm field is treated as a height map: its
// numeric gradient becomes a surface normal, and a cosine palette read off the
// reflected ray turns the surface into banded, iridescent metal. Dragging
// injects a tangential stir around the fingertip that decays over ~1 s.
private const val CHROME_FLOW_SRC = """
uniform float2 resolution;
uniform float time;
uniform float2 touchPos;
uniform float touchTime;
$NOISE_HELPERS

half4 main(float2 fragCoord) {
    float2 uv = (fragCoord - 0.5 * resolution) / resolution.y;
    float2 p = uv * 2.3;

    // Stir: a vortex impulse around the last touch position. Freshness comes
    // from touchTime — while the finger moves the stir stays at full strength,
    // after release it ebbs away.
    if (touchPos.x >= 0.0) {
        float2 tp = (touchPos - 0.5) * float2(resolution.x / resolution.y, 1.0);
        float2 d = uv - tp;
        float r = length(d) + 1e-3;
        float stir = 1.7 * exp(-max(time - touchTime, 0.0) * 0.9) * exp(-r * 2.0);
        p += (float2(-d.y, d.x) + d * 0.4) / r * stir * 0.35;
    }

    // Domain-warped height field: fbm(fbm-warped fbm) gives folded, molten bands.
    float drift = time * 0.10;
    float2 q = float2(fbm(p + drift), fbm(p + float2(5.2, 1.3) - drift * 0.8));
    float2 w = p + 2.6 * q + float2(0.0, time * 0.05);
    float h = fbm(w);

    // Height → normal via two extra taps of the same warped field.
    float e = 0.014;
    float hx = fbm(w + float2(e, 0.0)) - h;
    float hy = fbm(w + float2(0.0, e)) - h;
    float3 n = normalize(float3(-hx / e * 1.15, -hy / e * 1.15, 1.0));

    // Chrome look: an environment read off the reflected ray — alternating
    // dark/silver bands with an iridescent fringe where bands transition.
    float3 refl = reflect(float3(0.0, 0.0, -1.0), n);
    float bandCoord = refl.y * 7.0 + refl.x * 1.2 + h * 2.2 + time * 0.06;
    float stripe = 0.5 + 0.5 * sin(bandCoord);
    float3 metal = mix(float3(0.03, 0.04, 0.10), float3(0.88, 0.94, 1.05), pow(stripe, 1.6));
    float3 fringe = 0.5 + 0.5 * cos(6.2831 * (bandCoord * 0.13 + float3(0.0, 0.33, 0.67)));
    metal += fringe * stripe * (1.0 - stripe) * 1.3;

    half3 col = half3(0.018, 0.018, 0.028);
    col += half3(metal) * half(0.30 + 0.70 * smoothstep(0.15, 0.90, h));

    // Sun glint + cool fresnel sheen on steep slopes.
    float3 l = normalize(float3(0.4, -0.6, 0.7));
    float spec = pow(max(dot(refl, l), 0.0), 24.0);
    col += half3(1.0, 0.95, 0.85) * half(spec * 0.9);
    float fres = pow(1.0 - max(n.z, 0.0), 3.0);
    col += half3(0.35, 0.55, 0.90) * half(fres * 0.45);

    col *= half(1.0 - 0.40 * smoothstep(0.8, 1.9, length(uv)));
    return half4(col, 1.0);
}
"""

@Composable
fun ChromeFlowDemo() {
    val shader = rememberRuntimeShader(CHROME_FLOW_SRC)
    val brush = remember(shader) { ShaderBrush(shader) }
    val time by rememberShaderTime()

    // See WarpVoyageDemo: FloatArrays mutate on the per-frame draw path, no
    // snapshot state churn needed for the smoothed stir point.
    val target = remember { floatArrayOf(0.5f, 0.5f) }
    val stir = remember { floatArrayOf(0.5f, 0.5f) }
    val lastStir = remember { floatArrayOf(-1f) }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    fun stirAt(change: PointerInputChange) {
                        target[0] = change.position.x / size.width
                        target[1] = change.position.y / size.height
                        lastStir[0] = time
                    }
                    stirAt(awaitFirstDown(requireUnconsumed = false))
                    var pressed = true
                    while (pressed) {
                        val event = awaitPointerEvent()
                        pressed = false
                        for (change in event.changes) {
                            if (change.pressed) {
                                pressed = true
                                if (change.positionChanged()) stirAt(change)
                            }
                        }
                    }
                }
            }
            .drawBehind {
                stir[0] += (target[0] - stir[0]) * FOCUS_LERP
                stir[1] += (target[1] - stir[1]) * FOCUS_LERP
                shader.setFloatUniform("resolution", size.width, size.height)
                shader.setFloatUniform("time", time)
                shader.setFloatUniform("touchPos", stir[0], stir[1])
                shader.setFloatUniform("touchTime", lastStir[0])
                drawRect(brush)
            },
    ) {
        Text(
            text = "LIQUID CHROME",
            color = Color.White.copy(alpha = 0.72f),
            fontWeight = FontWeight.Light,
            fontSize = 13.sp,
            letterSpacing = 8.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 18.dp),
        )
        Text(
            text = "DRAG TO STIR",
            color = Color.White.copy(alpha = 0.48f),
            fontWeight = FontWeight.Light,
            fontSize = 10.sp,
            letterSpacing = 5.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 110.dp),
        )
    }
}

object ChromeFlow {
    val id = "showcase-08-chrome-flow"

    init {
        LessonRegistry.register(
            LessonModel(
                id = id,
                title = "Liquid Chrome",
                category = LessonCategory.SHOWCASE,
                complexity = 5,
                conceptIntro = "Molten metal: a warped fbm height map becomes a normal field, a cosine palette on the reflected ray paints iridescent bands, and drags stir the surface like mercury.",
                agslSource = CHROME_FLOW_SRC.trimIndent(),
                renderMode = LessonRenderMode.CUSTOM,
                customPreview = { ChromeFlowDemo() },
                screenRecordingHint = "Slow figure-eight drags leave swirling wakes in the metal.",
            )
        )
    }
}
