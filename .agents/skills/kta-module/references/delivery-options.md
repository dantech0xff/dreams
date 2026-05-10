# Dynamic Feature Delivery Options

## Install-time Delivery (Default)

Module installed when user downloads app from Play Store.

```xml
<dist:delivery>
    <dist:install-time />
</dist:delivery>
```

Use for: Core features always needed.

## On-demand Delivery

Module downloaded when requested by code.

```xml
<dist:delivery>
    <dist:on-demand />
</dist:delivery>
```

Request in code:
```kotlin
val splitInstallManager = SplitInstallManagerFactory.create(context)
val request = SplitInstallRequest.newBuilder()
    .addModule("feature_name")
    .build()
splitInstallManager.startInstall(request)
```

Use for: Large features, premium content.

## Conditional Delivery

Module installed based on device conditions.

```xml
<dist:delivery>
    <dist:install-time>
        <dist:conditions>
            <dist:device-feature dist:name="android.hardware.camera" />
            <dist:min-sdk dist:value="24" />
        </dist:conditions>
    </dist:install-time>
</dist:delivery>
```

Conditions: `device-feature`, `min-sdk`, `user-countries`.

## Fusing

Include module in pre-Lollipop APKs (multi-APK).

```xml
<dist:fusing dist:include="true" />
```

Always set `true` for backwards compatibility.

## Registration Checklist (Gradle wiring)

1. Application module `build.gradle.kts`: add the feature path to `dynamicFeatures` (`+= setOf(...)` or `= setOf(...)`, match existing style).
2. `settings.gradle.kts`: `include(":<path-to-feature>")` — mirror the project's path style (flat or grouped).

Any project-specific registrations (DI graph, navigation provider, route registry) are out of scope for this skill — discover and report them based on the patterns observed in sibling DFMs.
