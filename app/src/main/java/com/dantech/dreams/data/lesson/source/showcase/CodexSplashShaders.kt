package com.dantech.dreams.data.lesson.source.showcase

internal const val CODEX_SPLASH_BACKGROUND_SRC = """
uniform float2 resolution;
uniform float time;

float hash(float2 p) {
    return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453);
}

float softEllipse(float2 p, float2 center, float2 scale, float radius) {
    return smoothstep(radius, 0.0, length((p - center) * scale));
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / resolution;
    float m = min(resolution.x, resolution.y);
    float2 p = (fragCoord - resolution * 0.5) / m;
    float shimmer = 0.5 + 0.5 * sin(time * 0.28 + uv.x * 3.0 - uv.y * 2.0);

    float3 cobalt = float3(0.025, 0.20, 0.95);
    float3 violet = float3(0.18, 0.08, 0.86);
    float3 sky = float3(0.42, 0.72, 1.0);
    float3 col = mix(cobalt, violet, smoothstep(0.30, 0.96, uv.x));

    float horizonGlow = softEllipse(p, float2(-0.20, 0.02), float2(1.0, 2.4), 0.62);
    float lowerGlow = softEllipse(p, float2(-0.18, 0.33), float2(1.1, 1.8), 0.48);
    col = mix(col, sky, horizonGlow * 0.42);
    col += float3(0.02, 0.11, 0.28) * lowerGlow;

    float veil = 0.0;
    veil += softEllipse(p, float2(-0.56, -0.28), float2(0.62, 1.65), 0.58) * 0.78;
    veil += softEllipse(p, float2(-0.44, -0.03), float2(0.72, 1.45), 0.50) * 0.44;
    veil += softEllipse(p, float2(-0.30, 0.22), float2(0.88, 1.90), 0.42) * 0.30;
    veil *= smoothstep(1.05, 0.10, uv.x + uv.y * 0.18);
    col = mix(col, float3(0.92, 0.95, 1.0), veil * (0.48 + 0.04 * shimmer));

    float topBloom = softEllipse(p, float2(0.02, -0.55), float2(1.8, 0.75), 0.38);
    float centerBloom = softEllipse(p, float2(-0.08, -0.04), float2(1.55, 1.0), 0.32);
    col += float3(0.50, 0.45, 0.95) * topBloom * 0.30;
    col += float3(0.10, 0.28, 0.65) * centerBloom * 0.22;

    float vignette = smoothstep(0.92, 0.18, length(p * float2(1.05, 0.88)));
    col *= 0.72 + 0.28 * vignette;
    col += (hash(fragCoord + time * 17.0) - 0.5) * 0.018;

    return half4(half3(clamp(col, 0.0, 1.0)), 1.0);
}
"""

