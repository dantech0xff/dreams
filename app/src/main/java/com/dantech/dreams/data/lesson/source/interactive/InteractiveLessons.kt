package com.dantech.dreams.data.lesson.source.interactive

import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonControl
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRegistry
import kotlinx.collections.immutable.persistentListOf

// Interactive lessons receive two extra uniforms set by the lesson runtime
// when the user taps the preview:
//   uniform float2 touchPos;  // normalized [0..1], (-1,-1) before any tap
//   uniform float  touchTime; // time-of-tap in seconds (matches `time`), -1 before any tap
// All four lessons here degrade gracefully when no tap has happened yet.

object PointerSpotlight {
    val id = "interactive-01-spotlight"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float2 touchPos;
        uniform float  touchTime;
        uniform float radius;
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            // Fall back to screen center until first tap.
            float2 center = (touchPos.x < 0.0) ? float2(0.5) : touchPos;
            float aspect = resolution.x / resolution.y;
            float2 d = uv - center;
            d.x *= aspect;
            float r = length(d);
            float fall = 1.0 - smoothstep(radius * 0.6, radius, r);
            half3 dark = half3(0.02, 0.03, 0.08);
            half3 lit  = half3(0.95, 0.85, 0.55);
            return half4(mix(dark, lit, half(fall)), 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Spotlight", category = LessonCategory.INTERACTIVE, complexity = 2,
                conceptIntro = "touchPos lands as a uniform in 0..1 UV space. Distance + smoothstep gives a soft pool of light at the cursor.",
                agslSource = SOURCE,
                controls = persistentListOf(LessonControl.FloatRange("Radius", "radius", 0.10f, 0.80f, 0.35f)),
                screenRecordingHint = "Tap-and-drag is shown as discrete taps — each tap re-anchors the spotlight.",
            )
        )
    }
}

object PointerRipple {
    val id = "interactive-02-ripple"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float  time;
        uniform float2 touchPos;
        uniform float  touchTime;
        uniform float speed;
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            // Bail out cleanly until we have a tap.
            if (touchPos.x < 0.0) return half4(0.04, 0.05, 0.10, 1.0);
            float aspect = resolution.x / resolution.y;
            float2 d = uv - touchPos;
            d.x *= aspect;
            float r = length(d);
            float age = max(time - touchTime, 0.0);
            // Single expanding ring that fades and softens with age.
            float ringRadius = age * speed * 0.30;
            float w = 0.020 + age * 0.040;
            float band = exp(-pow((r - ringRadius) / w, 2.0));
            float fade = exp(-age * 1.2);
            half3 bg = half3(0.04, 0.05, 0.10);
            half3 fg = half3(0.30, 0.85, 1.00);
            return half4(mix(bg, fg, half(band * fade)), 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Tap Ripple", category = LessonCategory.INTERACTIVE, complexity = 3,
                conceptIntro = "(time - touchTime) drives a Gaussian band whose radius grows. Re-tap restarts the wave.",
                agslSource = SOURCE,
                controls = persistentListOf(LessonControl.FloatRange("Speed", "speed", 0.5f, 4f, 1.6f)),
            )
        )
    }
}

object PointerPullField {
    val id = "interactive-03-pull-field"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float2 touchPos;
        uniform float  touchTime;
        uniform float strength;
        // A static checker whose UVs are bent toward the touch point —
        // visualises a "gravity well" pulling the texture inward.
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            float2 center = (touchPos.x < 0.0) ? float2(0.5) : touchPos;
            float2 d = uv - center;
            float r = length(d) + 1e-4;
            // Pull strength falls off with distance; remap UV inward by `pull`.
            float pull = strength * exp(-r * 5.0);
            float2 p = uv - normalize(d) * pull;
            float2 g = floor(p * 18.0);
            float c = mod(g.x + g.y, 2.0);
            half3 a = half3(0.06, 0.08, 0.18);
            half3 b = half3(0.95, 0.55, 0.30);
            return half4(mix(a, b, half(c)), 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Pull Field", category = LessonCategory.INTERACTIVE, complexity = 3,
                conceptIntro = "Distort sample coords toward the cursor with exp falloff — texture bends like a gravity well.",
                agslSource = SOURCE,
                controls = persistentListOf(LessonControl.FloatRange("Strength", "strength", 0f, 0.25f, 0.10f)),
            )
        )
    }
}

