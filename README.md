# AGSL Engineer Playground

A curated, swipeable Jetpack Compose gallery for learning **Android Graphics Shading Language (AGSL)** by example, plus three full-screen "wow" showcase demos engineered for screen-recording.

## Why

AGSL ships with Android 13+ and lets you write GLSL/Skia-like fragment shaders that run as a Compose `ShaderBrush` or `RenderEffect`. There is no shortage of GLSL theory online — this project translates it into idiomatic Compose lessons on a real device.

## Audience

Android engineers who already write Compose UI and want a runnable, bite-sized intro to runtime shaders.

## Stack

- Kotlin / Jetpack Compose / Material3 (BOM `2026.02.01`)
- `minSdk = 33` (`RuntimeShader` requirement)
- `targetSdk = 36`, JVM 11
- Single-module app, no DI

## Build & Run

```bash
./gradlew :app:installDebug
```

Open on any Android 13+ device or emulator.

## Lesson Map

| Category  | Count | Highlights                                        |
|-----------|-------|---------------------------------------------------|
| Basics    | 6     | uniforms, fragCoord, gradients, polar coords      |
| SDF       | 6     | circle, rounded box, metaballs, breathing grid    |
| Noise     | 6     | hash, value noise, fBM, voronoi, plasma, lava     |
| Post-FX   | 6     | blur, aberration, ripple-tap, dissolve, glass     |
| Showcase  | 3     | liquid glass, aurora, raymarched sphere           |

## Showcase Recordings

Hosted out-of-repo (YouTube unlisted) — links coming once recorded.

## License

Apache-2.0 — see `LICENSE`.

## References

- [Android AGSL docs](https://developer.android.com/develop/ui/views/graphics/agsl)
- [The Book of Shaders](https://thebookofshaders.com/)
- [Inigo Quilez — articles](https://iquilezles.org/articles/)
