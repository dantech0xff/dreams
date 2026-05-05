package com.dantech.dreams.shaders.showcase

import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRegistry
import com.dantech.dreams.shaders.noise.NOISE_HELPERS

object AuroraRibbons {
    val id = "showcase-02-aurora-ribbons"

    val SOURCE = """
        uniform float2 resolution;
        uniform float time;
        $NOISE_HELPERS
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            uv.y = 1.0 - uv.y;
            float t = time * 0.20;
            float warp = fbm(float2(uv.x * 1.5, uv.y * 0.8 + t));
            float band1 = smoothstep(0.05, 0.0, abs(uv.y - 0.50 - warp * 0.18));
            float band2 = smoothstep(0.07, 0.0, abs(uv.y - 0.40 - fbm(uv * 2.0 + t * 1.3) * 0.20));
            float band3 = smoothstep(0.06, 0.0, abs(uv.y - 0.60 - fbm(uv * 2.5 + t * 0.7) * 0.20));

            half3 c1 = half3(0.10, 0.95, 0.65);
            half3 c2 = half3(0.20, 0.55, 0.95);
            half3 c3 = half3(0.85, 0.30, 0.95);

            half3 sky = mix(half3(0.01, 0.02, 0.06), half3(0.04, 0.06, 0.18), half(uv.y));
            half3 col = sky;
            col += c1 * half(band1) * 0.9;
            col += c2 * half(band2) * 0.7;
            col += c3 * half(band3) * 0.6;

            float stars = step(0.997, hash21(floor(fragCoord)));
            col += half3(stars) * 0.6;

            return half4(col, 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id,
                title = "Aurora Ribbons",
                category = LessonCategory.SHOWCASE,
                complexity = 4,
                conceptIntro = "Three fbm-warped horizontal bands over a vertical sky gradient + hash-noise stars. Pure Brush mode.",
                agslSource = SOURCE,
                screenRecordingHint = "Record 30s in portrait — aurora reads great on LinkedIn.",
            )
        )
    }
}
