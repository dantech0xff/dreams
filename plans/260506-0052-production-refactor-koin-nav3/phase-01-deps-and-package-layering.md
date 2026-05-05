# Phase 01 — Deps + Package Layering

## 1. Context Links
- Parent: [plan.md](plan.md)
- Inputs: `scout/scout-01-refactor-surface.md` §1, §6, §8
- Docs: Koin 4 setup https://insert-koin.io/docs/quickstart/android-compose | Nav3 https://developer.android.com/guide/navigation/navigation-3 | Compose BOM 2026.02.01 (already pinned)
- Blocks: phase-02, phase-03, phase-04 (everything else needs new deps + new package roots)

## 2. Overview
- **Date:** 2026-05-06
- **Description:** Add Koin BOM, koin-androidx-compose, koin-test, navigation3-runtime, navigation3-ui, lifecycle-viewmodel-navigation3, datastore-preferences, kotlinx-serialization, turbine, kotlinx-coroutines-test to `libs.versions.toml`. Wire kotlinx.serialization plugin. Move existing source into layered packages: `core/`, `data/`, `domain/`, `ui/feature/`. Compile clean.
- **Priority:** P1 (blocker for all later phases)
- **Implementation status:** pending
- **Review status:** pending

## 3. Key Insights
- Single Gradle module (locked decision) → layering is package-only, no `:core` `:data` modules.
- Existing `data/lesson/*` already lives under correct root — only re-organize internal subpackages.
- `shaders/**` is pure data (lesson definitions). Move under `data/lesson/source/{basics,sdf,noise,posteffect,showcase}/` so DI can wire registration without touching UI.
- AGSL canvas helpers (`ShaderModifiers.kt`, `RuntimeShaderUtils.kt`) are infra → move under `core/agsl/`.
- `kotlinx-serialization` plugin needed for Nav3 `@Serializable` Route classes (phase-04) AND DataStore JSON overrides (phase-05) — add now.

## 4. Requirements

### Functional
- Project builds with new deps; no class is unresolved post-move.
- Existing UI (PlaygroundApp, all screens, all 23 lessons) remains functional.
- All `import com.dantech.dreams...` statements updated to new paths.

### Non-Functional
- Zero behavioral diff. This is a pure mechanical phase.
- Version catalog uses `[versions]` refs, no inline strings.

## 5. Architecture

```
com.dantech.dreams/
├── core/
│   ├── agsl/            ← RuntimeShaderUtils.kt, ShaderModifiers.kt
│   └── motion/          ← (placeholder for phase-06 reduced-motion util)
├── data/
│   ├── lesson/          ← LessonModel, LessonControl, LessonCategory, LessonRegistry (impl)
│   │   └── source/      ← BasicsBootstrap, SdfBootstrap, NoiseBootstrap, PostFxBootstrap, ShowcaseBootstrap
│   └── prefs/           ← (placeholder for phase-05 UserPrefsRepository)
├── domain/
│   └── lesson/          ← LessonRepository interface (added phase-02)
├── ui/
│   ├── theme/           ← Theme, Color, Type, Tokens (phase-08)
│   └── feature/
│       ├── landing/     ← LandingScreen, AboutAgslSheet
│       ├── gallery/     ← GalleryScreen, LessonCard
│       ├── lesson/      ← LessonDetailScreen, ParameterSlider, AgslSourceViewer
│       ├── showcase/    ← ShowcaseScreen, RecordHintBanner
│       └── nav/         ← Routes + NavHost (becomes Nav3 in phase-04)
├── DreamsApp.kt
└── MainActivity.kt
```

## 6. Related Code Files

### Modify
- `gradle/libs.versions.toml` — add versions + libraries
- `app/build.gradle.kts` — apply `kotlin("plugin.serialization")`, add deps
- `build.gradle.kts` (root, if exists) — register serialization plugin classpath via `plugins {}` if needed
- All ~46 source files: package declaration + imports updated to new layout

### Create
- New directories per §5 (Kotlin packages — directories created implicitly by file moves)

### Delete
- None this phase

## 7. Implementation Steps

1. **Edit `gradle/libs.versions.toml` `[versions]`:** add
   ```toml
   koinBom = "4.0.0"
   navigation3 = "1.1.0-rc01"
   lifecycleViewmodelNav3 = "1.0.0-alpha01"
   datastore = "1.1.1"
   kotlinxSerialization = "1.7.3"
   turbine = "1.2.0"
   coroutinesTest = "1.9.0"
   ```
2. **Edit `gradle/libs.versions.toml` `[libraries]`:** add
   ```toml
   koin-bom = { group = "io.insert-koin", name = "koin-bom", version.ref = "koinBom" }
   koin-core = { group = "io.insert-koin", name = "koin-core" }
   koin-android = { group = "io.insert-koin", name = "koin-android" }
   koin-androidx-compose = { group = "io.insert-koin", name = "koin-androidx-compose" }
   koin-test = { group = "io.insert-koin", name = "koin-test" }
   koin-test-junit4 = { group = "io.insert-koin", name = "koin-test-junit4" }
   androidx-navigation3-runtime = { group = "androidx.navigation3", name = "navigation3-runtime", version.ref = "navigation3" }
   androidx-navigation3-ui = { group = "androidx.navigation3", name = "navigation3-ui", version.ref = "navigation3" }
   androidx-lifecycle-viewmodel-navigation3 = { group = "org.jetbrains.androidx.lifecycle", name = "lifecycle-viewmodel-navigation3", version.ref = "lifecycleViewmodelNav3" }
   androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
   kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
   turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
   kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutinesTest" }
   ```
