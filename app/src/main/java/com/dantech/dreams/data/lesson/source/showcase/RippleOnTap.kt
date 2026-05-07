package com.dantech.dreams.data.lesson.source.showcase

/**
 * Created by dan on 6/5/26
 *
 * Copyright © 2026 Dan Tech. All rights reserved.
 */

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import com.dantech.dreams.core.agsl.rememberRuntimeShader
import com.dantech.dreams.core.agsl.rememberShaderTime
import com.dantech.dreams.core.agsl.runtimeShaderEffect
import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRegistry
import com.dantech.dreams.data.lesson.LessonRenderMode

private const val MAX_RIPPLES = 16
private const val SLOT_FLOATS = 4                    // (x, y, t0, strength)
private const val DRAG_EMIT_INTERVAL_SEC = 0.06f
private const val MIN_STRENGTH = 0.3f
private const val MAX_STRENGTH = 1.5f

// Each ripple contributes a radial displacement to a virtual height field.
// We accumulate three things per pixel:
//   • totalOff    — 2D screen offset for refraction (`content.eval(fragCoord - totalOff)`).
//   • totalGrad   — analytic ∇h of the height field, used to build a 3D surface normal.
//   • totalHeight — signed sum of wave contributions, used for foam at positive crests.
// Lighting then layers on top:
//   • Chromatic refraction → R/B channels take slightly different offsets (prism).
//   • Caustics      → brighten the underlying floor where the surface focuses the sun.
//   • Blinn–Phong specular from a fixed sun → bright pinpoint glints on wave slopes.
//   • Schlick Fresnel → sky-blue tint at glancing wave faces (water reflectivity).
//   • Foam line     → thin neutral-white band at the highest crests.
private const val RIPPLE_TAP_SRC = """
// RENDER_EFFECT-style shader: the surface beneath is a real Compose subtree
// (see RippleBackdrop). We bind it as `uniform shader content;` and the
// chromatic refraction samples three slightly-offset coordinates from it.
//
// Earlier versions of this lesson generated the backdrop procedurally in-shader
// because attempts at content.eval() came back blank. Root cause turned out to
// be the missing `compositingStrategy = Offscreen` on the wrapping
// graphicsLayer — without an offscreen buffer there's nothing for the runtime
// shader effect to sample. `Modifier.runtimeShaderEffect` (ShaderModifiers.kt)
// sets that strategy and is what we use here.
//
// SLOTS deliberately not named N — local `float3 N = normalize(...)` later would
// shadow it. SkSL accepts the shadow but it's a code smell.
const int SLOTS = $MAX_RIPPLES;

uniform shader content;
uniform float2 iResolution;
uniform float  iTime;
uniform float  rip[${MAX_RIPPLES * SLOT_FLOATS}];

// Clamp sample coords to the layer bounds — at high ripple amplitudes near the
// edges, totalOff can pull samples past the offscreen buffer. The default
// out-of-bounds result is transparent black, which would show as dark fringes.
half3 sampleSurface(float2 coord) {
    float2 c = clamp(coord, float2(0.0), iResolution - float2(1.0));
    return content.eval(c).rgb;
}

half4 main(float2 fragCoord) {
    float speed = 360.0;
    float k     = 0.04;
    float W     = 30.0;
    float amp   = 18.0;
    float decay = 1.3;

    float2 totalOff    = float2(0.0);
    float2 totalGrad   = float2(0.0);
    float  totalHeight = 0.0;

    for (int i = 0; i < SLOTS; i++) {
        // AGSL requires `rip[]` indices to be a constant-multiple of the loop variable
        // plus a constant — pre-computing `int b = i * 4;` and using `rip[b]` is rejected.
        float rx = rip[i * 4];
        float ry = rip[i * 4 + 1];
        float t0 = rip[i * 4 + 2];
        float st = rip[i * 4 + 3];

        float  elapsed = iTime - t0;
        float2 toC     = fragCoord - float2(rx, ry);
        float  dist    = length(toC);
        float2 dir     = dist > 0.001 ? toC / dist : float2(0.0);

        float front     = elapsed * speed;
        float frontEnv  = 1.0 - smoothstep(front - W, front + W, dist);
        float timeEnv   = exp(-elapsed * decay);
        float spaceEnv  = 1.0 / sqrt(max(dist, 16.0)) * 7.0;
        float env       = frontEnv * timeEnv * spaceEnv;

        float kEff  = k / pow(max(st, 0.1), 0.4);
        float phase = kEff * dist - speed * kEff * elapsed;
        float s     = sin(phase);
        float c     = cos(phase);

        float wave   = s * env * amp * st;
        totalOff    += dir * wave;
        totalHeight += wave;

        // Carrier-derivative term dominates the height gradient near each crest;
        // we drop the (denv/dd) envelope-derivative term for cleaner highlights.
        float dWave  = c * kEff * env * amp * st;
        totalGrad   += dir * dWave;
    }

    // Ambient micro-waves: 3-octave procedural plane-wave field. Cheap (3 sin + 3 cos).
    // Higher spatial frequency than user ripples → frequent small glints across the
    // surface as the pattern drifts. Each octave moves in a different direction at a
    // different speed so the pattern never repeats. Feeds totalOff (small refraction
    // shift, gives chromatic refraction something to pick up) and totalGrad (drives
    // normal-based lighting → constant sparkle), but NOT totalHeight — foam stays gated
    // on real wave interference.
    float2 k1 = float2( 0.048,  0.034);
    float2 k2 = float2(-0.030,  0.058);
    float2 k3 = float2( 0.018, -0.072);
    float p1 = dot(fragCoord, k1) - iTime * 0.55;
    float p2 = dot(fragCoord, k2) - iTime * 0.85;
    float p3 = dot(fragCoord, k3) - iTime * 1.10;
    float2 ambGrad = 0.9 * cos(p1) * k1
                   + 0.7 * cos(p2) * k2
                   + 0.5 * cos(p3) * k3;
    totalOff  += ambGrad * 18.0;
    totalGrad += ambGrad * 4.5;

    // Build a 3D surface normal from the accumulated gradient. The 0.6 scale tames
    // very steep waves so the normal stays close to (0,0,1) on calm water.
    float3 N = normalize(float3(-totalGrad * 0.6, 1.0));
    float3 V = float3(0.0, 0.0, 1.0);
    float3 L = normalize(float3(0.55, -0.55, 0.55));   // sun from upper-left, low-ish
    float3 H = normalize(L + V);

    float NdotH   = max(dot(N, H), 0.0);
    float NdotV   = max(dot(N, V), 0.0);
    float NdotL   = max(dot(N, L), 0.0);

    // Anisotropic specular mask: real water glints stretch along wave crests
    // (the surface is smooth along a crest line, rough across it). The crest
    // tangent T is perpendicular to the height gradient. When H aligns with T
    // the highlight is suppressed; when H is perpendicular to T (i.e. aligned
    // with the gradient direction) the highlight is full. Multiplied with the
    // tight isotropic Phong term, this turns each round glint into a 1D streak
    // running along the wave crest.
    float2 g      = totalGrad;
    float  gLen   = length(g);
    float2 crestT = gLen > 0.001 ? float2(-g.y, g.x) / gLen : float2(1.0, 0.0);
    float  TdotH  = dot(crestT, H.xy);
    float  sinTH  = sqrt(max(1.0 - TdotH * TdotH, 0.0));
    float  aniso  = pow(sinTH, 24.0);

    float spec    = pow(NdotH, 96.0) * aniso * 2.0;     // streaked glints along crests
    float fresnel = pow(1.0 - NdotV, 5.0);              // Schlick approximation

    // Chromatic refraction — R bends a touch more than B, like a prism. On flat water
    // totalOff = 0 so all three samples coincide and the effect vanishes; magnitude
    // scales naturally with wave strength. Now sampling the live Compose surface
    // bound as `content` instead of a procedural pattern.
    half3 sampleR = sampleSurface(fragCoord - totalOff * 1.05);
    half3 sampleG = sampleSurface(fragCoord - totalOff);
    half3 sampleB = sampleSurface(fragCoord - totalOff * 0.95);
    half3 baseRgb = half3(sampleR.r, sampleG.g, sampleB.b);

    // Fake caustics — surface that tilts toward the sun focuses light onto the floor
    // below, brightening the underlying texture. pow(., 10) keeps it confined to the
    // sun-facing slopes; on calm water NdotL is constant so there's no directional bias.
    float caustic = pow(NdotL, 10.0) * 0.85;
    baseRgb *= half(1.0 + caustic);

    // Sky reflection at glancing wave faces — fades the underlying surface toward
    // a cool sky tint where the normal turns away from the camera.
    half3 sky = half3(0.55, 0.78, 1.0);
    half3 col = mix(baseRgb, sky, half(fresnel * 0.55));

    // Specular sun on top.
    col += half3(spec);

    // Foam: thin bright fringe along the highest crests of the summed wave field.
    float foamAmt = smoothstep(amp * 0.85, amp * 1.20, totalHeight);
    col += half3(foamAmt * 0.7);

    // Backdrop is fully procedural and opaque, so output is always opaque.
    return half4(col, 1.0);
}
"""

