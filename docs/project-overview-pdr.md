# Dreams: AGSL Engineer Playground — Project Overview & PDR

## Project Vision

Dreams is a curated, swipeable Jetpack Compose gallery for learning **Android Graphics Shading Language (AGSL)** through 23 bite-sized lessons organized by topic, plus three full-screen "wow" showcase demos engineered for screen-recording.

**Target Audience:** Android engineers already writing Compose who want a runnable, interactive intro to runtime shaders on real devices.

---

## Core Product Metrics

| Metric | Target |
|--------|--------|
| **Min SDK** | 33 (AGSL requirement) |
| **Target SDK** | 36 |
| **Lessons** | 23 (6 Basics, 6 SDF, 6 Noise, 5 Post-FX) |
| **Showcases** | 3 (Liquid Glass, Aurora, Raymarched Sphere) |
| **Build Time** | <90s (single module, no multi-module overhead) |
| **Test Coverage** | ≥80% (data layer, VM logic, DI) |

---

## Technology Stack

- **Kotlin** — JVM 11+, Coroutines (viewModelScope, Flow)
- **Jetpack Compose** — Material3 (BOM 2026.02.01), Modifier chains, State management
- **Navigation** — Navigation3 1.1.1 (`@Serializable` routes, type-safe navigation)
- **DI** — Koin 4.2.0 (BOM), viewModel scoping via `rememberViewModelStoreNavEntryDecorator`
- **Persistence** — DataStore Preferences (`dreams_prefs`), kotlinx.serialization
- **Testing** — JUnit, Turbine, Koin.verify(), UnconfinedTestDispatcher
- **AGSL** — `RuntimeShader`, `ShaderBrush`, fragment shaders 460

---

## Product Development Requirements (PDR)

### Functional Requirements

#### F1: Lesson Gallery
- Display lessons in swipeable tabs by category (Basics, SDF, Noise, Post-FX)
- Each lesson card shows title, shader preview, and favorite toggle
- Favorites persist across app restarts via DataStore
- Last viewed lesson remembered; gallery auto-scrolls to it on cold start

#### F2: Lesson Detail Screen
- Full-screen AGSL shader with interactive controls (sliders for parameters)
- Slider value changes persist with 200ms debounce
- Smooth shared-element transition from gallery card to detail screen
- Back navigation returns to gallery at last-viewed position

#### F3: Showcase Screens
- Three full-screen, tap-responsive shader demos (liquid glass, aurora, raymarched sphere)
- Reduced-motion users see instant swaps instead of animated transitions
- Touchable regions trigger visual feedback (ripples, parameter shifts)

#### F4: Settings & Preferences
- Modal bottom sheet with reduced-motion toggle
- Toggle persists via DataStore
- Reduced-motion overrides system `ANIMATOR_DURATION_SCALE`

### Non-Functional Requirements

#### NF1: Architectural Clarity
- Single Gradle module, layered packages (`core`, `data`, `domain`, `ui`)
- Clear separation: DI, repository, ViewModel, UI (Composables)
- No global mutable singletons in app code (LessonRegistry hidden behind interface)

#### NF2: Testability
- DI modules verifiable via Koin.verify() test
- Repositories fakeable (e.g., minimal AGSL stubs for instrumented tests)
- VMs tested with Turbine + UnconfinedTestDispatcher

#### NF3: Accessibility
- Reduced-motion compliance (snap vs. tween based on system + user prefs)
- Semantic navigation (back-stack survives config change + process death)
- Settings UI keyboard-accessible

#### NF4: Performance
- AGSL uniforms updated on Compose thread (SnapshotStateMap) — no async shader recompilation
- Lesson bootstrap (23 shaders + metadata) completes in <500ms
- DataStore reads cached in-memory (Flow-based), writes debounced

---

## Architecture Overview

### Package Layering

```
com.dantech.dreams/
├── core/
│   ├── agsl/           # RuntimeShader utilities, AGSL code as assets
│   ├── di/             # Koin modules (AppModule, DataModule, FeatureModule)
│   └── motion/         # Motion utilities (reduced-motion logic)
├── data/
│   ├── lesson/         # LessonRepositoryImpl, Lesson entity, lesson sources
│   └── prefs/          # UserPrefsRepositoryImpl, Prefs data class
├── domain/
│   └── lesson/         # LessonRepository interface (repo contract)
└── ui/
    ├── feature/
    │   ├── landing/    # Landing (onboarding) screen + VM
    │   ├── gallery/    # Gallery + VM
    │   ├── lesson/     # LessonDetail + VM (with slider persistence)
    │   ├── showcase/   # Showcase + VM
    │   ├── settings/   # Settings bottom sheet (reduced-motion toggle)
    │   ├── common/     # Shared Composables (LessonCard, shared-element logic)
    │   └── nav/        # Route definitions, NavDisplay, nav logic
    └── theme/          # Tokens, colors, typography, spacing
```

