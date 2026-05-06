package com.dantech.dreams.data.lesson.source.motion

import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonControl
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRegistry
import kotlinx.collections.immutable.persistentListOf

// Visualizes 4 easings simultaneously — a moving dot per row, plus the curve
// drawn as a faint trail. Lets the user *see* why easeOutCubic feels "snappy"
// and easeInOut "balanced" rather than just trusting names.
object EasingExplorer {
    val id = "motion-01-easing"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float time;
        uniform float speed;

        float easeLinear(float t)    { return t; }
        float easeOutCubic(float t)  { float u = 1.0 - t; return 1.0 - u*u*u; }
        float easeInOutCubic(float t){ return (t < 0.5) ? 4.0*t*t*t : 1.0 - pow(-2.0*t+2.0, 3.0)/2.0; }
        float easeOutBack(float t)   { float c = 1.70158; float u = t - 1.0; return 1.0 + (c+1.0)*u*u*u + c*u*u; }

        float dot2d(float2 p, float2 c, float r) {
            return 1.0 - smoothstep(r, r + 0.005, length(p - c));
        }

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            float t = fract(time * speed * 0.25);
            float rows = 4.0;
            float row = floor(uv.y * rows);
            float e = (row < 1.0) ? easeLinear(t)
                    : (row < 2.0) ? easeOutCubic(t)
                    : (row < 3.0) ? easeInOutCubic(t)
                                  : easeOutBack(t);
            float yBand = (row + 0.5) / rows;
            float a = dot2d(uv, float2(0.05 + e * 0.90, yBand), 0.025);
            half3 col = mix(half3(0.04, 0.06, 0.12), half3(0.30, 0.95, 0.70), half(a));
            // Faint baseline grid between rows.
            float line = step(0.998, fract(uv.y * rows));
            col = mix(col, half3(0.20, 0.30, 0.40), half(line));
            return half4(col, 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Easing Curves", category = LessonCategory.MOTION, complexity = 2,
                conceptIntro = "Easings reshape a 0→1 parameter over time. Compare linear, cubic-out, cubic-in-out, and back-out side by side.",
                agslSource = SOURCE,
                controls = persistentListOf(LessonControl.FloatRange("Speed", "speed", 0.2f, 4f, 1f)),
            )
        )
    }
}

object SineHarmonics {
    val id = "motion-02-harmonics"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float time;
        uniform float harmonic;
        // Plots y = sum_{k=1..H} (1/k) * sin(k * (x + t)) — a band-limited square wave
        // approximation. Sweep harmonic to see Gibbs ringing emerge.
        half4 main(float2 fragCoord) {
            float2 uv = (fragCoord / resolution) * 2.0 - 1.0;
            uv.x *= resolution.x / resolution.y;
            float v = 0.0;
            for (int k = 1; k <= 12; k++) {
                if (float(k) > harmonic) break;
                float kf = float(k);
                v += sin(kf * (uv.x * 3.0 + time)) / kf;
            }
            v *= 0.6;
            float d = abs(uv.y - v);
            float a = 1.0 - smoothstep(0.005, 0.020, d);
            half3 bg = half3(0.05, 0.05, 0.10);
            half3 fg = half3(0.30, 0.95, 0.70);
            return half4(mix(bg, fg, half(a)), 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Sine Harmonics", category = LessonCategory.MOTION, complexity = 3,
                conceptIntro = "Adding 1/k·sin(k·x) terms approximates a square wave. The Gibbs ringing is visible at the discontinuities.",
                agslSource = SOURCE,
                controls = persistentListOf(LessonControl.FloatRange("Harmonics", "harmonic", 1f, 12f, 5f)),
                screenRecordingHint = "Sweep harmonic 1 → 12 — wave sharpens visibly with each step.",
            )
        )
    }
}

object WaveTrain {
    val id = "motion-03-wave-train"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float time;
        uniform float speed;
        // Stack of horizontal sinusoids, each shifted in phase. Reads as wind on
        // a flag, or a sound waveform — the same equation does both.
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            float bands = 18.0;
            float y = uv.y * bands;
            float row = floor(y);
            float local = fract(y) - 0.5;
            float phase = row * 0.35;
            float wave = sin(uv.x * 12.0 + time * speed + phase) * 0.30;
            float d = abs(local - wave);
            float a = 1.0 - smoothstep(0.06, 0.10, d);
            half3 bg = half3(0.04, 0.06, 0.10);
            half3 fg = mix(half3(0.20, 0.95, 0.65), half3(0.30, 0.55, 0.95), half(uv.y));
            return half4(mix(bg, fg, half(a)), 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Wave Train", category = LessonCategory.MOTION, complexity = 2,
                conceptIntro = "Stack N sine rows offset in phase — each row reads the same wave at a slightly later moment.",
                agslSource = SOURCE,
                controls = persistentListOf(LessonControl.FloatRange("Speed", "speed", 0f, 6f, 2f)),
            )
        )
    }
}

object PendulumChain {
    val id = "motion-04-pendulum-chain"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float time;
        uniform float spread;
        // Five oscillators with detuned frequencies — drift apart, momentarily
        // align, drift apart again. The classic pendulum-wave installation.
        half4 main(float2 fragCoord) {
            float2 uv = (fragCoord - 0.5 * resolution) / resolution.y;
            float minD = 1.0;
            for (int i = 0; i < 5; i++) {
                float fi = float(i);
                float f = 1.0 + fi * spread;
                float x = -0.4 + fi * 0.20;
                float y = sin(time * f) * 0.30;
                float d = length(uv - float2(x, y));
                minD = min(minD, d);
            }
            float a = 1.0 - smoothstep(0.025, 0.030, minD);
            half3 bg = half3(0.05, 0.07, 0.12);
            half3 fg = half3(0.95, 0.85, 0.30);
            return half4(mix(bg, fg, half(a)), 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Pendulum Chain", category = LessonCategory.MOTION, complexity = 3,
                conceptIntro = "N oscillators with detuned frequencies fall in and out of phase — a hypnotic 'pendulum wave'.",
                agslSource = SOURCE,
                controls = persistentListOf(LessonControl.FloatRange("Spread", "spread", 0.05f, 0.40f, 0.18f)),
                screenRecordingHint = "Record 30s — eye sees an apparent travelling wave despite each dot moving 1D.",
            )
        )
    }
}