3. **Edit `[plugins]`:** add `kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }`
4. **Edit `app/build.gradle.kts`:** apply `alias(libs.plugins.kotlin.serialization)`. In `dependencies`:
   - `implementation(platform(libs.koin.bom))`
   - `implementation(libs.koin.androidx.compose)`
   - `implementation(libs.koin.android)`
   - `implementation(libs.androidx.navigation3.runtime)`
   - `implementation(libs.androidx.navigation3.ui)`
   - `implementation(libs.androidx.lifecycle.viewmodel.navigation3)`
   - `implementation(libs.androidx.datastore.preferences)`
   - `implementation(libs.kotlinx.serialization.json)`
   - REMOVE `implementation(libs.androidx.navigation.compose)` (deferred to phase-04 cutover but mark TODO)
   - `testImplementation(libs.koin.test)`, `testImplementation(libs.koin.test.junit4)`, `testImplementation(libs.turbine)`, `testImplementation(libs.kotlinx.coroutines.test)`
5. **Sync Gradle.** Address any version conflicts. Confirm Compose BOM still resolves Compose UI 1.8.x for SharedTransitionLayout.
6. **Move sources** (use IDE refactor → "Move package"):
   - `shaders/agsl/RuntimeShaderUtils.kt` → `core/agsl/RuntimeShaderUtils.kt`
   - `shaders/agsl/ShaderModifiers.kt` → `core/agsl/ShaderModifiers.kt`
   - `ui/playground/common/AgslCanvas.kt` → `ui/feature/common/AgslCanvas.kt`
   - `ui/playground/common/ShaderTimeUniform.kt` → `ui/feature/common/ShaderTimeUniform.kt`
   - `shaders/{basics,sdf,noise,posteffect,showcase}/*Bootstrap.kt` → `data/lesson/source/{...}/*Bootstrap.kt`
   - `shaders/{basics,sdf,noise,posteffect,showcase}/*.kt` (lesson defs) → same `data/lesson/source/{...}/`
   - `ui/playground/landing/*` → `ui/feature/landing/*`
   - `ui/playground/gallery/*` → `ui/feature/gallery/*`
   - `ui/playground/lesson/*` → `ui/feature/lesson/*`
   - `ui/playground/showcase/*` → `ui/feature/showcase/*`
   - `ui/playground/PlaygroundNavHost.kt` → `ui/feature/nav/PlaygroundNavHost.kt`
7. **Run `./gradlew :app:compileDebugKotlin`.** Fix any missed import. Run `./gradlew :app:installDebug` to confirm runtime parity.

## 8. Todo
- [ ] versions.toml updated
- [ ] build.gradle.kts deps + plugin applied
- [ ] Gradle sync clean
- [ ] Source files moved per §5
- [ ] Imports / package declarations updated
- [ ] `./gradlew :app:installDebug` succeeds
- [ ] App launches, all 23 lessons still in registry (`LessonRegistryTest` still green)

## 9. Success Criteria
- Single command `./gradlew :app:installDebug` succeeds.
- `./gradlew test` runs `LessonRegistryTest` green.
- Manual: open app → landing → gallery → tap any lesson → renders.
- `git diff --stat` shows file moves only; behavior unchanged.

## 10. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Nav3 alpha version of `lifecycle-viewmodel-navigation3` not on Maven Central yet | Med | Med | Verify on first sync; fall back to manual ViewModelStoreOwner plumbing if missing |
| Compose BOM 2026.02.01 conflicts with Koin BOM transitive Compose pin | Low | Med | Use `platform(libs.androidx.compose.bom)` AFTER `platform(libs.koin.bom)` so Compose wins for `androidx.compose.*` |
| Kotlin-serialization plugin needs Kotlin 2.2.10 alignment | Low | Low | `version.ref = "kotlin"` in plugins block guarantees alignment |
| File move breaks unmoved imports across 46 files | Med | Low | IDE-driven move + grep `com.dantech.dreams.shaders` and `ui.playground` remnants |

## 11. Security Considerations
- None. No new permissions, no network, no IPC. DataStore + serialization arrive in later phases (their security covered there).

## 12. Next Steps
- Phase-02 (Koin DI bootstrap) consumes the new layered packages.

## Unresolved Questions
- **Koin 4.x BOM pin:** Does Compose BOM 2026.02.01 pin a transitive `androidx.compose.runtime` version that conflicts with Koin 4.0.0's expectations? Verify on first Gradle sync — if mismatch, pin koin-androidx-compose explicitly without BOM.
- **`lifecycle-viewmodel-navigation3` artifact group:** Researcher-02 cites `org.jetbrains.androidx.lifecycle` (KMP) — confirm Android-only vs KMP variant after sync; switch to `androidx.lifecycle:lifecycle-viewmodel-navigation3` if AndroidX direct artifact exists.
