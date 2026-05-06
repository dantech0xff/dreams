package com.dantech.dreams.data.lesson.source.sdf

internal const val SDF_HELPERS = """
    float sdCircle(float2 p, float r) {
        return length(p) - r;
    }
    float sdBox(float2 p, float2 b, float r) {
        float2 q = abs(p) - b;
        return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
    }
    float opSmoothUnion(float d1, float d2, float k) {
        float h = clamp(0.5 + 0.5 * (d2 - d1) / k, 0.0, 1.0);
        return mix(d2, d1, h) - k * h * (1.0 - h);
    }
"""
