# AGSL Shader Demo Curriculum: Showcase-Worthy Learning Gallery

**Target Audience:** Android engineers learning AGSL (Android 13+)  
**Goal:** Progressive lessons + LinkedIn-worthy visual effects  
**Format:** 4 categories × 3-5 lessons each = ~20 teachable shaders, 5 "wow" showcase combos

---

## Category A: Fundamentals (Uniforms, Coordinates, Gradients)

| # | Title | New Concept | Visual Hook | AGSL Trick |
|---|-------|-------------|-------------|-----------|
| A1 | **Solid Color Uniform** | Pass CPU→GPU data | Red → Yellow → Blue animated | `uniform half4 color; return color;` |
| A2 | **Animated RGB Chase** | `iTime` uniform + modulo arithmetic | RGB triplet scrolls across | `return half4(sin(iTime) * 0.5 + 0.5, cos(iTime + 2.0), 0.3, 1.0);` |
| A3 | **Linear Gradient X** | UV coordinate mapping | Color sweep left→right | `return mix(half4(1,0,0,1), half4(0,1,0,1), fragCoord.x / iResolution.x);` |
| A4 | **Radial Gradient** | `length()` for distance from center | Bullseye: white core → dark edges | `float d = length(fragCoord - iResolution.xy*0.5); return mix(half4(1), half4(0), d/500.0);` |
| A5 | **Polar Coordinates + Rotation** | `atan()` for angle, time-driven rotation | Spinning color wheel | `float angle = atan(uv.y, uv.x) + iTime; return half4(sin(angle), cos(angle), 0.5, 1.0);` |
| A6 | **Animated Vignette** | `smoothstep()` for soft falloff | Glowing center, dim edges (breathing effect) | `float vignette = smoothstep(0.8, 0.3, length(uv - 0.5)); return mix(half4(0), half4(1), vignette);` |

---

## Category B: SDF Shapes & Patterns

| # | Title | New Concept | Visual Hook | AGSL Trick |
|---|-------|-------------|-------------|-----------|
| B1 | **Circle SDF** | Basic signed distance function | Glowing circle in dark field | `float sdf = length(p - center) - radius; return sdf < 0.0 ? bright : dark;` |
| B2 | **Box SDF + Rounded Corners** | `max()` for box, subtract radius to round | Smooth-edged rectangle glow | `vec2 d = abs(p - center) - (size * 0.5); float sdf = length(max(d, 0.0)) - radius;` |
| B3 | **Smooth-Min Blobs (Metaballs)** | Interpolate SDFs of multiple circles | Two fuzzy circles morph & merge | `float blob1 = sphere(p, c1, r); float blob2 = sphere(p, c2, r); return smoothMin(blob1, blob2, k);` |
| B4 | **Checkerboard Pattern** | `mod()` for tiling, `step()` for edges | Infinite black-white squares (zoomable) | `return step(0.5, mod(p.x, 1.0)) != step(0.5, mod(p.y, 1.0)) ? dark : light;` |
| B5 | **Breathing Grid** | Scale checkerboard over time | Grid expands/contracts sinusoidally | `float scale = 10.0 + 5.0 * sin(iTime); float d = mod(p * scale, 2.0); return d < 1.0 ? color : dark;` |
| B6 | **Isolines / Contour Rings** | `fract()` to extract fractional part of SDF | Concentric rings around shape (depth illusion) | `float d = length(p - center) - radius; float rings = fract(d * 5.0); return mix(dark, bright, rings);` |

---

## Category C: Noise & Procedural Textures

