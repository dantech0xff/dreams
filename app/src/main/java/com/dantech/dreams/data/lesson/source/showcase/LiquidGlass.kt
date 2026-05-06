package com.dantech.dreams.data.lesson.source.showcase

import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRegistry
import com.dantech.dreams.data.lesson.source.noise.NOISE_HELPERS

object LiquidGlass {
    val id = "showcase-01-liquid-glass"

    val SOURCE = """
        uniform float2 resolution;
        uniform float time;
        $NOISE_HELPERS

        half3 background(float2 uv) {
            float2 p = uv * 3.0 + float2(0.0, time * 0.05);
            float v = fbm(p);
            half3 lo = half3(0.05, 0.10, 0.30);
            half3 hi = half3(0.95, 0.55, 0.85);
            return mix(lo, hi, half(v));
        }

        float roundedRect(float2 p, float2 b, float r) {
            float2 q = abs(p) - b;
            return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
        }

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            float2 c = float2(0.5 + 0.10 * sin(time * 0.6), 0.5 + 0.06 * cos(time * 0.4));
            float2 p = uv - c;
            p.x *= resolution.x / resolution.y;

            float2 d = float2(fbm(uv * 2.5 + time * 0.10), fbm(uv * 2.5 + 7.7));
            d = (d - 0.5) * 0.06;
            half3 bg = background(uv);
            half3 refr = background(uv + d);

            float panel = roundedRect(p, float2(0.30, 0.18), 0.12);
            float inside = 1.0 - smoothstep(0.0, 0.005, panel);
            float rim = smoothstep(-0.01, 0.0, panel) - smoothstep(0.0, 0.01, panel);

            half3 glass = mix(refr, half3(1.0), 0.10) + half3(rim) * 0.35;
            half3 col = mix(bg, glass, half(inside));

            return half4(col, 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id,
                title = "Liquid Glass",
                category = LessonCategory.SHOWCASE,
                complexity = 5,
                conceptIntro = "Animated rounded-rect panel over a procedural background. Inside the panel: fbm-displaced sample + rim highlight = iOS 26 vibe.",
                agslSource = SOURCE,
                screenRecordingHint = "Record portrait — the drifting panel reads as 'liquid glass' immediately.",
            )
        )
    }
}