object PointerHeatStripes {
    val id = "interactive-04-heat-stripes"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float  time;
        uniform float2 touchPos;
        uniform float  touchTime;
        uniform float density;
        // Stripes whose phase offset is driven by distance to cursor *and* age
        // since the last tap — gives a heat-shockwave feel.
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            float2 center = (touchPos.x < 0.0) ? float2(0.5) : touchPos;
            float aspect = resolution.x / resolution.y;
            float2 d = uv - center;
            d.x *= aspect;
            float r = length(d);
            float age = max(time - touchTime, 0.0);
            float phase = -age * 6.0 + r * density;
            float v = 0.5 + 0.5 * sin(phase);
            // Heat falloff: redder near recent tap.
            float heat = exp(-r * 4.0) * exp(-age * 0.8);
            half3 cool = half3(0.06, 0.10, 0.20);
            half3 warm = mix(half3(0.95, 0.85, 0.30), half3(0.95, 0.30, 0.20), half(heat));
            half3 col = mix(cool, warm, half(v));
            return half4(col, 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Heat Shockwave", category = LessonCategory.INTERACTIVE, complexity = 4,
                conceptIntro = "Combine distance-to-touch with (time - touchTime) inside a sin() — phases sweep outward like a thermal pulse.",
                agslSource = SOURCE,
                controls = persistentListOf(LessonControl.FloatRange("Density", "density", 8f, 60f, 24f)),
                screenRecordingHint = "Tap repeatedly — overlapping shockwaves create rich beat patterns.",
            )
        )
    }
}

object PointerLensFlare {
    val id = "interactive-05-lens-flare"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float  time;
        uniform float2 touchPos;
        uniform float  touchTime;
        uniform float intensity;
        // Classic anamorphic lens flare: a hot core at the touch point plus a
        // row of ghost circles strung along the touch→centre axis, mirroring
        // how real lens elements reflect across the optical axis.
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            float aspect = resolution.x / resolution.y;
            // Until the first tap the sun drifts on a slow figure-eight.
            float2 sun = (touchPos.x < 0.0)
                ? float2(0.5 + 0.20 * sin(time * 0.21), 0.46 + 0.13 * sin(time * 0.34))
                : touchPos;
            float2 axis = float2(0.5) - sun;
            float2 d = (uv - sun) * float2(aspect, 1.0);
            float r = length(d);

            half3 col = mix(half3(0.015, 0.020, 0.045), half3(0.030, 0.022, 0.055), half(uv.y));

            // Hot core + wide halo; a fresh tap adds a flash that decays fast.
            float flash = (touchTime < 0.0) ? 0.0 : exp(-max(time - touchTime, 0.0) * 2.4);
            float core = exp(-r * r * 320.0) * (1.6 + 2.4 * flash);
            float halo = exp(-r * 4.5) * 0.35;
            col += half3(1.0, 0.92, 0.78) * half(core);
            col += half3(0.55, 0.45, 0.90) * half(halo);

            // Five ghosts strung through the screen centre at fixed axis steps.
            for (int i = 0; i < 5; i++) {
                float fi = float(i);
                float ts = -0.55 + fi * 0.36;
                float2 gp = sun + axis * ts;
                float2 gd = (uv - gp) * float2(aspect, 1.0);
                float gr = 0.028 + 0.055 * fract(fi * 0.618);
                // Hollow ring ghosts read better than discs.
                float ring = exp(-abs(length(gd) - gr) * 42.0) * (0.30 + 0.25 * fract(fi * 0.377));
                half3 tint = half3(0.5) + half3(0.5) * cos(6.2831 * (fi * 0.29 + half3(0.0, 0.33, 0.67)));
                col += tint * half(ring);
            }

            // Anamorphic streak: a thin horizontal blade through the sun.
            float blade = exp(-abs(uv.y - sun.y) * 90.0) * exp(-abs(uv.x - sun.x) * 1.9);
            col += half3(0.35, 0.60, 1.0) * half(blade * 0.8);

            col *= half(intensity);
            return half4(col, 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Lens Flare", category = LessonCategory.INTERACTIVE, complexity = 4,
                conceptIntro = "Ghosts placed at fixed fractions along the touch→centre axis mimic internal lens reflections; tap flash = exp(-age).",
                agslSource = SOURCE,
                controls = persistentListOf(LessonControl.FloatRange("Intensity", "intensity", 0f, 2.5f, 1.1f)),
                screenRecordingHint = "Tap a corner, then near centre — the ghost train pivots through the middle.",
            )
        )
    }
}