| # | Title | New Concept | Visual Hook | AGSL Trick |
|---|-------|-------------|-------------|-----------|
| C1 | **Hash-Based Value Noise (1D→2D)** | Pseudo-random seed per grid cell | Grayscale cloudy texture | `float noise = fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);` |
| C2 | **Fractal Brownian Motion (fBM)** | Stack octaves of noise at scales | Detailed organic landscape-like texture | `float fbm = 0.0; for(int i=0; i<4; i++) { fbm += noise(p * freq) / freq; freq *= 2.0; }` |
| C3 | **Voronoi Cells (Cellular Noise)** | Distance to nearest seed point | "Cellular life" animated → seeds morph over time | `float d = dist_to_nearest(p); return d < threshold ? cellColor : borderColor;` |
| C4 | **Plasma / Electric Energy** | fBM + trig warping + color remapping | Flowing pink/purple/cyan energy waves | `float f = fbm(p + sin(iTime)); return mix(color1, color2, sin(f * 3.14159));` |
| C5 | **Domain Warping (Inigo Classic)** | Displace noise input using other noise | Swirly turbulent clouds + Lava-like flow | `p += noise(p + iTime) * 0.5; return fbm(p);` — *[Inigo Quilez technique](https://iquilezles.org/articles/warp/)* |
| C6 | **Animated Lava / Fire** | Blend domain warping + hot-to-cold color ramp | Glowing molten effect, bubbling top layer | `float lava = fbm(warp(p, iTime)); return mix(darkRed, brightYellow, lava);` |

---

## Category D: Image Post-Processing on Composables (RenderEffect)

| # | Title | New Concept | Visual Hook | AGSL Trick |
|---|-------|-------------|-------------|-----------|
| D1 | **Gaussian-ish Blur on Card** | Sampler2D input texture, multi-tap blur | Real-time frosted-glass card effect | `half4 blur = (sample(uv-offset1) + sample(uv) + sample(uv+offset2)) / 3.0;` |
| D2 | **Chromatic Aberration on Tap** | Offset RGB channels differently | Red/blue split streaks, iOS-ish | `half r = sample(uv + offset).r; half b = sample(uv - offset).b; return half4(r,g,b,1);` |
| D3 | **Ripple on Tap** | Touch position drives wave center + sine deformation | Concentric rings expand from finger tap | `float d = distance(fragCoord, touchPos); float wave = sin(d - iTime * speed) / (d + 0.1);` |
| D4 | **Dissolve / Burn Transition** | Noise thresholding between two samplers | Screen burn effect, transition between UI states | `float n = noise(fragCoord); return mix(tex1, tex2, step(iProgress, n));` |
| D5 | **Displacement Map (Liquid Glass)** | Sample normal map, perturb UV | Refracted photo through rippling water overlay | `vec2 disp = sample(normalMap, uv).xy * strength; return sample(inputTexture, uv + disp);` |
| D6 | **Pixelate / Mosaic** | Quantize UV to grid cells | 8-bit pixel art effect, smoothly adjustable | `vec2 pixelUV = floor(fragCoord / pixelSize) * pixelSize; return sample(tex, pixelUV);` |

---

## Showcase / "Wow" Lessons (LinkedIn-Worthy Combos)

**Strategy:** Combine 2–3 techniques from A/B/C/D; target <100 AGSL LOC; record 2–3 sec demos.

### S1: **Liquid Glass / iOS 26 Refraction Overlay**
- **Visual Hook:** Frosted-glass card slides over photo; background warps + reflects realistically  
- **Core Techniques:** Displacement map + chromatic abberation + multi-tap blur  
- **Key Uniforms:** `sampler2D inputTexture, normalMap; float strength, blurAmount; vec2 touchPos`  
- **AGSL Complexity:** 3/5 (blur is GPU-friendly; refraction needs careful UV offset)  
- **Prior Art:** [react-native-liquid-glass](https://github.com/uginy/react-native-liquid-glass), [LiquidGlassKit iOS](https://github.com/DnV1eX/LiquidGlassKit)  
- **Teaching Payload:** Combining displacement + blur to fake refraction; how to preserve performance.

### S2: **Aurora Borealis / Ribbon Shader**
- **Visual Hook:** Flowing colored bands dance across screen (green→purple→cyan), organic morph  
- **Core Techniques:** fBM + domain warping + animated color ramp + polar coordinates  
- **Key Uniforms:** `float iTime; vec3 colorA, colorB, colorC; float flow_speed`  
- **AGSL Complexity:** 3/5 (fBM + warp is moderate; color cycling is simple)  
- **Prior Art:** Domain warping [Inigo Quilez](https://iquilezles.org/articles/warp/); fBM [The Book of Shaders](https://thebookofshaders.com/13/)  
- **Teaching Payload:** Noise layering + time-driven animation; visual proof that proceduralism = infinite variation.

### S3: **Ray-Marched Sphere with Minimal Lighting**
- **Visual Hook:** 3D sphere floats in center, lit by single directional light + soft ambient (shiny, impressive for 2D shader)  
- **Core Techniques:** Sphere SDF + ray marching loop + normal calculation (finite difference) + Phong/Lambertian lighting  
- **Key Uniforms:** `float iTime; vec3 lightDir, lightColor, ambientColor; float shininess`  
- **AGSL Complexity:** 4/5 (raymarching loop + normal calc = intermediate; worth learning)  
- **Prior Art:** [Jamie Wong's primer](https://jamie-wong.com/2016/07/15/ray-marching-signed-distance-functions/); [Inigo Quilez SDF article](https://iquilezles.org/articles/distfunctions/)  
- **Teaching Payload:** Proof that 3D scenes run in a 2D fragment shader; marching + normal estimation unlocks all SDF techniques.

### S4: **Audio-Reactive / Touch-Trail Waveform**
- **Visual Hook:** Vertical bars respond to audio freq OR finger trails leave glowing ribbons  
- **Core Techniques:** Ripple shader + time-series history OR audio sampler input + animated color gradient  
- **Key Uniforms:** `sampler2D waveformTexture (or audio history buffer); float magnitude[16]; float iTime; float touchDamping`  
- **AGSL Complexity:** 2/5 (ripple is simpler); 4/5 (if audio input via external buffer)  
- **Prior Art:** [DynamicVisualEffectsAGSL](https://github.com/JumpingKeyCaps/DynamicVisualEffectsAGSL) on GitHub  
- **Teaching Payload:** Bridging Compose→shader via uniforms; touch/sensor→GPU feedback loop.

### S5: **Animated Gradient Mesh / Aurora Splash Background**
- **Visual Hook:** Multi-point gradient mesh with 3–5 animated control points; soft blurs between them → "aurora aurora" loading screen  
- **Core Techniques:** Multi-center radial gradients (overlaid/blended) + time-driven position oscillation + fBM overlay for turbulence  
- **Key Uniforms:** `vec2 points[5]; half4 colors[5]; float amplitudes[5]; float iTime; float turbulence_strength`  
- **AGSL Complexity:** 3/5 (loop over points + blend; fBM adds polish)  
- **Prior Art:** Animated gradient mesh concept from [KINTO Tech Blog](https://blog.kinto-technologies.com/posts/2024-12-15-AGSL/); research above  
- **Teaching Payload:** Parametric color control from CPU; Compose Brush or RenderEffect setup for full-screen backgrounds.

---

## Technical Foundation (Assumed Knowledge)

- **AGSL Syntax:** `main(vec2 fragCoord)` entry point, `half4` return type  
- **Built-ins:** `length(), distance(), sin(), cos(), atan(), smoothstep(), mix(), step(), mod(), fract(), abs()`  
- **Uniforms Pattern:** `uniform float iTime; uniform vec2 iResolution; uniform sampler2D iChannel0;`  
- **Compose Integration:** `createRuntimeShaderEffect()` → `graphicsLayer { }` on Composable  
- **Performance Assumption:** Students target 60 FPS; introduce loop unrolling / octave limits as complexity grows.

---

## Curriculum Flow (Suggested Progression)

**Week 1–2:** A1→A4 (warm up: math + uniform passing)  
**Week 3:** A5–A6 + B1–B2 (coordinate systems + shape rendering)  
**Week 4:** B3–B6 (SDF blending + patterns)  
**Week 5–6:** C1–C3 (noise fundamentals, cellular automata)  
**Week 7:** C4–C6 (procedural + animation)  
**Week 8:** D1–D3 (post-processing basics)  
**Week 9:** D4–D6 (advanced filters)  
**Week 10:** S1–S5 (showcase + integration, 1 demo per session)

Each lesson: 30–50 line shader + 2-min explainer + runnable Compose example + "how to record for LinkedIn" note.

---

## Sources & Reference Architecture

**Fundamentals:**
- [AGSL Official Docs](https://developer.android.com/develop/ui/views/graphics/agsl)
- [AGSL Quick Reference](https://developer.android.com/develop/ui/views/graphics/agsl/agsl-quick-reference)
- [Chet Haase: AGSL Made in the Shade](https://medium.com/androiddevelopers/agsl-made-in-the-shade-r-7d06d14fe02a)

**SDF & Raymarching:**
- [Inigo Quilez: Signed Distance Functions](https://iquilezles.org/articles/distfunctions/) (3D/2D SDFs, exact formulas)
- [Inigo Quilez: 2D Signed Distance Fields](https://iquilezles.org/articles/distfunctions2d/)
- [Jamie Wong: Ray Marching & SDFs](https://jamie-wong.com/2016/07/15/ray-marching-signed-distance-functions/)

**Noise & Procedural:**
- [The Book of Shaders: Noise](https://thebookofshaders.com/11/)
- [The Book of Shaders: More Noise](https://thebookofshaders.com/12/)
- [The Book of Shaders: fBM](https://thebookofshaders.com/13/)
- [Inigo Quilez: Domain Warping](https://iquilezles.org/articles/warp/)

**Compose + AGSL Implementations:**
- [drinkthestars/shady](https://github.com/drinkthestars/shady) — collection of AGSL shaders in Compose
- [Carrieukie/AGSL-Playground](https://github.com/Carrieukie/AGSL-Playground)
- [JumpingKeyCaps/DynamicVisualEffectsAGSL](https://github.com/JumpingKeyCaps/DynamicVisualEffectsAGSL) — touch-reactive + sensor integration

**Post-Processing & Glass Effects:**
- [NVIDIA GPU Gems: Refraction Simulation](https://developer.nvidia.com/gpugems/gpugems2/part-ii-shading-lighting-and-shadows/chapter-19-generic-refraction-simulation)
- [Liquid Glass iOS Effect Explanation](https://medium.com/@aghajari/liquid-glass-ios-effect-explanation-dabadd6414ae)
- [react-native-liquid-glass](https://github.com/uginy/react-native-liquid-glass)
- [Mortd3kay/liquid-glass-compose](https://github.com/Mortd3kay/liquid-glass-compose)

**Voronoi & Cellular Noise:**
- [The Book of Shaders: Cellular Noise](https://thebookofshaders.com/12/) (Worley/Voronoi)
- [Godot Shaders: Voronoi](https://godotshaders.com/snippet/voronoi/)

**Shadertoy & Community:**
- [Shadertoy.com](https://www.shadertoy.com/) — live shader editor & portfolio; sort by trending
- [Codrops: Shape Lens Blur with SDFs](https://tympanus.net/codrops/2024/06/12/shape-lens-blur-effect-with-sdfs-and-webgl/)
- [Codrops: WebGL Shaders with GSAP Animation](https://tympanus.net/codrops/2025/10/08/how-to-animate-webgl-shaders-with-gsap-ripples-reveals-and-dynamic-blur-effects/)

---

## Unresolved Questions

1. **Audio Input Integration:** How to feed audio frequency data (via ExoPlayer/AudioTrack) to AGSL shaders as sampler2D or uniform array? (Likely CPU→GPU buffer sync; not yet seen production AGSL example.)
2. **Multiple Touch Points:** AGSL doesn't expose multi-touch natively; does drinkthestars/shady or another repo handle this? (Research: may need Compose state → uniform array.)
3. **Metaball SDF Exact Formula:** Smooth-min blend strength `k` parameter ranges & best practices for Android GPU (varies by device)?
4. **Tessellation / Multi-Pass Shaders:** Can AGSL do vertex shaders, or is it fragment-only? (Likely fragment-only; confirm.)
5. **Shadertoy Port Licensing:** Which Shadertoy shaders are MIT/GPL-friendly for Android app shipping?
6. **Performance Baseline:** Typical FPS drop for: fBM (4 octaves), raymarching (50 iterations), blur (9-tap) on Pixel 8 / Galaxy S24?

---

**Report Generated:** 2026-05-05  
**Token Estimate:** ~1200 AGSL LOC across all 20+ lessons; ~500 lines Compose integration.
