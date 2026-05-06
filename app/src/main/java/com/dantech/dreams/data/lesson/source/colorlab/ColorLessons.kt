package com.dantech.dreams.data.lesson.source.colorlab

import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonControl
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRegistry
import kotlinx.collections.immutable.persistentListOf

// Iñigo Quílez's cosine palette — six floats describe an entire color ramp.
// We expose the phase shift (`shift`) as the only control to keep the lesson
// focused; A/B/C/D are baked-in tasteful defaults so the output reads nicely
// without expert tuning.
object CosinePalette {
    val id = "color-01-cosine-palette"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float shift;
        half4 main(float2 fragCoord) {
            float t = fragCoord.x / resolution.x;
            float3 a = float3(0.50, 0.50, 0.50);
            float3 b = float3(0.50, 0.50, 0.50);
            float3 c = float3(1.00, 1.00, 1.00);
            float3 d = float3(0.00, 0.33, 0.67);
            float3 col = a + b * cos(6.2831 * (c * t + d + shift));
            return half4(half3(col), 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "IQ Cosine Palette", category = LessonCategory.COLOR, complexity = 2,
                conceptIntro = "a + b·cos(2π·(c·t + d)) maps a 1D parameter to a smooth color ramp. Six floats define the entire palette.",
                agslSource = SOURCE,
                controls = persistentListOf(LessonControl.FloatRange("Phase", "shift", 0f, 1f, 0f)),
                screenRecordingHint = "Sweep phase 0 → 1 — ramp glides through the whole spectrum.",
            )
        )
    }
}

object HsvWheel {
    val id = "color-02-hsv-wheel"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float saturation;
        // Standard hsv2rgb — k = mod(n + h*6, 6), c = v - v*s*max(min(k,4-k,1),0).
        float3 hsv2rgb(float h, float s, float v) {
            float3 k = mod(float3(5.0, 3.0, 1.0) + h * 6.0, 6.0);
            return v - v * s * max(min(min(k, 4.0 - k), 1.0), 0.0);
        }
        half4 main(float2 fragCoord) {
            float2 uv = (fragCoord - 0.5 * resolution) / min(resolution.x, resolution.y);
            float r = length(uv) * 2.0;
            float a = atan(uv.y, uv.x) / 6.2831 + 0.5;
            float mask = 1.0 - smoothstep(0.95, 1.0, r);
            float3 col = hsv2rgb(a, saturation * clamp(r, 0.0, 1.0), 1.0) * mask;
            return half4(half3(col), 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "HSV Wheel", category = LessonCategory.COLOR, complexity = 3,
                conceptIntro = "Polar coords give hue (angle) and saturation (radius). hsv2rgb without branches is a four-line piecewise.",
                agslSource = SOURCE,
                controls = persistentListOf(LessonControl.FloatRange("Saturation", "saturation", 0f, 1f, 0.95f)),
            )
        )
    }
}

object GradientStops {
    val id = "color-03-gradient-stops"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float bias;
        // Three-stop gradient: blend across two segments, biased by a power curve.
        // Demonstrates how a non-linear remap of t reshapes a perceptually uniform ramp.
        half4 main(float2 fragCoord) {
            float t = fragCoord.x / resolution.x;
            float p = pow(t, mix(0.4, 2.5, bias));
            float3 a = float3(0.10, 0.05, 0.30);
            float3 b = float3(0.95, 0.30, 0.55);
            float3 c = float3(1.00, 0.95, 0.65);
            float3 col = (p < 0.5)
                ? mix(a, b, p * 2.0)
                : mix(b, c, (p - 0.5) * 2.0);
            return half4(half3(col), 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Gradient Stops", category = LessonCategory.COLOR, complexity = 2,
                conceptIntro = "Branch on the parameter for two-segment gradients. Reshape with pow(t, k) to bias toward dark or light.",
                agslSource = SOURCE,
                controls = persistentListOf(LessonControl.FloatRange("Bias", "bias", 0f, 1f, 0.5f)),
            )
        )
    }
}

object AcesToneMap {
    val id = "color-04-aces-tonemap"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float exposure;
        // ACES filmic curve (Narkowicz 2015 approximation).
        float3 aces(float3 x) {
            return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
        }
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            // Synthetic HDR signal — bright sun on cool sky.
            float sun = exp(-30.0 * length(uv - float2(0.7, 0.35)));
            float3 hdr = float3(0.20, 0.30, 0.55) + 8.0 * sun * float3(1.0, 0.85, 0.55);
            float3 mapped = aces(hdr * exposure);
            return half4(half3(mapped), 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "ACES Tone Map", category = LessonCategory.COLOR, complexity = 3,
                conceptIntro = "HDR colors above 1 must be tone-mapped before display. ACES gives a filmic shoulder/toe in 8 ALU ops.",
                agslSource = SOURCE,
                controls = persistentListOf(LessonControl.FloatRange("Exposure", "exposure", 0.1f, 4f, 1f)),
                screenRecordingHint = "Sweep exposure 0.2 → 3 — sun saturates without ever clipping pure white.",
            )
        )
    }
}
