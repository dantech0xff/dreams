# Library Module Structure

Plain Android library — for reusable utilities, data, domain, design tokens, shared UI primitives. Always ships in the base APK. Cannot own a feature nav route in a dynamic-feature delivery graph.

> The templates below are **vanilla AGP defaults**. Before writing files, adapt to the project's actual conventions discovered in Step 0 of SKILL.md (convention plugins, version-catalog aliases, namespace pattern, sibling deps, JVM/SDK levels).

## File Layout

```
{module-name}/
├── build.gradle.kts
├── consumer-rules.pro          # empty unless you publish ProGuard hints
└── src/main/
    ├── AndroidManifest.xml     # minimal — namespace only via build.gradle
    └── kotlin/<package-path>/
        └── (your packages)
```

No `<dist:module>`, no application reverse-dep.

## 1. `build.gradle.kts` (vanilla template)

```kotlin
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    // Add only if module ships Compose UI:
    // id("org.jetbrains.kotlin.plugin.compose")
    // Add only if module uses kotlinx.serialization:
    // id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "<your.package.namespace>"
    compileSdk = <project compileSdk>

    defaultConfig {
        minSdk = <project minSdk>
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.<project JVM>
        targetCompatibility = JavaVersion.<project JVM>
    }

    kotlinOptions {
        jvmTarget = "<project JVM>"
    }

    // Only if shipping Compose UI:
    // buildFeatures { compose = true }
}

dependencies {
    // Mirror sibling library modules — pull only what's needed
    // implementation(project(":other-internal-module"))

    // Project's preferred logger / DI / etc:
    // implementation(libs.timber)
    // implementation(libs.koin.core)   // or Hilt: implementation(libs.hilt.android)

    testImplementation("junit:junit:4.13.2")
    // Project's preferred test stack — mirror siblings
}
```

**If the project uses a convention plugin** (e.g. `acme.android.library` published from `build-logic/`), apply that single plugin and skip the manual `compileSdk` / `minSdk` / `compileOptions` / `kotlinOptions` blocks — the convention provides them. Detect this in Step 0 by reading sibling library `build.gradle.kts` files.

**If the project uses a version catalog** (`gradle/libs.versions.toml`), use `alias(libs.plugins.xxx)` and `libs.xxx` references instead of raw plugin IDs and dep coordinates. Match catalog naming.

## 2. `AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest />
```

Namespace is provided by `android { namespace = ... }` in `build.gradle.kts`.

## 3. `consumer-rules.pro`

Empty file. Add ProGuard rules only if this library ships APIs that consumers must keep.

## 4. Wiring

- `settings.gradle.kts` → `include(":{module}")` (top-level) or `include(":{group}:{module}")` if the project groups modules. Mirror existing path style.
- **No** application module change — application module already pulls libraries via `implementation(project(":{module}"))` as needed.
- Add `implementation(project(":{module}"))` in every consumer module.

## 5. Verify

```bash
./gradlew :{module}:compileDebugKotlin
./gradlew :{module}:testDebugUnitTest
```

## When NOT to Pick Library

- You need a navigation route registered with the app's nav graph → use Feature Module or Dynamic Feature.
- You need on-demand / conditional / instant delivery → use Dynamic Feature.
- The code has zero consumers besides one feature → just inline; don't make a module.
