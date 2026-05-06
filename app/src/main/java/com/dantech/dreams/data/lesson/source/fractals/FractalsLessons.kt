package com.dantech.dreams.data.lesson.source.fractals

import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonControl
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRegistry
import kotlinx.collections.immutable.persistentListOf

object Mandelbrot {
    val id = "fractals-01-mandelbrot"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float zoom;
        $FRACTAL_HELPERS
        half4 main(float2 fragCoord) {
            float2 uv = (fragCoord - 0.5 * resolution) / resolution.y;
            float2 c = uv / zoom + float2(-0.75, 0.0);
            float2 z = float2(0.0);
            float iter = 0.0;
            const int MAX = 96;
            for (int i = 0; i < MAX; i++) {
                z = cmul(z, z) + c;
                if (dot(z, z) > 256.0) { iter = float(i); break; }
                iter = float(i);
            }
            float t = smoothEscape(iter, z) / float(MAX);
            float3 col = 0.5 + 0.5 * cos(6.2831 * (t + float3(0.0, 0.10, 0.20)));
            if (iter >= float(MAX - 1)) col = float3(0.02, 0.02, 0.06);
            return half4(half3(col), 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Mandelbrot", category = LessonCategory.FRACTALS, complexity = 4,
                conceptIntro = "Iterate z ← z² + c until |z| escapes. Smooth iteration count + cosine palette gives continuous coloring.",
                agslSource = SOURCE,
                controls = persistentListOf(LessonControl.FloatRange("Zoom", "zoom", 0.5f, 12f, 1f)),
                screenRecordingHint = "Sweep zoom 0.5 → 8 — boundary becomes increasingly intricate.",
            )
        )
    }
}

object JuliaSet {
    val id = "fractals-02-julia"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float time;
        uniform float radius;
        $FRACTAL_HELPERS
        half4 main(float2 fragCoord) {
            float2 uv = (fragCoord - 0.5 * resolution) / resolution.y * 1.6;
            // c traces a slow circle in parameter space → set "breathes".
            float2 c = float2(cos(time * 0.20), sin(time * 0.27)) * radius;
            float2 z = uv;
            float iter = 0.0;
            const int MAX = 80;
            for (int i = 0; i < MAX; i++) {
                z = cmul(z, z) + c;
                if (dot(z, z) > 64.0) { iter = float(i); break; }
                iter = float(i);
            }
            float t = smoothEscape(iter, z) / float(MAX);
            float3 col = 0.5 + 0.5 * cos(6.2831 * (t + float3(0.20, 0.40, 0.70)));
            if (iter >= float(MAX - 1)) col = float3(0.0);
            return half4(half3(col), 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Julia Set", category = LessonCategory.FRACTALS, complexity = 4,
                conceptIntro = "Same iterate as Mandelbrot, but c is fixed across the plane. Animate c to morph the set in real time.",
                agslSource = SOURCE,
                controls = persistentListOf(LessonControl.FloatRange("Radius", "radius", 0.50f, 0.85f, 0.78f)),
                screenRecordingHint = "Record 45s — set continuously morphs, irresistible to watch.",
            )
        )
    }
}

object NewtonZ3 {
    val id = "fractals-03-newton"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float zoom;
        $FRACTAL_HELPERS
        // Newton's method on z^3 - 1 = 0. Three roots → three basins of attraction
        // colored distinctly. Boundary is fractal because basins interlace at every scale.
        half4 main(float2 fragCoord) {
            float2 uv = (fragCoord - 0.5 * resolution) / resolution.y / zoom;
            float2 z = uv;
            const int MAX = 32;
            for (int i = 0; i < MAX; i++) {
                float2 z2 = cmul(z, z);
                float2 z3 = cmul(z2, z);
                float2 num = z3 - float2(1.0, 0.0);
                float2 den = 3.0 * z2 + float2(1e-4, 0.0);
                z = z - cdiv(num, den);
            }
            // Three roots of unity at angles 0, 2π/3, 4π/3.
            float2 r0 = float2( 1.0, 0.0);
            float2 r1 = float2(-0.5,  0.866);
            float2 r2 = float2(-0.5, -0.866);
            float d0 = distance(z, r0);
            float d1 = distance(z, r1);
            float d2 = distance(z, r2);
            float3 col = (d0 < d1 && d0 < d2) ? float3(0.95, 0.30, 0.30)
                       : (d1 < d2)            ? float3(0.30, 0.85, 0.55)
                                              : float3(0.30, 0.55, 0.95);
            // Fade by distance to nearest root → soft basin shading.
            float minD = min(d0, min(d1, d2));
            col *= 1.0 - clamp(minD, 0.0, 0.9);
            return half4(half3(col), 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Newton's Method (z³ − 1)", category = LessonCategory.FRACTALS, complexity = 5,
                conceptIntro = "Newton iteration on z³−1 has 3 attractors. Color each pixel by the root it lands in — basins are fractal.",
                agslSource = SOURCE,
                controls = persistentListOf(LessonControl.FloatRange("Zoom", "zoom", 0.5f, 6f, 1f)),
            )
        )
    }
}

object SierpinskiFold {
    val id = "fractals-04-sierpinski"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float time;
        uniform float depth;
        // IFS-style fold: at each step, mirror p toward a triangle vertex and scale.
        // Distance to triangle approximates Sierpinski-gasket SDF.
        float sdTri(float2 p) {
            const float k = 1.732;
            p.x = abs(p.x) - 0.5;
            p.y = p.y + 0.289;
            if (p.x + k * p.y > 0.0) p = float2(p.x - k * p.y, -k * p.x - p.y) * 0.5;
            p.x -= clamp(p.x, -1.0, 0.0);
            return -length(p) * sign(p.y);
        }
        half4 main(float2 fragCoord) {
            float2 p = (fragCoord - 0.5 * resolution) / resolution.y * 1.2;
            float s = 1.0;
            for (int i = 0; i < 8; i++) {
                if (float(i) >= depth) break;
                p = abs(p);
                p.x -= 0.5 * s;
                if (p.x < 0.0) p.x = -p.x;
                s *= 0.5;
                p *= 2.0;
            }
            float d = sdTri(p) * s;
            float a = 1.0 - smoothstep(0.0, 0.005, d);
            float pulse = 0.5 + 0.5 * sin(time * 1.5);
            half3 bg = half3(0.04, 0.04, 0.10);
            half3 fg = mix(half3(0.30, 0.85, 0.95), half3(0.95, 0.55, 0.30), half(pulse));
            return half4(mix(bg, fg, half(a)), 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Sierpinski Fold", category = LessonCategory.FRACTALS, complexity = 5,
                conceptIntro = "Iterated function systems: fold and scale space repeatedly, then evaluate one SDF — exact geometric self-similarity.",
                agslSource = SOURCE,
                controls = persistentListOf(LessonControl.FloatRange("Depth", "depth", 1f, 8f, 5f)),
            )
        )
    }
}
