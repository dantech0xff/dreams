# Production-Grade Refactor Surface Map — Dreams AGSL Playground

## 1. Static Singletons / Global State

| File | Note |
|------|------|
| `/Users/dan/Desktop/Development/Android-Kotlin/dreams/app/src/main/java/com/dantech/dreams/data/lesson/LessonRegistry.kt` | `object LessonRegistry` holds mutable `all: List<LessonModel>`. Consumers: `LessonCategory.byCategory()`, `byId()`, `bootstrap()` called from `DreamsApp.onCreate()`. Would become `LessonRepository` with DI. |

## 2. Navigation Graph

| File | Note |
|------|------|
| `/Users/dan/Desktop/Development/Android-Kotlin/dreams/app/src/main/java/com/dantech/dreams/ui/playground/PlaygroundNavHost.kt` | `rememberNavController()` + `NavHost`. Routes: `LANDING`, `GALLERY`, `LESSON/{id}`, `SHOWCASE/{id}`. All composable routes hardcoded. Target: Nav3 with type-safe routing. |

## 3. Screen-Level State Holders (Candidate ViewModels)

| File | State Variables | Note |
|------|-----------------|------|
| `/Users/dan/Desktop/Development/Android-Kotlin/dreams/app/src/main/java/com/dantech/dreams/ui/playground/gallery/GalleryScreen.kt` | `selected: Int` (tab index) | Holds category tab selection; could be ViewModel. |
| `/Users/dan/Desktop/Development/Android-Kotlin/dreams/app/src/main/java/com/dantech/dreams/ui/playground/lesson/LessonDetailScreen.kt` | `controlValues: SnapshotStateMap<String, Any>` (shader uniforms), `touchPosUv`, `touchTime` | Lesson state, shader control values. Prime ViewModel candidate. |
| `/Users/dan/Desktop/Development/Android-Kotlin/dreams/app/src/main/java/com/dantech/dreams/ui/playground/landing/LandingScreen.kt` | `aboutOpen: Boolean` (sheet state) | Minor; could be composable-local. |
| `/Users/dan/Desktop/Development/Android-Kotlin/dreams/app/src/main/java/com/dantech/dreams/ui/playground/showcase/ShowcaseScreen.kt` | `hideUi: Boolean` | Toggle UI visibility; composable-local OK. |

## 4. Application Class / Entry Point

| File | Detail |
|------|--------|
| `/Users/dan/Desktop/Development/Android-Kotlin/dreams/app/src/main/java/com/dantech/dreams/DreamsApp.kt` | Extends `Application`. Calls `LessonRegistry.bootstrap()` + validation loop in DEBUG. No lifecycle awareness beyond onCreate. |
| `/Users/dan/Desktop/Development/Android-Kotlin/dreams/app/src/main/java/com/dantech/dreams/MainActivity.kt` | `ComponentActivity`. Calls `enableEdgeToEdge()`, wraps `PlaygroundApp()` composable in `DreamsTheme`. No state holder. |

## 5. Lesson Data Model

| File | Note |
|------|------|
| `/Users/dan/Desktop/Development/Android-Kotlin/dreams/app/src/main/java/com/dantech/dreams/data/lesson/LessonModel.kt` | `@Immutable data class`: id, title, category, complexity, conceptIntro, agslSource, controls (list), renderMode, postEffectContent, customPreview. Would feed `LessonRepository`. |
| `/Users/dan/Desktop/Development/Android-Kotlin/dreams/app/src/main/java/com/dantech/dreams/data/lesson/LessonControl.kt` | Sealed interface: `FloatRange`, `ColorPicker`. Uniforms + defaults. Schema-driven uniform binding. |
| `/Users/dan/Desktop/Development/Android-Kotlin/dreams/app/src/main/java/com/dantech/dreams/data/lesson/LessonCategory.kt` | Enum: BASICS, SDF, NOISE, POSTFX, SHOWCASE. No DB backing; in-memory only. |

## 6. Shader Bootstrap Files

| File | Registers | Called From |
|------|-----------|-------------|
| `/Users/dan/Desktop/Development/Android-Kotlin/dreams/app/src/main/java/com/dantech/dreams/shaders/basics/BasicsBootstrap.kt` | 6 lessons: SolidColor, AnimatedColor, LinearGradient, RadialGradient, PolarCoords, AnimatedVignette | `LessonRegistry.bootstrap()` |
| `/Users/dan/Desktop/Development/Android-Kotlin/dreams/app/src/main/java/com/dantech/dreams/shaders/sdf/SdfBootstrap.kt` | 6 SDF lessons (CircleSdf, RoundedBoxSdf, Metaballs, Checkerboard, BreathingGrid, Isolines) | `LessonRegistry.bootstrap()` |
| `/Users/dan/Desktop/Development/Android-Kotlin/dreams/app/src/main/java/com/dantech/dreams/shaders/noise/NoiseBootstrap.kt` | 6 noise lessons (HashLesson, ValueNoise, FbmClouds, VoronoiCells, Plasma, WarpedLava) | `LessonRegistry.bootstrap()` |
| `/Users/dan/Desktop/Development/Android-Kotlin/dreams/app/src/main/java/com/dantech/dreams/shaders/posteffect/PostFxBootstrap.kt` | 6 post-FX lessons (Blur, ChromaticAberration, RippleTap, Dissolve, DisplacementGlass, Pixelate) | `LessonRegistry.bootstrap()` |
| `/Users/dan/Desktop/Development/Android-Kotlin/dreams/app/src/main/java/com/dantech/dreams/shaders/showcase/ShowcaseBootstrap.kt` | 5 showcase lessons (AuroraRibbons, RaymarchedSphere, LiquidGlass, RippleOnPond, RippleOnTap) | `LessonRegistry.bootstrap()` |

