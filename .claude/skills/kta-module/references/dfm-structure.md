# Dynamic Feature Module Structure

For features that may be deferred (on-demand / conditional / instant) and that own a nav route. Reverse-depends on the application module — cannot be a dependency of any other module.

> The templates below are **vanilla AGP defaults**. Adapt to the project's actual conventions discovered in Step 0 of SKILL.md (convention plugins, version-catalog aliases, namespace pattern, sibling deps, JVM/SDK levels, DI/nav stack).

## File Layout (illustrative)

```
{name}/
├── build.gradle.kts
└── src/main/
    ├── AndroidManifest.xml
    ├── kotlin/<package-path>/
    │   ├── di/
    │   ├── navigation/
    │   ├── ui/
    │   └── viewmodel/
    └── res/values/strings.xml
```

If the project groups dynamic features under a folder (e.g. `feature/<name>`), mirror that path. If it places them flat at the root, mirror that.

## 1. `build.gradle.kts` (vanilla template)

```kotlin
plugins {
    id("com.android.dynamic-feature")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")  // if Compose UI
    // Add only as the project requires:
    // id("com.google.devtools.ksp")
    // id("com.google.dagger.hilt.android")
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
    // REQUIRED for dynamic features: reverse-depend on the application module
    implementation(project(":<application-module-name>"))  // typically ":app"

    // Internal libraries — mirror sibling DFMs
    // implementation(project(":<core>"))
    // implementation(project(":<design-system>"))
    // implementation(project(":<domain>"))

    // Compose — match project's BOM / catalog
    implementation(platform("androidx.compose:compose-bom:<version>"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Navigation
    implementation("androidx.navigation:navigation-compose:<version>")

    // SplitInstall (only if module triggers on-demand installs from code)
    // implementation("com.google.android.play:feature-delivery-ktx:<version>")

    // DI — match project's choice
    // implementation("io.insert-koin:koin-androidx-compose:<version>")
    // implementation("com.google.dagger:hilt-android:<version>")
    // ksp("com.google.dagger:hilt-compiler:<version>")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
```

**Convention plugins**: if the project ships `acme.android.feature` (or similar dynamic-feature convention), apply that one plugin and drop the manual blocks it covers.

**Application module name**: discover the actual application module path from `settings.gradle.kts` and `dynamicFeatures = setOf(...)` in the application's `build.gradle.kts`. Common values are `":app"`, `":application"`, `":main"`. Don't hardcode `:app`.

## 2. `AndroidManifest.xml` (install-time default)

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:dist="http://schemas.android.com/apk/distribution">

    <dist:module
        dist:instant="false"
        dist:title="@string/title_{name}">
        <dist:delivery>
            <dist:install-time />
        </dist:delivery>
        <dist:fusing dist:include="true" />
    </dist:module>
</manifest>
```

For other delivery modes see `delivery-options.md`.

## 3. `res/values/strings.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="title_{name}">{Display Name}</string>
</resources>
```

The `dist:title` in the manifest references this string — required by the manifest validator.

## 4. Package contents

Generate stubs that match the project's existing DFM patterns. Sample one existing dynamic feature before authoring. Look for:
- ViewModel base class
- Route / Screen composable convention
- DI registration (Hilt automatically wires DFM components in many setups; Koin needs explicit module registration; Dagger uses subcomponents)
- Nav graph extension functions vs route registries
- Route data type (sealed class, `@Serializable`, string constants)

Mirror what's there. If greenfield (no DFMs yet but user picked DFM), use the simplest viable: plain `ViewModel`, `@Composable Route(...)` + `@Composable Screen(...)`, `NavGraphBuilder.<feature>NavGraph()` extension.

**Note**: registering the feature's DI module / nav graph in the application module is **project-specific** and out of scope for this skill. Report what the user must do next based on the patterns observed.

## 5. Wiring

- `settings.gradle.kts` → `include(":<path-to-feature>")` matching the project's path style.
- Application module's `build.gradle.kts` → add the path to `dynamicFeatures += setOf(...)` (or `dynamicFeatures = setOf(...)` depending on existing style).
- **Do NOT** add `implementation(project(":<feature>"))` to the application module — DFMs are wired via `dynamicFeatures`, not regular `implementation`.

## 6. Verify

```bash
./gradlew :<path-to-feature>:compileDebugKotlin
```

If full `assembleDebug` requires environment-specific files (e.g. `google-services.json`, signing keys), prefer `compileDebugKotlin` for sanity-checking the skeleton.
