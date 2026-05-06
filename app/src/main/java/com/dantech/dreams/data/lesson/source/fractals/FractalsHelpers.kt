package com.dantech.dreams.data.lesson.source.fractals

// Complex-number helpers shared by Mandelbrot/Julia/Newton lessons.
// `cmul` is complex multiplication, `cdiv` is complex division.
internal const val FRACTAL_HELPERS = """
    float2 cmul(float2 a, float2 b) {
        return float2(a.x * b.x - a.y * b.y, a.x * b.y + a.y * b.x);
    }
    float2 cdiv(float2 a, float2 b) {
        float denom = b.x * b.x + b.y * b.y + 1e-6;
        return float2(a.x * b.x + a.y * b.y, a.y * b.x - a.x * b.y) / denom;
    }
    // Smooth iteration count: takes the integer escape step and adds the
    // fractional `log(log|z|)` correction so coloring is continuous, not banded.
    float smoothEscape(float i, float2 z) {
        return i - log2(max(log2(dot(z, z)), 1e-6)) + 4.0;
    }
"""
