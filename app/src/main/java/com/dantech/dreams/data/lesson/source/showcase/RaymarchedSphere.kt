package com.dantech.dreams.data.lesson.source.showcase

import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRegistry

object RaymarchedSphere {
    val id = "showcase-03-raymarched-sphere"

    val SOURCE = """
        uniform float2 resolution;
        uniform float time;

        float sdSphere(float3 p, float r) { return length(p) - r; }

        float3 lambert(float3 n, float3 l, float3 albedo) {
            float diff = max(dot(n, l), 0.0);
            float3 ambient = albedo * 0.15;
            return ambient + albedo * diff;
        }

        half4 main(float2 fragCoord) {
            float2 uv = (fragCoord - 0.5 * resolution) / resolution.y;

            float3 ro = float3(0.0, 0.0, 2.5);
            float3 rd = normalize(float3(uv, -1.5));

            float t = 0.0;
            float hit = 0.0;
            float3 p = ro;
            for (int i = 0; i < 64; i++) {
                p = ro + rd * t;
                float d = sdSphere(p, 1.0);
                if (d < 0.001) { hit = 1.0; break; }
                if (t > 6.0) break;
                t += d;
            }

            half3 col = half3(0.05, 0.05, 0.10);
            if (hit > 0.5) {
                float3 n = normalize(p);
                float3 l = normalize(float3(sin(time), 0.6, cos(time)));
                float3 albedo = float3(0.85, 0.45, 0.95);
                float3 c = lambert(n, l, albedo);
                col = half3(c);
            } else {
                float v = 0.5 + 0.5 * uv.y;
                col = mix(half3(0.05, 0.05, 0.15), half3(0.10, 0.20, 0.50), half(v));
            }
            return half4(col, 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id,
                title = "Raymarched Sphere",
                category = LessonCategory.SHOWCASE,
                complexity = 5,
                conceptIntro = "64-step raymarcher rendering a single sphere with lambert lighting. Pure fragment shader 3D.",
                agslSource = SOURCE,
                screenRecordingHint = "Record landscape, the orbiting light reads better on wider aspect.",
            )
        )
    }
}
