package com.dantech.dreams.data.lesson.source.noise

import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonControl
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRegistry
import kotlinx.collections.immutable.persistentListOf

object HashLesson {
    val id = "noise-01-hash"
    private val SOURCE = """
        uniform float2 resolution;
        $NOISE_HELPERS
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            float v = hash21(floor(uv * 64.0));
            return half4(half3(v), 1.0);
        }
    """.trimIndent()
    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Pseudo-Random Hash", category = LessonCategory.NOISE, complexity = 2,
                conceptIntro = "fract(sin(dot(p, magic)) * big) yields cheap, deterministic per-pixel noise — the bedrock of procedural texturing.",
                agslSource = SOURCE,
            )
        )
    }
}

object ValueNoise {
    val id = "noise-02-value"
    private val SOURCE = """
        uniform float2 resolution;
        $NOISE_HELPERS
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            float v = valueNoise(uv * 8.0);
            return half4(half3(v), 1.0);
        }
    """.trimIndent()
    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Value Noise", category = LessonCategory.NOISE, complexity = 2,
                conceptIntro = "Bilinear-interpolated hash gives smooth-ish noise — the ancestor of Perlin/Simplex.",
                agslSource = SOURCE,
            )
        )
    }
}

object FbmClouds {
    val id = "noise-03-fbm"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float octaves;
        $NOISE_HELPERS
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            float2 p = uv * 4.0;
            float v = 0.0;
            float amp = 0.5;
            for (int i = 0; i < 6; i++) {
                if (float(i) >= octaves) break;
                v += amp * valueNoise(p);
                p *= 2.0;
                amp *= 0.5;
            }
            half3 sky = mix(half3(0.10, 0.20, 0.40), half3(0.95, 0.95, 1.00), half(v));
            return half4(sky, 1.0);
        }
    """.trimIndent()
    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "fBM Clouds", category = LessonCategory.NOISE, complexity = 3,
                conceptIntro = "Sum N octaves of noise, doubling frequency and halving amplitude each step. Bounded loop required for AGSL.",
                agslSource = SOURCE,
                controls = persistentListOf(LessonControl.FloatRange("Octaves", "octaves", 1f, 6f, 4f)),
            )
        )
    }
}

object VoronoiCells {
    val id = "noise-04-voronoi"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float time;
        uniform float cells;
        $NOISE_HELPERS
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution * cells;
            float2 i = floor(uv);
            float2 f = fract(uv);
            float minDist = 1.0;
            for (int y = -1; y <= 1; y++) {
                for (int x = -1; x <= 1; x++) {
                    float2 g = float2(float(x), float(y));
                    float2 o = float2(hash21(i + g), hash21(i + g + 17.7));
                    o = 0.5 + 0.5 * sin(time + 6.2831 * o);
                    float2 r = g + o - f;
                    minDist = min(minDist, dot(r, r));
                }
            }
            float d = sqrt(minDist);
            half3 col = mix(half3(0.05, 0.05, 0.10), half3(0.30, 0.85, 0.95), half(d));
            return half4(col, 1.0);
        }
    """.trimIndent()
    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Voronoi Cells", category = LessonCategory.NOISE, complexity = 4,
                conceptIntro = "For each pixel, find the nearest jittered seed in a 3×3 neighborhood. Animate seeds with sin(time) for cellular life.",
                agslSource = SOURCE,
                controls = persistentListOf(LessonControl.FloatRange("Cells", "cells", 4f, 32f, 12f)),
                screenRecordingHint = "Record 30s with cells=12 — looks alive, very LinkedIn-friendly.",
            )
        )
    }
}

