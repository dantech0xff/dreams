package com.dantech.dreams.data.lesson.source.lighting

import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonControl
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRegistry
import kotlinx.collections.immutable.persistentListOf

// Shared sphere helper — all lighting lessons paint a unit sphere on a flat
// background. We compute the implicit normal from screen-space distance, so
// no raymarching is needed and the math stays focused on the *lighting* term.
// Returns float4 packed as (n.xyz, mask) — avoids out-params for SkSL portability.
private const val SPHERE_HELPERS = """
    float4 sphereSample(float2 uv) {
        float r2 = dot(uv, uv);
        if (r2 >= 1.0) return float4(0.0, 0.0, 1.0, 0.0);
        float z = sqrt(1.0 - r2);
        return float4(uv, z, 1.0);
    }
"""

object LambertSphere {
    val id = "lighting-01-lambert"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float time;
        uniform float ambient;
        $SPHERE_HELPERS
        half4 main(float2 fragCoord) {
            float2 uv = (fragCoord - 0.5 * resolution) / (0.5 * min(resolution.x, resolution.y));
            float4 s = sphereSample(uv);
            float3 n = s.xyz;
            float m = s.w;
            // Light orbits in the xy plane.
            float3 L = normalize(float3(cos(time * 0.5), sin(time * 0.5) * 0.6, 0.7));
            float diff = max(dot(n, L), 0.0);
            float3 albedo = float3(0.95, 0.55, 0.35);
            float3 col = albedo * (ambient + (1.0 - ambient) * diff);
            float3 bg = float3(0.04, 0.05, 0.10);
            return half4(half3(mix(bg, col, m)), 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Lambert Sphere", category = LessonCategory.LIGHTING, complexity = 2,
                conceptIntro = "Lambert: diffuse = max(dot(N, L), 0). The whole 'looks 3D' illusion in one inner product.",
                agslSource = SOURCE,
                controls = persistentListOf(LessonControl.FloatRange("Ambient", "ambient", 0f, 0.6f, 0.10f)),
            )
        )
    }
}

object PhongHighlight {
    val id = "lighting-02-phong"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float time;
        uniform float shininess;
        $SPHERE_HELPERS
        half4 main(float2 fragCoord) {
            float2 uv = (fragCoord - 0.5 * resolution) / (0.5 * min(resolution.x, resolution.y));
            float4 s = sphereSample(uv);
            float3 n = s.xyz;
            float m = s.w;
            float3 L = normalize(float3(cos(time * 0.7), sin(time * 0.7) * 0.5, 0.8));
            float3 V = float3(0.0, 0.0, 1.0);
            float3 R = reflect(-L, n);
            float diff = max(dot(n, L), 0.0);
            float spec = pow(max(dot(R, V), 0.0), shininess);
            float3 albedo = float3(0.20, 0.45, 0.95);
            float3 col = albedo * (0.10 + 0.85 * diff) + float3(1.0) * spec;
            float3 bg = float3(0.04, 0.05, 0.10);
            return half4(half3(mix(bg, col, m)), 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Phong Highlight", category = LessonCategory.LIGHTING, complexity = 3,
                conceptIntro = "Add specular = pow(max(dot(R, V), 0), n). Shininess collapses the highlight from gloss to mirror.",
                agslSource = SOURCE,
                controls = persistentListOf(LessonControl.FloatRange("Shininess", "shininess", 1f, 128f, 32f)),
                screenRecordingHint = "Sweep shininess 4 → 96 — highlight tightens like adjusting a real flashlight.",
            )
        )
    }
}

object RimLight {
    val id = "lighting-03-rim"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float power;
        $SPHERE_HELPERS
        half4 main(float2 fragCoord) {
            float2 uv = (fragCoord - 0.5 * resolution) / (0.5 * min(resolution.x, resolution.y));
            float4 s = sphereSample(uv);
            float3 n = s.xyz;
            float m = s.w;
            float3 V = float3(0.0, 0.0, 1.0);
            // Rim term: bright where the surface faces away from the viewer.
            float rim = pow(1.0 - max(dot(n, V), 0.0), power);
            float3 base = float3(0.05, 0.07, 0.15);
            float3 rimColor = float3(0.30, 0.95, 1.00);
            float3 col = base + rimColor * rim;
            float3 bg = float3(0.02, 0.03, 0.06);
            return half4(half3(mix(bg, col, m)), 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Rim Light", category = LessonCategory.LIGHTING, complexity = 2,
                conceptIntro = "rim = pow(1 - dot(N, V), p) — sells silhouette and rescues unlit darks. Free 'subsurface' look.",
                agslSource = SOURCE,
                controls = persistentListOf(LessonControl.FloatRange("Power", "power", 1f, 8f, 3f)),
            )
        )
    }
}

object DayNightTerminator {
    val id = "lighting-04-terminator"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float time;
        uniform float softness;
        $SPHERE_HELPERS
        // Earth-style day/night: same N·L Lambert, but blended between two albedos
        // (lit ocean vs city-light blue) with a softness-controlled terminator band.
        half4 main(float2 fragCoord) {
            float2 uv = (fragCoord - 0.5 * resolution) / (0.5 * min(resolution.x, resolution.y));
            float4 s = sphereSample(uv);
            float3 n = s.xyz;
            float m = s.w;
            float3 L = normalize(float3(cos(time * 0.30), 0.10, sin(time * 0.30)));
            float lambert = dot(n, L);
            float t = smoothstep(-softness, softness, lambert);
            float3 night = float3(0.02, 0.04, 0.10) + float3(0.95, 0.85, 0.55) * 0.20 * pow(1.0 - t, 4.0);
            float3 day = mix(float3(0.10, 0.30, 0.55), float3(0.85, 0.90, 1.00), t);
            float3 col = mix(night, day, t);
            float3 bg = float3(0.0);
            return half4(half3(mix(bg, col, m)), 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Day/Night Terminator", category = LessonCategory.LIGHTING, complexity = 3,
                conceptIntro = "Same Lambert dot product, but blend between *two* shaded materials. Softness widens the dawn band.",
                agslSource = SOURCE,
                controls = persistentListOf(LessonControl.FloatRange("Softness", "softness", 0.02f, 0.5f, 0.12f)),
            )
        )
    }
}
