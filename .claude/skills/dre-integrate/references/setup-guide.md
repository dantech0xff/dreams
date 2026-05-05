# Setup Guide

## Workflow

1. **Detect project type** — Read `build.gradle.kts` to determine Android app vs pure Kotlin
2. **Check existing dependency** — Grep for `dre-core` or `dre-android` in build files
3. **Add dependency** — Insert into correct `build.gradle.kts`
4. **Verify** — Run `./gradlew dependencies` to confirm resolution
5. **Suggest next step** — Offer to scaffold first feature

## Step 1: Detect Project Type

Read root `build.gradle.kts` and app module's `build.gradle.kts`:

- Has `com.android.application` plugin → **Android app** → use `dre-android`
- Has `org.jetbrains.kotlin.jvm` only → **Pure Kotlin** → use `dre-core`
- Has `com.android.library` → **Android library** → use `dre-android`

## Step 2: Check Existing

Search for existing dre-kt dependency:
```
Grep pattern: "dre-core|dre-android|dantech0xff"
```

If found → inform user already integrated, offer `/dre-integrate feature` instead.

## Step 3: Add Dependency

### For Android projects

Add to app module's `build.gradle.kts` dependencies block:

```kotlin
implementation("io.github.dantech0xff:dre-android:0.1.0")
```

### For pure Kotlin projects

```kotlin
implementation("io.github.dantech0xff:dre-core:0.1.0")
```

### Verify `mavenCentral()` in repositories

Check `settings.gradle.kts` or root `build.gradle.kts` has:
```kotlin
repositories {
    mavenCentral()
}
```

## Step 4: Verify

Run:
```bash
./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep dre
```

Expected output:
```
+--- io.github.dantech0xff:dre-android:0.1.0
|    \--- io.github.dantech0xff:dre-core:0.1.0
```

## Step 5: Suggest Next Steps

Ask user via `AskUserQuestion`:
- "Scaffold first feature?" → run `/dre-integrate feature <name>`
- "Done for now" → exit

## Recommended Project Structure

Suggest organizing DRE features by feature folder:

```
app/src/main/kotlin/com/example/app/
├── feature/
│   ├── login/
│   │   ├── LoginContract.kt      # State, Action, Effect, AsyncOp
│   │   ├── LoginReducer.kt       # Pure reducer
│   │   ├── LoginViewModel.kt     # DreStoreViewModel subclass
│   │   └── LoginScreen.kt        # Compose UI
│   ├── home/
│   │   ├── HomeContract.kt
│   │   ├── HomeReducer.kt
│   │   ├── HomeViewModel.kt
│   │   └── HomeScreen.kt
│   └── settings/
│       ├── SettingsContract.kt
│       ├── SettingsReducer.kt
│       ├── SettingsViewModel.kt   # DreStoreViewModel
│       └── SettingsScreen.kt
```