object Plasma {
    val id = "noise-05-plasma"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float time;
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution * 6.0;
            float v = sin(uv.x + time) + sin(uv.y + time*1.3) + sin(length(uv) + time*0.7);
            v = 0.5 + 0.25 * v;
            half3 col = half3(
                half(0.5 + 0.5 * sin(v * 6.2831)),
                half(0.5 + 0.5 * sin(v * 6.2831 + 2.094)),
                half(0.5 + 0.5 * sin(v * 6.2831 + 4.188))
            );
            return half4(col, 1.0);
        }
    """.trimIndent()
    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Plasma", category = LessonCategory.NOISE, complexity = 2,
                conceptIntro = "Three sine sums + an HSV-like phase shift = 90s demoscene plasma in 8 lines.",
                agslSource = SOURCE,
            )
        )
    }
}

object WarpedLava {
    val id = "noise-06-warped-lava"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float time;
        $NOISE_HELPERS
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            float2 p = uv * 3.0 + float2(0.0, time * 0.10);
            float2 q = float2(fbm(p), fbm(p + 5.2));
            float2 r = float2(fbm(p + 4.0 * q + float2(1.7, 9.2)), fbm(p + 4.0 * q + float2(8.3, 2.8)));
            float v = fbm(p + 4.0 * r);
            half3 lo = half3(0.10, 0.02, 0.04);
            half3 hi = half3(1.00, 0.65, 0.10);
            half3 col = mix(lo, hi, half(smoothstep(0.20, 0.80, v)));
            return half4(col, 1.0);
        }
    """.trimIndent()
    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Warped Lava", category = LessonCategory.NOISE, complexity = 5,
                conceptIntro = "Domain warping: feed fbm into itself. The payoff lesson — looks like a graphics textbook cover.",
                agslSource = SOURCE,
                screenRecordingHint = "Record 45s of warped-lava as the showcase prelude.",
            )
        )
    }
}

object AuroraBorealis {
    val id = "noise-07-aurora-borealis"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float time;
        uniform float drift;
        uniform float curtains;
        $NOISE_HELPERS
        // fbm feeds the ridge line of each curtain; a second, high-frequency
        // fbm gates it into vertical light rays — the two together read as
        // real aurora drapery instead of a soft glow blob.
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            float t = time * drift;

            // Night sky, deepening toward the top of the frame.
            half3 col = mix(half3(0.008, 0.020, 0.060), half3(0.002, 0.004, 0.018), half(smoothstep(0.05, 1.0, uv.y)));

            // Star field: sparse hashed grid points with slow twinkle, kept
            // out of the lower sky where the aurora will sit.
            float2 sg = float2(uv.x * resolution.x / resolution.y, uv.y) * 42.0;
            float2 cell = floor(sg);
            float hs = hash21(cell);
            float2 sp = float2(hash21(cell + 11.0), hash21(cell + 27.0));
            float sd = length(fract(sg) - sp);
            float tw = 0.55 + 0.45 * sin(time * (1.0 + hs * 3.0) + hs * 40.0);
            float star = (1.0 - smoothstep(0.0, 0.05, sd)) * step(0.968, hs) * tw;
            col += half3(0.85, 0.92, 1.0) * half(star * 1.4) * half(smoothstep(0.30, 0.55, uv.y));

            // Three parallax curtains. Each hangs from an fbm ridge that drifts
            // over x; brightness peaks just under the ridge and fades upward.
            for (int i = 0; i < 3; i++) {
                float fi = float(i);
                float ridge = fbm(float2(uv.x * (1.8 + fi * 0.9) + fi * 17.0, t * 0.11 + fi * 3.0));
                float base = 0.42 + fi * 0.085 + (ridge - 0.5) * 0.30;
                float d = uv.y - base;
                // Above the ridge the light decays exponentially; just below it,
                // a short skirt of spill keeps the edge soft.
                float rise = exp(-max(d, 0.0) * (11.0 - fi * 2.0));
                float skirt = exp(-max(-d, 0.0) * 30.0) * 0.45;
                float rays = 0.40 + 0.60 * fbm(float2(uv.x * (16.0 + fi * 6.0), t * 0.32 + fi * 9.0));
                float layer = (rise + skirt) * rays * (1.0 - fi * 0.18);
                // Green near the ridge fading to violet higher up — the real
                // altitude gradient of oxygen emission.
                half3 aurora = mix(half3(0.10, 0.95, 0.45), half3(0.40, 0.22, 0.90), half(clamp(d * 4.0 + 0.30 + fi * 0.16, 0.0, 1.0)));
                col += aurora * half(layer * curtains * 0.42);
            }

            // Cold horizon haze anchoring the bottom of the frame.
            col += half3(0.04, 0.08, 0.15) * half(smoothstep(0.32, 0.0, uv.y)) * 0.4;
            return half4(col, 1.0);
        }
    """.trimIndent()
    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Aurora Borealis", category = LessonCategory.NOISE, complexity = 4,
                conceptIntro = "Layered curtains hung from wandering fbm ridge lines, gated into vertical rays by a second high-frequency fbm.",
                agslSource = SOURCE,
                controls = persistentListOf(
                    LessonControl.FloatRange("Drift", "drift", 0f, 3f, 1f),
                    LessonControl.FloatRange("Curtains", "curtains", 0.3f, 2f, 1f),
                ),
                screenRecordingHint = "Drift 1.4 with Curtains 1.3 is the postcard look.",
            )
        )
    }
}