### Dependency Graph

```
ui/feature/{landing,gallery,lesson,showcase,settings}
    ↓
domain/lesson (LessonRepository interface)
    ↓
data/lesson + data/prefs
    ↓
core/di (Koin modules) + core/agsl + core/motion
```

---

## Key Decisions

### 1. Koin for DI
- **Why:** Lightweight, Kotlin-first, minimal boilerplate, native viewModel scoping
- **Trade-off:** Single module means no incremental compilation benefit; chosen for clarity over scale

### 2. Navigation3 with `@Serializable` Routes
- **Why:** Type-safe routes, no string keys, seamless route parsing, back-stack safe
- **Trade-off:** Requires kotlinx.serialization; lesson IDs must be Serializable (String)

### 3. ViewModel per Feature
- **Why:** State ownership, SavedStateHandle for tab position recovery, natural screen boundaries
- **Trade-off:** 4 ViewModels + 4 UiState data classes (boilerplate); justified by testability

### 4. Shared Elements with Reduced-Motion Fallback
- **Why:** Gallery→Detail transition polished when animations enabled; instant swap when disabled
- **Trade-off:** Requires motion-aware composables; fallback ensures all users see transitions

### 5. DataStore (not SharedPreferences)
- **Why:** Type-safe, proto-backed by default (could evolve), async-first Flow API, coroutine-friendly
- **Trade-off:** Slightly heavier than SharedPreferences; justified by consistency with modern Compose patterns

### 6. Lesson Bootstrap with Idempotency Guard
- **Why:** LessonRegistry.bootstrap() checks `if (all.isEmpty())` before loading 23 lessons; safe for test restarts
- **Trade-off:** No explicit reset method; tests rely on isolation (each test gets fresh app state via DreamsTestRunner)

---

## Testing Strategy

### Unit Tests (JVM)
- **DI verification:** `KoinModulesCheckTest.dataModule.verify()`
- **ViewModel logic:** Turbine + UnconfinedTestDispatcher (no real time passage)
- **Repository:** Fake LessonRepository + FakeDataStore for prefs

### Instrumented Tests (androidTest)
- **Navigation flow:** DreamsTestRunner + TestDreamsApp override Koin with minimal AGSL stubs
- **UI interactions:** Composable lambda testing (future: androidx.compose.ui.test)

### Manual Tests
- Cold start → Gallery remembers last lesson
- Toggle favorite → DataStore persists
- Reduced-motion toggle → transitions snap vs. animate
- All 23 lessons load and render without crashes

---

## Known Limitations & Future Work

### Current Limitations
1. **Instrumented tests incomplete:** UI test framework scaffolded but not fully implemented (deferred per plan)
2. **No multi-module:** Single app module; if codebase grows, future refactor to feature modules recommended
3. **AGSL stubs on CI emulator:** Real shader execution only on physical device or real emulator (no CPU-side rendering)

### Future Enhancements
1. **Lesson sharing:** Export shader code as gist / GitHub
2. **Custom shader editor:** Write and test AGSL inline
3. **Performance profiling:** Frame time vs. uniform update cost
4. **Multi-language lesson content**

---

## Success Criteria (Shipped State)

- [x] App builds clean, no warnings
- [x] All 23 lessons load and render
- [x] Gallery + favorites persist across cold start
- [x] Back-stack survives config change + process death
- [x] Reduced-motion → instant swaps (not animated)
- [x] `./gradlew test` passes (DI verification, VM tests)
- [x] No global mutable singletons in app code
- [x] Settings sheet toggles reduced-motion preference

---

## Glossary

- **AGSL** — Android Graphics Shading Language (GLSL-like, runs on Skia/GPU)
- **Koin** — Lightweight service locator / DI framework for Kotlin
- **Navigation3** — Type-safe navigation library with `@Serializable` route support
- **DataStore** — Async-first preference storage, replaces SharedPreferences
- **Reduced-motion** — System + app setting to disable animations for accessibility
- **SavedStateHandle** — Bundle that survives process death and config changes
- **Turbine** — Testing library for Kotlin Flows; mock time + collect emissions
- **UiState** — Immutable data class representing UI rendering state (replaces MutableState)

---

## Links to Implementation

- **Refactor plan:** [Production-grade refactor (Koin + Nav3 + ViewModel)](../plans/260506-0052-production-refactor-koin-nav3/plan.md)
- **DI modules:** `app/src/main/java/com/dantech/dreams/core/di/`
- **Navigation routes:** `app/src/main/java/com/dantech/dreams/ui/feature/nav/Route.kt`
- **Test scaffold:** `app/src/test/java/com/dantech/dreams/core/di/KoinModulesCheckTest.kt`
- **Preferences:** `app/src/main/java/com/dantech/dreams/data/prefs/`
