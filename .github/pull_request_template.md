<!-- One topic per pull request. The full flow is in CONTRIBUTING.md. -->

## Summary

<!-- What changes and why. Link the issue if there is one: Closes #123 -->

## Type of change

- [ ] New lesson
- [ ] Change to an existing lesson (shader math, controls, text)
- [ ] App or runtime (Compose UI, navigation, preview pipeline, theme)
- [ ] Tooling (`tools/shader-catalog`, CI)
- [ ] Documentation only
- [ ] Bug fix

## Lesson checklist

<!-- Delete this section if no lesson was added or changed. -->

- [ ] The lesson `object` lives in the right category package and its id follows `<prefix>-<NN>-<slug>`
- [ ] `<Category>Bootstrap.touch()` references the object (its position is the display order)
- [ ] Every control targets a uniform declared in `SOURCE` (`uniform float x;` or `layout(color) uniform half4 x;`)
- [ ] `LessonRegistryTest` updated: category count, and for Basics/Patterns the ordering list plus exactly three `learningNotes`
- [ ] `cd tools/shader-catalog && npm run all` executed; `docs/catalog/lessons.json`, `docs/index.html`, `README.md` and `docs/gallery/<id>.png` committed
- [ ] The thumbnail reads well (or an override was added in `tools/shader-catalog/thumb-states.mjs`)
- [ ] Tested on an API 33+ device or a GPU-accelerated emulator; no `LessonRepo` compile errors in logcat

## General checklist

- [ ] `./gradlew test` passes
- [ ] `./gradlew :app:assembleDebug` builds
- [ ] Follows `docs/code-standards.md`; no unrelated reformatting
- [ ] Commit messages use `type(scope): summary` (for example `feat(lessons): add curl noise flow`)

## Screenshots or recording

<!-- Required for anything visual. Say which device and Android version. -->

| Before | After |
|---|---|
|  |  |

## How this was tested

<!-- Device(s), Android version(s), which sliders you dragged and where you tapped. -->
