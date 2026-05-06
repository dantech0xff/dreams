package com.dantech.dreams.data.lesson.source.noise

internal const val NOISE_HELPERS = """
    float hash21(float2 p) {
        return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
    }
    float valueNoise(float2 p) {
        float2 i = floor(p);
        float2 f = fract(p);
        float a = hash21(i);
        float b = hash21(i + float2(1.0, 0.0));
        float c = hash21(i + float2(0.0, 1.0));
        float d = hash21(i + float2(1.0, 1.0));
        float2 u = f * f * (3.0 - 2.0 * f);
        return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
    }
    float fbm(float2 p) {
        float v = 0.0;
        float a = 0.5;
        for (int i = 0; i < 6; i++) {
            v += a * valueNoise(p);
            p *= 2.0;
            a *= 0.5;
        }
        return v;
    }
"""