private fun pressureToStrength(pressure: Float): Float =
    (pressure * 1.2f).coerceIn(MIN_STRENGTH, MAX_STRENGTH)

@Composable
fun RippleTapDemo() {
    val shader = rememberRuntimeShader(RIPPLE_TAP_SRC)
    val time by rememberShaderTime()
    val ripples = remember {
        FloatArray(MAX_RIPPLES * SLOT_FLOATS).also { arr ->
            for (i in 0 until MAX_RIPPLES) {
                arr[i * SLOT_FLOATS + 2] = -1000f      // t0 — long-decayed
                arr[i * SLOT_FLOATS + 3] = 0f          // strength = 0 → contributes nothing
            }
        }
    }
    val slot = remember { mutableIntStateOf(0) }

    fun emit(pos: Offset, t: Float, strength: Float) {
        val s = slot.intValue
        val base = s * SLOT_FLOATS
        ripples[base] = pos.x
        ripples[base + 1] = pos.y
        ripples[base + 2] = t
        ripples[base + 3] = strength
        slot.intValue = (s + 1) % MAX_RIPPLES
    }

    // RENDER_EFFECT pattern via Modifier.runtimeShaderEffect:
    //   • pointerInput collects taps/drags into the `rip[]` ring buffer.
    //   • runtimeShaderEffect wraps the children in a graphicsLayer with
    //     compositingStrategy = Offscreen and binds the shader as a RenderEffect.
    //     Children (RippleBackdrop) are rasterized into the offscreen buffer; the
    //     shader's `uniform shader content` reads from it via content.eval(coord).
    //   • The onFrame block re-runs every frame thanks to the `time` snapshot
    //     read — that re-creates the runtime shader effect with current uniform
    //     values (Skia's RuntimeShaderEffect freezes uniforms at construction,
    //     so we must rebuild it per frame).
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Multi-touch: each finger has its own throttled emit timer in
                // `lastEmits`. New fingers that touch down during an existing gesture
                // get an immediate emit + map entry; lifted fingers are removed but
                // the gesture loop keeps running until ALL fingers are up.
                awaitEachGesture {
                    val lastEmits = HashMap<PointerId, Float>()
                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                    emit(firstDown.position, time, pressureToStrength(firstDown.pressure))
                    lastEmits[firstDown.id] = time

                    while (lastEmits.isNotEmpty()) {
                        val event = awaitPointerEvent()
                        val now = time
                        for (change in event.changes) {
                            val id = change.id
                            if (change.pressed) {
                                val last = lastEmits[id]
                                if (last == null) {
                                    emit(change.position, now, pressureToStrength(change.pressure))
                                    lastEmits[id] = now
                                } else if (change.positionChanged() &&
                                    now - last >= DRAG_EMIT_INTERVAL_SEC
                                ) {
                                    emit(change.position, now, pressureToStrength(change.pressure))
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
                shader.setFloatUniform("iTime", time)
                shader.setFloatUniform("rip", ripples)
            },
    ) {
        RippleBackdrop(Modifier.fillMaxSize())
    }
}

object RippleOnTap {
    val id = "showcase-05-ripple-on-tap"

    init {
        LessonRegistry.register(
            LessonModel(
                id = id,
                title = "Ripple on Tap",
                category = LessonCategory.SHOWCASE,
                complexity = 5,
                conceptIntro = "Multi-touch + drag emit into a 16-slot ripple ring buffer that distorts a live Compose surface (gradient + bokeh + typography). Anisotropic specular streaks along crests, chromatic refraction (R bends more than B), Schlick fresnel sky-tint, foam fringe at peaks.",
                agslSource = RIPPLE_TAP_SRC.trimIndent(),
                renderMode = LessonRenderMode.CUSTOM,
                customPreview = { RippleTapDemo() },
                screenRecordingHint = "Drag with two fingers for streak interference. Record portrait — the prism + foam reads instantly.",
            )
        )
    }
}
