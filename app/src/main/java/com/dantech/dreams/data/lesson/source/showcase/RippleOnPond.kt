package com.dantech.dreams.data.lesson.source.showcase

import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonControl
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRegistry
import kotlinx.collections.immutable.persistentListOf

/**
 * Showcase: rain on a midnight pond.
 *
 * Six deterministically-spawned drop sources emit damped traveling sine waves.
 * Each pixel sums the wavefronts to a height field, samples four neighbors for a
 * central-difference normal, then shades with Blinn-Phong specular + Fresnel rim
 * over a depth-tinted water gradient. Caustic-style highlights pop the crests.
 */
object RippleOnPond {
    val id = "showcase-04-ripple-on-pond"

    private val SOURCE = """
        uniform float2 resolution;
        uniform float time;
        uniform float dropRate;
        uniform float waveSpeed;
        uniform float damping;
        // Touch-driven ripple slot: touchPos in [0,1] uv, touchTime is shader-time of the
        // last tap. touchTime < 0 means "no tap yet" — the host writes (-1,-1,-1) initially.
        uniform float2 touchPos;
        uniform float touchTime;

        float hash11(float n) {
            return fract(sin(n * 127.1) * 43758.5453);
        }
        float2 hash22(float n) {
            return float2(hash11(n), hash11(n + 17.31));
        }

        // Single damped travelling wave from `src`, evaluated at `p` after `age` seconds.
        float ripple(float2 p, float2 src, float age) {
            float d = length(p - src);
            float front = age * waveSpeed;
            float tt = d - front;
            float distEnv = exp(-damping * abs(tt));
            float ageEnv = exp(-age * 0.45);
            return sin(tt * 38.0) * distEnv * ageEnv;
        }

        // Touch-driven ripple: 2.5x amplitude relative to procedural drops, lifespan 5s.
        float touchRipple(float2 p, float minDim) {
            if (touchTime < 0.0) return 0.0;
            float age = time - touchTime;
            if (age < 0.0 || age >= 5.0) return 0.0;
            float2 tp = (touchPos * resolution - 0.5 * resolution) / minDim;
            return ripple(p, tp, age) * 2.5;
        }

        // Splash flash: bright expanding ring at the tap point, fades over 0.6s.
        // Lives outside the height field so it pops even if the wave physics is subtle.
        half3 touchSplash(float2 p, float minDim) {
            if (touchTime < 0.0) return half3(0.0);
            float age = time - touchTime;
            if (age < 0.0 || age >= 0.6) return half3(0.0);
            float2 tp = (touchPos * resolution - 0.5 * resolution) / minDim;
            float d = length(p - tp);
            float r = age * waveSpeed * 1.2;
            float ring = smoothstep(0.05, 0.0, abs(d - r));
            float core = smoothstep(0.06, 0.0, d) * (1.0 - smoothstep(0.0, 0.15, age));
            float fade = 1.0 - smoothstep(0.0, 0.6, age);
            return half3(0.95, 0.98, 1.00) * half((ring + core * 0.6) * fade);
        }

        // Sum of 6 staggered drops + the live touch ripple; each procedural slot reseeds
        // its position every `lifespan` seconds.
        float height(float2 p, float2 viewExt, float minDim) {
            float h = 0.0;
            float lifespan = 5.0;
            for (int i = 0; i < 6; i++) {
                float fi = float(i);
                float tt = time * dropRate + fi * (lifespan / 6.0);
                float cycle = floor(tt / lifespan);
                float age = tt - cycle * lifespan;
                float2 src = (hash22(fi * 31.7 + cycle * 7.91) * 2.0 - 1.0) * 0.45 * viewExt;
                h += ripple(p, src, age);
            }
            h += touchRipple(p, minDim);
            return h;
        }

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            float minDim = min(resolution.x, resolution.y);
            float2 viewExt = resolution / minDim;
            float2 p = (fragCoord - 0.5 * resolution) / minDim;

            float h = height(p, viewExt, minDim);

            // Central-difference normal of the height field.
            float eps = 0.004;
            float hxp = height(p + float2(eps, 0.0), viewExt, minDim);
            float hxm = height(p - float2(eps, 0.0), viewExt, minDim);
            float hyp = height(p + float2(0.0, eps), viewExt, minDim);
            float hym = height(p - float2(0.0, eps), viewExt, minDim);
            float dx = (hxp - hxm) / (2.0 * eps);
            float dy = (hyp - hym) / (2.0 * eps);
            float3 n = normalize(float3(-dx, -dy, 12.0));

            // Blinn-Phong specular + Fresnel-ish rim against a top-left light.
            float3 L = normalize(float3(-0.5, -0.7, 0.7));
            float3 V = float3(0.0, 0.0, 1.0);
            float3 H = normalize(L + V);
            float spec = pow(max(dot(n, H), 0.0), 80.0);
            float fres = pow(1.0 - max(dot(n, V), 0.0), 3.0);

            // Water gradient driven by height, then darkened toward the top of frame.
            half3 deep = half3(0.01, 0.04, 0.12);
            half3 mid  = half3(0.05, 0.20, 0.42);
            half3 surf = half3(0.30, 0.78, 0.92);
            float th = clamp(0.5 + 0.5 * h, 0.0, 1.0);
            half3 water = mix(deep, mid, half(smoothstep(0.0, 0.55, th)));
            water = mix(water, surf, half(smoothstep(0.65, 1.0, th)));
            water = mix(deep, water, half(0.55 + 0.45 * uv.y));

            // Crest caustics + spec + fresnel rim accumulate as additive highlights.
            half3 caustic = half3(0.45, 0.95, 1.00) * half(pow(max(h, 0.0), 2.0) * 2.0);
            half3 rim = half3(0.20, 0.60, 0.90) * half(fres * 0.25);
            half3 col = water + caustic + half3(spec * 0.9) + rim + touchSplash(p, minDim);

            // Centered vignette in screen-uv space.
            float2 cuv = uv - 0.5;
            float vr = length(cuv);
            col *= half(smoothstep(0.65, 0.10, vr));

            return half4(col, 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id,
                title = "Ripple on Pond",
                category = LessonCategory.SHOWCASE,
                complexity = 5,
                conceptIntro = "Tap anywhere to drop a stone — your touch adds a 1.8x amplitude ripple on top of six procedural raindrops. Central-difference normals + Blinn-Phong specular + caustic highlights sell the water surface.",
                agslSource = SOURCE,
                controls = persistentListOf(
                    LessonControl.FloatRange("Drop Rate", "dropRate", 0.3f, 2.5f, 1.0f),
                    LessonControl.FloatRange("Wave Speed", "waveSpeed", 0.2f, 1.5f, 0.6f),
                    LessonControl.FloatRange("Damping", "damping", 1.5f, 6.0f, 3.5f),
                ),
                screenRecordingHint = "Record portrait. Slow Drop Rate to 0.4, then tap to throw stones in — perfect for a 'tap-to-ripple' clip.",
            )
        )
    }
}