## 7. AGSL Utilities

| File | Dependencies / Notes |
|------|-------|
| `/Users/dan/Desktop/Development/Android-Kotlin/dreams/app/src/main/java/com/dantech/dreams/shaders/agsl/RuntimeShaderUtils.kt` | `rememberRuntimeShader()`, `rememberShaderTime()`. No SDK version gating needed (AGSL is API 33+, minSdk already 33). |
| `/Users/dan/Desktop/Development/Android-Kotlin/dreams/app/src/main/java/com/dantech/dreams/shaders/agsl/ShaderModifiers.kt` | `runtimeShaderEffect()` modifier. Uses `NativeRenderEffect.createRuntimeShaderEffect()`, `CompositingStrategy.Offscreen`. API 29+ (inline, no check needed). |
| `/Users/dan/Desktop/Development/Android-Kotlin/dreams/app/src/main/java/com/dantech/dreams/ui/playground/common/ShaderTimeUniform.kt` | `rememberShaderTime()` with regex-detection of uniform declarations. Animated frame-loop via `withFrameNanos`. |
| `/Users/dan/Desktop/Development/Android-Kotlin/dreams/app/src/main/java/com/dantech/dreams/ui/playground/common/AgslCanvas.kt` | `AgslBrushCanvas()` + `AgslRenderEffectCanvas()`. Declares resolution auto-detection, shader compilation error handling. |

## 8. Build Configuration

| File | Key Versions |
|------|--------|
| `/Users/dan/Desktop/Development/Android-Kotlin/dreams/gradle/libs.versions.toml` | AGP 9.1.1, Kotlin 2.2.10, Compose BOM 2026.02.01, Nav 2.8.5, no Koin/Hilt yet. MinSdk 33, TargetSdk 36. |
| `/Users/dan/Desktop/Development/Android-Kotlin/dreams/app/build.gradle.kts` | `compose = true`, `buildConfig = true`. No custom test runners or plugins. Deps: androidx-core, lifecycle, activity-compose, material3, navigation-compose, coil, kotlinx-collections-immutable. |
| `settings.gradle.kts` | Single-module app. |

## 9. Tests

| Path | Status |
|------|--------|
| `/Users/dan/Desktop/Development/Android-Kotlin/dreams/app/src/test/java/com/dantech/dreams/ExampleUnitTest.kt` | Scaffold only (JUnit stub). |
| `/Users/dan/Desktop/Development/Android-Kotlin/dreams/app/src/test/java/com/dantech/dreams/data/lesson/LessonRegistryTest.kt` | Test file exists; needs inspection. |
| `/Users/dan/Desktop/Development/Android-Kotlin/dreams/app/src/androidTest/java/com/dantech/dreams/ExampleInstrumentedTest.kt` | Scaffold only (AndroidX Test stub). |

## 10. Resources / Theme

| File | Note |
|------|------|
| `/Users/dan/Desktop/Development/Android-Kotlin/dreams/app/src/main/res/values/colors.xml` | 7 static colors (purple_200/500/700, teal_200/700, black, white). No semantic naming. |
| `/Users/dan/Desktop/Development/Android-Kotlin/dreams/app/src/main/java/com/dantech/dreams/ui/theme/Color.kt` | 6 Compose colors: Purple80/40, PurpleGrey80/40, Pink80/40. Hardcoded M3 palette. Would become design tokens. |
| `/Users/dan/Desktop/Development/Android-Kotlin/dreams/app/src/main/java/com/dantech/dreams/ui/theme/Type.kt` | `Typography` with bodyLarge default. Other styles commented out. Minimal customization. |
| `/Users/dan/Desktop/Development/Android-Kotlin/dreams/app/src/main/java/com/dantech/dreams/ui/theme/Theme.kt` | `DreamsTheme()` with dynamic color (API 31+). Applies M3 color + typography to `MaterialTheme`. |
| `/Users/dan/Desktop/Development/Android-Kotlin/dreams/app/src/main/AndroidManifest.xml` | App class: `.DreamsApp`. Activity: `.MainActivity`. No permissions, no custom intent-filters. |

---

## Status / Summary / Concerns

**Status:** DONE

**Summary:**  
46-file Kotlin codebase for shader education app. Single module, Compose-only UI. No DI framework (Koin/Hilt), no typed navigation (Nav3), no ViewModels. Global lesson registry holds all state; screens use remember/mutableState for local control/UI toggles. Bootstrap pattern registers 23 AGSL lessons via singleton touch() calls. Shader utilities lean on runtime detection (regex for uniform declarations) to avoid UnsafeMutableState and CheckJNI crashes.

**Refactor Surface:**
- **DI**: Inject `LessonRepository` (wraps registry), shader compile service, theme/design tokens.
- **Navigation**: Replace string routes with Nav3 type-safe routes (sealed classes or inline type mapping).
- **State**: Promote `LessonDetailScreen` + `GalleryScreen` state to ViewModels. Use `remember { viewModel() }` to persist across recomposition.
- **Theme**: Consolidate M3 colors + typography into semantic design-system tokens. Move `colors.xml` static assets to Compose-first approach.
- **Tests**: Expand from 2 stubs. Add unit tests for `LessonRegistry`, integration tests for nav graph.

**Blockers/Dependencies:**  
None immediate. Kotlin version, compose BOM, and minSdk already modern. Build is clean. Ready for gradual refactor (DI → Nav3 → ViewModels in sequence).