internal const val CODEX_SPLASH_ICON_SRC = """
uniform float2 resolution;
uniform float time;

float sdRoundBox(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + float2(r);
    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;
}

float sdSegment(float2 p, float2 a, float2 b, float r) {
    float2 pa = p - a;
    float2 ba = b - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length(pa - ba * h) - r;
}

float fill(float d, float aa) {
    return smoothstep(aa, -aa, d);
}

float rim(float d, float width, float softness) {
    return exp(-abs(d - width) * softness);
}

float3 prism(float phase) {
    return 0.52 + 0.48 * cos(6.2831853 * (phase + float3(0.00, 0.34, 0.67)));
}

float svgSegmentDistance(float2 p, float2 a, float2 b) {
    float2 pa = p - a;
    float2 ba = b - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length(pa - ba * h);
}

float svgRayCross(float2 p, float2 a, float2 b) {
    float crossesY = step(min(a.y, b.y), p.y) * (1.0 - step(max(a.y, b.y), p.y));
    float denom = b.y - a.y;
    float safeDenom = denom + (1.0 - step(0.0001, abs(denom))) * 0.0001;
    float x = a.x + (p.y - a.y) * (b.x - a.x) / safeDenom;
    return crossesY * step(p.x, x);
}

float2 svgEdge(float2 p, float2 a, float2 b) {
    return float2(svgSegmentDistance(p, a, b), svgRayCross(p, a, b));
}

float cloudSdf(float2 p) {
    float2 q = p / 0.02875 + float2(12.0, 12.0);
    float d = 1000.0;
    float crossings = 0.0;
    float2 s = float2(0.0, 0.0);
    s = svgEdge(q, float2(9.064, 3.344), float2(9.631, 3.153)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(9.631, 3.153), float2(10.218, 3.038)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(10.218, 3.038), float2(10.816, 3.000)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(10.816, 3.000), float2(11.413, 3.040)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(11.413, 3.040), float2(12.000, 3.155)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(12.000, 3.155), float2(12.567, 3.347)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(12.567, 3.347), float2(13.102, 3.615)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(13.102, 3.615), float2(13.599, 3.948)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(13.599, 3.948), float2(14.060, 4.328)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(14.060, 4.328), float2(14.648, 4.222)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(14.648, 4.222), float2(15.246, 4.183)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(15.246, 4.183), float2(15.843, 4.224)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(15.843, 4.224), float2(16.429, 4.342)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(16.429, 4.342), float2(16.996, 4.536)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(16.996, 4.536), float2(17.532, 4.802)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(17.532, 4.802), float2(18.029, 5.134)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(18.029, 5.134), float2(18.479, 5.529)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(18.479, 5.529), float2(18.874, 5.979)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(18.874, 5.979), float2(19.207, 6.476)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(19.207, 6.476), float2(19.471, 7.013)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(19.471, 7.013), float2(19.668, 7.578)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(19.668, 7.578), float2(19.782, 8.166)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(19.782, 8.166), float2(19.816, 8.763)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(19.816, 8.763), float2(19.784, 9.361)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(19.784, 9.361), float2(19.678, 9.949)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(19.678, 9.949), float2(20.056, 10.409)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(20.056, 10.409), float2(20.391, 10.905)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(20.391, 10.905), float2(20.656, 11.442)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(20.656, 11.442), float2(20.849, 12.008)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(20.849, 12.008), float2(20.961, 12.596)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(20.961, 12.596), float2(21.000, 13.193)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(21.000, 13.193), float2(20.964, 13.791)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(20.964, 13.791), float2(20.847, 14.378)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(20.847, 14.378), float2(20.652, 14.943)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(20.652, 14.943), float2(20.383, 15.478)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(20.383, 15.478), float2(20.052, 15.977)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(20.052, 15.977), float2(19.660, 16.429)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(19.660, 16.429), float2(19.210, 16.823)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(19.210, 16.823), float2(18.711, 17.155)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(18.711, 17.155), float2(18.174, 17.418)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(18.174, 17.418), float2(17.615, 17.625)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(17.615, 17.625), float2(17.396, 18.182)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(17.396, 18.182), float2(17.137, 18.721)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(17.137, 18.721), float2(16.805, 19.218)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(16.805, 19.218), float2(16.408, 19.667)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(16.408, 19.667), float2(15.959, 20.062)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(15.959, 20.062), float2(15.462, 20.396)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(15.462, 20.396), float2(14.925, 20.660)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(14.925, 20.660), float2(14.357, 20.849)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(14.357, 20.849), float2(13.770, 20.962)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(13.770, 20.962), float2(13.172, 21.000)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(13.172, 21.000), float2(12.575, 20.964)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(12.575, 20.964), float2(11.987, 20.849)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(11.987, 20.849), float2(11.421, 20.655)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(11.421, 20.655), float2(10.887, 20.385)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(10.887, 20.385), float2(10.392, 20.049)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(10.392, 20.049), float2(9.930, 19.672)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(9.930, 19.672), float2(9.344, 19.786)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(9.344, 19.786), float2(8.746, 19.813)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(8.746, 19.813), float2(8.148, 19.777)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(8.148, 19.777), float2(7.561, 19.662)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(7.561, 19.662), float2(6.995, 19.469)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(6.995, 19.469), float2(6.459, 19.202)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(6.459, 19.202), float2(5.962, 18.868)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(5.962, 18.868), float2(5.514, 18.471)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(5.514, 18.471), float2(5.122, 18.019)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(5.122, 18.019), float2(4.776, 17.531)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(4.776, 17.531), float2(4.522, 16.989)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(4.522, 16.989), float2(4.329, 16.422)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(4.329, 16.422), float2(4.215, 15.835)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(4.215, 15.835), float2(4.179, 15.237)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(4.179, 15.237), float2(4.220, 14.640)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(4.220, 14.640), float2(4.315, 14.053)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(4.315, 14.053), float2(3.923, 13.601)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(3.923, 13.601), float2(3.593, 13.102)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(3.593, 13.102), float2(3.332, 12.563)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(3.332, 12.563), float2(3.139, 11.997)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(3.139, 11.997), float2(3.029, 11.408)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(3.029, 11.408), float2(3.000, 10.811)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(3.000, 10.811), float2(3.038, 10.213)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(3.038, 10.213), float2(3.144, 9.624)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(3.144, 9.624), float2(3.344, 9.060)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(3.344, 9.060), float2(3.611, 8.524)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(3.611, 8.524), float2(3.944, 8.027)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(3.944, 8.027), float2(4.339, 7.578)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(4.339, 7.578), float2(4.789, 7.183)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(4.789, 7.183), float2(5.284, 6.846)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(5.284, 6.846), float2(5.822, 6.586)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(5.822, 6.586), float2(6.381, 6.380)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(6.381, 6.380), float2(6.582, 5.816)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(6.582, 5.816), float2(6.849, 5.281)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(6.849, 5.281), float2(7.185, 4.785)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(7.185, 4.785), float2(7.579, 4.335)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(7.579, 4.335), float2(8.029, 3.940)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(8.029, 3.940), float2(8.527, 3.608)); d = min(d, s.x); crossings += s.y;
    s = svgEdge(q, float2(8.527, 3.608), float2(9.064, 3.344)); d = min(d, s.x); crossings += s.y;
    float inside = step(0.5, mod(crossings, 2.0));
    return mix(d, -d, inside) * 0.02875;
}

float2 tileNormal(float2 p) {
    float e = 0.003;
    return normalize(float2(
        sdRoundBox(p + float2(e, 0.0), float2(0.345), 0.160) -
            sdRoundBox(p - float2(e, 0.0), float2(0.345), 0.160),
        sdRoundBox(p + float2(0.0, e), float2(0.345), 0.160) -
            sdRoundBox(p - float2(0.0, e), float2(0.345), 0.160)
    ));
}

half4 main(float2 fragCoord) {
    float2 halfRes = resolution * 0.5;
    float m = min(resolution.x, resolution.y);
    float2 p = (fragCoord - halfRes) / m;
    float aa = 2.2 / m;
    float pulse = 0.5 + 0.5 * sin(time * 0.85);
    float sweepCenter = mix(-0.70, 0.72, fract(time * 0.42));
    float sweepLine = p.x * 0.86 - p.y * 0.72;
    float glassSweep = exp(-pow((sweepLine - sweepCenter) * 7.4, 2.0));
    float3 glassPrism = prism(sweepLine * 1.85 + time * 0.32);

    float tileD = sdRoundBox(p, float2(0.345), 0.160);
    float tile = fill(tileD, aa);
    float shadowWide = fill(sdRoundBox(p - float2(0.000, 0.038), float2(0.350), 0.162), 0.060) * 0.20;
    float shadowTight = fill(sdRoundBox(p - float2(0.000, 0.020), float2(0.344), 0.158), 0.022) * 0.16;
    float glow = smoothstep(0.62, 0.12, length(p)) * (0.07 + pulse * 0.018);

    float3 col = float3(0.18, 0.34, 1.0) * glow;
    float alpha = max(glow * 0.55, max(shadowWide, shadowTight));
    col = mix(col, float3(0.08, 0.10, 0.18), shadowWide * 0.58);
    col = mix(col, float3(0.03, 0.04, 0.08), shadowTight * 0.45);

    float2 tn = tileNormal(p);
    float topLeftLight = clamp(dot(tn, normalize(float2(-0.75, -0.85))), 0.0, 1.0);
    float bottomRightShade = clamp(dot(tn, normalize(float2(0.72, 0.86))), 0.0, 1.0);
    float diag = clamp((p.x + p.y) * 1.15 + 0.5, 0.0, 1.0);
    float centerMilk = smoothstep(0.47, 0.02, length(p * float2(1.02, 0.94)));
    float3 tileCol = mix(float3(1.0, 1.0, 0.985), float3(0.865, 0.860, 0.835), diag);
    tileCol = mix(tileCol, float3(1.0), centerMilk * 0.36);
    tileCol += float3(0.11, 0.13, 0.18) * topLeftLight * rim(tileD, 0.000, 82.0);
    tileCol -= float3(0.095, 0.092, 0.078) * bottomRightShade * rim(tileD, 0.006, 62.0);

    float outerBright = rim(tileD, 0.000, 120.0) * topLeftLight;
    float innerBevel = rim(tileD, -0.015, 78.0) * tile;
    float cornerGlint = exp(-length((p - float2(-0.235, -0.248)) * float2(1.3, 1.0)) * 18.0) * tile;
    float lowerEdge = exp(-abs(p.y - 0.342) * 58.0) * smoothstep(-0.29, 0.24, p.x) * tile;
    float tileGlassEdge = clamp(rim(tileD, 0.000, 190.0) + rim(tileD, -0.024, 82.0) * tile, 0.0, 1.0);
    float tileGlass = tileGlassEdge * (0.48 + glassSweep * 1.25);
    tileCol += float3(0.30, 0.36, 0.46) * outerBright;
    tileCol += float3(0.10, 0.12, 0.16) * innerBevel * topLeftLight;
    tileCol += float3(0.18, 0.20, 0.23) * cornerGlint;
    tileCol -= float3(0.055, 0.052, 0.045) * lowerEdge;
    tileCol += glassPrism * tileGlass * 0.34;
    tileCol += float3(1.0, 1.0, 1.0) * tileGlassEdge * glassSweep * 0.38;
    col = mix(col, tileCol, tile);
    alpha = max(alpha, tile);

    float2 cp = p - float2(0.000, 0.010);
    float cloudD = cloudSdf(cp);
    float cloud = fill(cloudD, aa);
    float cloudShadow = fill(cloudD - 0.018, 0.026) * tile * 0.18;
    col = mix(col, float3(0.20, 0.22, 0.36), cloudShadow * (1.0 - cloud));

    float topMix = clamp(0.52 - cp.y * 3.15 + cp.x * 0.24 + pulse * 0.025, 0.0, 1.0);
    float midBand = exp(-pow((cp.y + 0.020) * 12.0, 2.0));
    float lowerPool = smoothstep(-0.02, 0.19, cp.y);
    float3 cloudCol = mix(float3(0.045, 0.130, 0.985), float3(0.42, 0.61, 1.0), 0.42);
    cloudCol = mix(cloudCol, float3(0.765, 0.515, 1.0), topMix * 0.78);
    cloudCol = mix(cloudCol, float3(0.120, 0.270, 1.0), midBand * 0.42);
    cloudCol = mix(cloudCol, float3(0.020, 0.085, 0.950), lowerPool * 0.48);

    float cloudTopRim = rim(cloudD, 0.000, 94.0) * smoothstep(0.040, -0.150, cp.y);
    float cloudLeftRim = rim(cloudD, 0.000, 72.0) * smoothstep(0.050, -0.210, cp.x) * 0.70;
    float cloudLowerShade = rim(cloudD, -0.004, 70.0) * smoothstep(0.010, 0.185, cp.y);
    float capGlow = exp(-length((cp - float2(-0.006, -0.124)) * float2(1.15, 1.0)) * 20.0);
    cloudCol += float3(0.24, 0.20, 0.34) * cloudTopRim;
    cloudCol += float3(0.09, 0.24, 0.50) * cloudLeftRim;
    cloudCol += float3(0.12, 0.06, 0.20) * capGlow;
    cloudCol -= float3(0.05, 0.08, 0.16) * cloudLowerShade;
    float glyphGlassEdge = clamp(rim(cloudD, 0.000, 118.0) + rim(cloudD, -0.010, 84.0) * cloud, 0.0, 1.0);
    float glyphSweep = glassSweep * (0.62 + 0.38 * smoothstep(0.16, -0.16, cp.x));
    cloudCol += prism(cp.x * 2.4 - cp.y * 1.6 + time * 0.36) * glyphGlassEdge * (0.34 + glyphSweep * 0.78);
    cloudCol += float3(0.94, 0.98, 1.0) * glyphGlassEdge * glyphSweep * 0.28;
    col = mix(col, cloudCol, cloud);

    float gtA = sdSegment(cp, float2(-0.132, -0.075), float2(-0.060, 0.020), 0.018);
    float gtB = sdSegment(cp, float2(-0.060, 0.020), float2(-0.132, 0.112), 0.018);
    float dash = sdSegment(cp, float2(0.060, 0.056), float2(0.176, 0.056), 0.016);
    float markD = min(min(gtA, gtB), dash);
    float markGlow = rim(markD, 0.000, 42.0) * cloud;
    float marks = fill(markD, aa) * cloud;
    col += float3(0.24, 0.48, 0.95) * markGlow * 0.20;
    col = mix(col, float3(0.955, 0.980, 1.0), marks);

    return half4(half3(clamp(col, 0.0, 1.0)), half(clamp(alpha, 0.0, 1.0)));
}
"""
