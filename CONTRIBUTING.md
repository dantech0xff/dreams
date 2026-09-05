# Contributing to Dreams

Thanks for helping make Dreams a better place to learn AGSL. The repo is an Android app (Kotlin, Jetpack Compose) whose content is 56 runnable shader lessons, plus the Node tooling that turns those lessons into the README catalog, the thumbnails and the web gallery. This guide is about adding to it without breaking the pieces that hold it together.

- [What we are looking for](#what-we-are-looking-for)
- [Before you start](#before-you-start)
- [Development setup](#development-setup)
- [Build, test and lint](#build-test-and-lint)
- [Adding or changing a lesson](#adding-or-changing-a-lesson)
- [Regenerating the catalog, and what CI checks](#regenerating-the-catalog-and-what-ci-checks)
- [Commit messages](#commit-messages)
- [Pull requests](#pull-requests)
- [Code of conduct](#code-of-conduct)
- [License](#license)

## What we are looking for

| Contribution | Notes |
|---|---|
| **New lessons** | The most valuable thing you can add. A lesson teaches one idea, fits in one screen of AGSL and exposes one to three controls worth dragging. See [Adding or changing a lesson](#adding-or-changing-a-lesson). |
| **Fixes to shader math** | Aspect-ratio bugs, banding, off-by-one tiling, precision differences between GPUs. Include before/after screenshots from a device. |
| **Better explanations** | The `conceptIntro`, `learningNotes` and `screenRecordingHint` strings inside each lesson `object`, and the Markdown under [`docs/`](docs/). Plain language beats jargon. |
| **Tooling** | The scripts in [`tools/shader-catalog/`](tools/shader-catalog/) (Kotlin lesson extractor, AGSL to GLSL transpiler, headless renderer, gallery builder) and the workflows in [`.github/workflows/`](.github/workflows/). |
| **Bug reports** | Rendering differences between GPUs, crashes, wrong control ranges. Use the [bug report form](.github/ISSUE_TEMPLATE/bug_report.yml). |
| **Translations of lesson notes** | Not wired up yet: lesson text lives as Kotlin string literals inside each lesson `object`, not in Android string resources. Open an issue to discuss the approach before translating anything. |

## Before you start

- Search existing issues and pull requests.
- For a new lesson, open a [lesson proposal](.github/ISSUE_TEMPLATE/lesson_proposal.yml) first. It takes two minutes and avoids two people building the same Voronoi variant. Small fixes can go straight to a pull request.
- Read [`docs/code-standards.md`](docs/code-standards.md) (Kotlin conventions and architecture rules) and skim [`docs/system-architecture.md`](docs/system-architecture.md) so you know where things live.

## Development setup

| Requirement | Why |
|---|---|
| **Android Studio**, the latest stable release that supports **AGP 9.1.1** (check the [AGP and Android Studio compatibility table](https://developer.android.com/build/releases/gradle-plugin#android_gradle_plugin_and_android_studio_compatibility)) | The project pins AGP 9.1.1, Kotlin 2.2.10 and the 2026.02.01 Compose BOM in [`gradle/libs.versions.toml`](gradle/libs.versions.toml). |
| **JDK 17** | What CI uses. The app itself compiles to Java 11 bytecode (`compileOptions` in [`app/build.gradle.kts`](app/build.gradle.kts)), so any JDK 17 or newer can run Gradle. |
| **Gradle**: nothing to install | The wrapper pins Gradle 9.3.1 ([`gradle/wrapper/gradle-wrapper.properties`](gradle/wrapper/gradle-wrapper.properties)). Always use `./gradlew`. |
| **A device or emulator on Android 13 (API 33) or newer, with GPU acceleration** | `RuntimeShader` (AGSL) exists only from API 33, hence `minSdk = 33`. For an emulator pick a system image with hardware graphics; software rendering is slow and hides real-GPU precision differences. |
| **Node.js 20+** and Playwright Chromium (optional) | Only for the catalog tooling in `tools/shader-catalog` (`"engines": { "node": ">=20" }`). CI runs Node 22. |

```bash
git clone https://github.com/dantech0xff/dreams.git
cd dreams
./gradlew :app:assembleDebug          # first run downloads the Gradle distribution and dependencies
./gradlew :app:installDebug           # install on the connected device or emulator

# Catalog tooling (optional)
cd tools/shader-catalog
npm install
npx playwright install chromium       # once
```

## Build, test and lint

| Command | What it does |
|---|---|
| `./gradlew test` | JVM unit tests, no device needed. Includes [`LessonRegistryTest`](app/src/test/java/com/dantech/dreams/data/lesson/LessonRegistryTest.kt), which pins the per-category lesson counts, the Basics and Patterns ordering, and that every control targets a declared uniform. |
| `./gradlew :app:assembleDebug` | Builds `app/build/outputs/apk/debug/app-debug.apk`. |
| `./gradlew :app:installDebug` | Installs the debug build on the connected device. |
| `./gradlew :app:lint` | Android Lint, as bundled with AGP. |
| `cd tools/shader-catalog && npm run check` | Transpiles every lesson to GLSL ES 3.00 and compiles it in headless Chromium. Catches most syntax errors without a phone. |

There is **no ktlint, detekt or spotless** in this project. [`gradle.properties`](gradle.properties) sets `kotlin.code.style=official`; beyond that, follow [`docs/code-standards.md`](docs/code-standards.md) and match the surrounding code.

Two things the JVM tests cannot do:

- **Compile AGSL.** `RuntimeShader` is an Android framework class, so `./gradlew test` never compiles a shader. Debug builds do it for you at startup: [`DreamsApp`](app/src/main/java/com/dantech/dreams/DreamsApp.kt) calls `LessonRepository.validate()`, which compiles every lesson (including its `extraAgslSources`) with `RuntimeShader` and logs failures under the `LessonRepo` and `LessonRegistry` tags. After launching a debug build, run `adb logcat -s LessonRepo LessonRegistry`.
- **See the picture.** Colour management, `half` precision and driver quirks only show up on a real GPU. Run a new or changed lesson on a device before opening a pull request.

## Adding or changing a lesson

The full walkthrough is [`docs/adding-a-lesson.md`](docs/adding-a-lesson.md). The short version:

**1. Create the lesson object** in the category package under [`app/src/main/java/com/dantech/dreams/data/lesson/source/`](app/src/main/java/com/dantech/dreams/data/lesson/source/). Ids are `<prefix>-<NN>-<slug>`, numbered in display order within the category.

| Category | Package | Id prefix | Lessons today |
|---|---|---|---|
| Basics | `basics` | `basics-` | 6 |
| Patterns | `patterns` | `patterns-` | 10 |
| Color | `colorlab` | `color-` | 4 |
| SDF | `sdf` | `sdf-` | 6 |
| Noise | `noise` | `noise-` | 6 |
| Motion | `motion` | `motion-` | 4 |
| Fractals | `fractals` | `fractals-` | 4 |
| Lighting | `lighting` | `lighting-` | 4 |
| Interactive | `interactive` | `interactive-` | 4 |
| Post-FX | `posteffect` | `postfx-` | 6 |
| Showcase | `showcase` | `showcase-` | 2 |

A lesson is a Kotlin `object` with a public `id`, a `private val SOURCE` raw string and an `init` block that registers a `LessonModel`. This is [`AnimatedColor.kt`](app/src/main/java/com/dantech/dreams/data/lesson/source/basics/AnimatedColor.kt), lightly trimmed:

```kotlin
object AnimatedColor {
    val id = "basics-02-animated-color"

    private val SOURCE = """
        uniform float time;
        uniform float speed;

        half4 main(float2 fragCoord) {
            float t = 0.5 + 0.5 * sin(time * speed);
            half3 a = half3(0.10, 0.40, 0.90);
            half3 b = half3(0.95, 0.35, 0.20);
            return half4(mix(a, b, t), 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id,
                title = "Animated Color",
                category = LessonCategory.BASICS,
                complexity = 1,                        // 1..5, shown as bolts in the app
                conceptIntro = "...",                  // one or two sentences
                learningNotes = persistentListOf("...", "...", "..."),
                agslSource = SOURCE,
                controls = persistentListOf(
                    LessonControl.FloatRange("Speed", "speed", 0.1f, 4f, 1f),
                ),
            )
        )
    }
}
```

**2. Declare exactly the uniforms you use.** The runtime regex-scans `agslSource` ([`ShaderUniformBindings.kt`](app/src/main/java/com/dantech/dreams/ui/feature/common/ShaderUniformBindings.kt), [`AgslCanvas.kt`](app/src/main/java/com/dantech/dreams/ui/feature/common/AgslCanvas.kt)) and only writes uniforms that are declared, because writing an undeclared one throws under CheckJNI. A control whose uniform is missing is silently ignored at runtime, and `LessonRegistryTest` and the catalog extractor both fail on it.

| Declaration in AGSL | What the runtime writes | Notes |
|---|---|---|
| `uniform float2 resolution;` | Preview size in pixels | `fragCoord / resolution` gives 0..1 UV. |
| `uniform float time;` | Seconds, updated every frame | Leave it out for static lessons. |
| `uniform float2 touchPos;` **and** `uniform float touchTime;` | The last tap in 0..1 UV, and the value of `time` when it happened | Both must be declared or taps are not wired. Before any tap they are `(-1, -1)` and `-1`; branch on `touchPos.x < 0.0` the way the Interactive lessons do. |
| `uniform float <name>;` | The `LessonControl.FloatRange` whose `uniformName` is `<name>` | A slider with `min`, `max` and `default`. |
| `layout(color) uniform half4 <name>;` | The `LessonControl.ColorPicker` whose `uniformName` is `<name>` | A colour swatch. |
| `uniform shader content;` | The Compose subtree the effect is applied to | Post-FX only. Set `renderMode = LessonRenderMode.RENDER_EFFECT` and `postEffectContent = { SampleContent() }`, then read pixels with `content.eval(coord)`. See [`PostFxLessons.kt`](app/src/main/java/com/dantech/dreams/data/lesson/source/posteffect/PostFxLessons.kt). |

**3. Register it in the category's Bootstrap**, for example [`BasicsBootstrap.kt`](app/src/main/java/com/dantech/dreams/data/lesson/source/basics/BasicsBootstrap.kt). `touch()` references every lesson's `id` so the `init` block runs, and the order of those lines is the display order. [`LessonRegistry.bootstrap()`](app/src/main/java/com/dantech/dreams/data/lesson/LessonRegistry.kt) calls every Bootstrap, and `LessonRegistry.register()` rejects duplicate ids. The catalog extractor fails if a lesson object is never referenced by a Bootstrap.

**4. Update the tests** in [`LessonRegistryTest.kt`](app/src/test/java/com/dantech/dreams/data/lesson/LessonRegistryTest.kt): bump the category count in `each category has expected lesson count`, and if the lesson is in Basics or Patterns, add its id to the ordering list and give it exactly three `learningNotes`.

**5. Regenerate the catalog** (next section) and run the lesson on a device.

For a completely different render path (`LessonRenderMode.CUSTOM` with a `customPreview`), look at the two Showcase lessons; they own their own shader, clock and gestures.

## Regenerating the catalog, and what CI checks

[`docs/catalog/lessons.json`](docs/catalog/lessons.json) is generated from the Kotlin sources and is the single source of truth for the README lesson table, the thumbnails in [`docs/gallery/`](docs/gallery/) and the web gallery [`docs/index.html`](docs/index.html). Do not edit those by hand.

```bash
cd tools/shader-catalog
npm run all        # extract, thumbnails, README table, docs/index.html
```

Or step by step:

| Command | Output |
|---|---|
| `node tools/shader-catalog/extract-lessons.mjs` (`npm run extract`) | `docs/catalog/lessons.json` |
| `node tools/shader-catalog/render-thumbnails.mjs` (`npm run thumbs`) | `docs/gallery/<lesson-id>.png` and the `poster-hero.png` contact sheet. `--only <id>` renders one lesson, `--check` only compiles. |
| `node tools/shader-catalog/update-readme.mjs` (`npm run readme`) | The lesson table between the `<!-- catalog:start -->` and `<!-- catalog:end -->` markers in `README.md` |
| `node tools/shader-catalog/build-site.mjs` (`npm run site`) | `docs/index.html` |

If the default thumbnail frame (t = 2.5 s, default control values, a simulated tap for touch lessons) does not show your lesson well, add an override for its id in [`tools/shader-catalog/thumb-states.mjs`](tools/shader-catalog/thumb-states.mjs).

**CI** ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)) runs on every pull request and on pushes to `master`:

1. `./gradlew test` and `./gradlew :app:assembleDebug` on JDK 17.
2. `npm run catalog` (extract, README table, site), then **fails if `docs/catalog/lessons.json`, `docs/index.html` or `README.md` differ from what you committed**.
3. `npm run check`: every lesson must compile as GLSL ES 3.00 in headless Chromium.

CI does not re-render PNGs, so the thumbnail for a new or visually changed lesson is your job. Commit it together with the code.

## Commit messages

Conventional Commits, as in the existing history:

```
feat(lessons): expand patterns category
feat(theme): add shader lab light and dark themes
fix(nav): keep bottom bar visible on lesson detail
refactor(nav): split nav into outer push stack + inner tab shell
perf(compose): mark VMs and prefs stable for skippable recomposition
style: refine workbench theme chrome
docs: replace broken <video> tag with inline demo gif
chore: ignore .claude/settings.local.json
```

Format: `type(scope): imperative, lowercase summary`. Types in use: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `ci`, `chore`; `test` is fine for test-only changes. The scope is optional and names the area (`lessons`, `showcase`, `theme`, `nav`, `ui`, `compose`, `tooling`, ...). Put the reasoning in the body when the title is not enough.

## Pull requests

- Branch from `master`, one topic per pull request. A new lesson with its test, catalog and thumbnail changes is one topic; a refactor of the preview runtime is another.
- Fill in the [pull request template](.github/pull_request_template.md). For anything visual, attach a device screenshot or a short recording; reviewers cannot run every GPU.
- Keep the generated files in sync (see above) so CI stays green.
- Expect review comments about clarity as much as correctness. The lesson text is the product.

Before you open it:

- [ ] `./gradlew test` passes
- [ ] `./gradlew :app:assembleDebug` builds
- [ ] The change was run on an API 33+ device or a GPU-accelerated emulator
- [ ] New or changed lesson: Bootstrap updated; `LessonRegistryTest` counts (and ordering plus three notes for Basics and Patterns) updated
- [ ] New or changed lesson: `npm run all` executed and `docs/catalog/lessons.json`, `docs/index.html`, `README.md` and `docs/gallery/<id>.png` committed
- [ ] Commit messages follow the format above
- [ ] No unrelated reformatting

## Code of conduct

Be kind, assume good intent, and keep feedback about the work rather than the person. This project follows the [Contributor Covenant v2.1](https://www.contributor-covenant.org/version/2/1/code_of_conduct/); if something goes wrong, contact the maintainer through the repository.

## License

Dreams is released under the [MIT License](LICENSE). By contributing you agree that your contributions are licensed under MIT as well. Only port shader code whose license allows that (Shadertoy's default licence, CC BY-NC-SA 3.0, does **not**), and when you adapt a published technique, credit the author in a comment inside `SOURCE` or in `conceptIntro`.
