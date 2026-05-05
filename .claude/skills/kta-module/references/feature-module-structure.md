# Feature Module Structure

A regular Android library that hosts a feature (UI + ViewModel + nav route) but ships in the base APK. Pick this when the user wants modular separation but is certain Play Feature Delivery (on-demand/conditional/instant) is not needed.

> The templates below are **vanilla AGP defaults**. Adapt to the project's actual conventions discovered in Step 0 of SKILL.md (convention plugins, version-catalog aliases, namespace pattern, sibling deps, JVM/SDK levels, DI/nav stack).

## How This Differs From a Plain Library

- Owns a navigation route (and the screen/composable behind it)
- Typically depends on Compose, navigation, and the project's DI of choice
- Registered in the application module via standard `implementation(project(":..."))` — **not** via `dynamicFeatures`

## File Layout (illustrative — adapt to project conventions)

```
{name}/
├── build.gradle.kts
└── src/main/
    ├── AndroidManifest.xml
    └── kotlin/<package-path>/
        ├── di/         (if project uses module-level DI registration)
        ├── navigation/ (if project has a feature-level nav provider pattern)
        ├── ui/         (Route, Screen, composables)
        └── viewmodel/
```

If the project's existing features use a different layout (e.g. `presentation/`, `screens/`, no `di/` folder because Hilt scans automatically), match that layout instead.

## 1. `build.gradle.kts` (vanilla template)

```kotlin
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")  // if Compose UI
    // Add only as the project requires:
    // id("com.google.devtools.ksp")
    // id("com.google.dagger.hilt.android")
    // id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "<your.package.namespace>"
    compileSdk = <project compileSdk>

    defaultConfig {
        minSdk = <project minSdk>
    }

    compileOptions {
        sourceCompatibility = JavaVersion.<project JVM>
        targetCompatibility = JavaVersion.<project JVM>
    }

    kotlinOptions {
        jvmTarget = "<project JVM>"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Internal modules — mirror what sibling features pull in
    // implementation(project(":<core>"))
    // implementation(project(":<design-system>"))
    // implementation(project(":<domain>"))

    // Compose — match the project's Compose BOM / catalog version
    implementation(platform("androidx.compose:compose-bom:<version>"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Navigation
    implementation("androidx.navigation:navigation-compose:<version>")

    // DI — use whatever the project uses (Hilt, Koin, Dagger, etc.):
    // implementation("io.insert-koin:koin-androidx-compose:<version>")
    // implementation("com.google.dagger:hilt-android:<version>")
    // ksp("com.google.dagger:hilt-compiler:<version>")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    // Add test stack mirroring siblings (mockk / turbine / coroutines-test)
}
```

**Application reverse-dep**: do **not** add `implementation(project(":app"))` (or whatever the application module is). Plain library modules cannot reverse-depend on the application module — that's exclusive to dynamic features.

**Convention plugins**: if the project ships an internal `acme.android.feature` (or similar) convention plugin, apply that one plugin and drop the manual `compileSdk`/`minSdk`/`compileOptions`/`kotlinOptions`/`buildFeatures` blocks the convention already provides.

## 2. `AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest />
```

No `<dist:module>` — that's DFM-only.

## 3. Package contents

Generate stubs that match the project's existing patterns. Concrete file names, base classes, and API shapes vary by project — sample one existing feature module before authoring new ones.

Common patterns to look for:
- ViewModel base class (plain `ViewModel`, `AndroidViewModel`, project-specific base, MVI/store from a library)
- Screen composable conventions (single Route + Screen pair, NavGraph extension functions, etc.)
- DI registration (Hilt: `@HiltViewModel`; Koin: `viewModel { ... }` in a module; Dagger: subcomponent factories)
- Route / nav-args representation (sealed class, `@Serializable` data class, plain string constants)

Mirror what's there. If the codebase has zero precedent (greenfield), use the simplest viable: a plain `ViewModel` + a `@Composable Route(...)` + a `@Composable Screen(...)` + a route registration extension on `NavGraphBuilder`.

## 4. Wiring

- `settings.gradle.kts` → `include(":{name}")` (mirror project's path style — flat or grouped).
- Application module's `build.gradle.kts` → add `implementation(project(":{name}"))`.
- **Do NOT** add to `dynamicFeatures`.
- Register the feature's nav graph / DI module wherever the project does that — this is project-specific and out of scope for this skill.

## 5. Verify

```bash
./gradlew :{name}:compileDebugKotlin
```

## Last Sanity Check

If any of these are true, abort and switch to Dynamic Feature:
- The user mentioned on-demand, conditional, instant, premium-only, or "we might defer install"
- The feature ships > 5 MB of assets (models, video, audio)
- The codebase already uses dynamic features for every feature — diverging here breaks consistency without justification
