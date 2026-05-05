# Phase 02 — Koin DI Bootstrap

## 1. Context Links
- Parent: [plan.md](plan.md)
- Depends on: [phase-01-deps-and-package-layering.md](phase-01-deps-and-package-layering.md)
- Inputs: `research/researcher-01-koin-vm-datastore.md` §1
- Docs: https://insert-koin.io/docs/reference/koin-android/start | https://insert-koin.io/docs/reference/koin-test/checkmodules

## 2. Overview
- **Date:** 2026-05-06
- **Description:** Introduce Koin 4 DI. Define `appModule`, `dataModule`, `featureModule`. Hide `LessonRegistry` behind `LessonRepository` interface. Wire `startKoin {}` in `DreamsApp.onCreate()`. Add `checkModules()` test gate.
- **Priority:** P1
- **Implementation status:** pending
- **Review status:** pending

## 3. Key Insights
- `object LessonRegistry` (data/lesson/LessonRegistry.kt:10) currently holds mutable global state. Repository interface lets us inject fakes in tests + isolate registration logic.
- Bootstrap pattern (`BasicsBootstrap.touch()` etc.) stays — it's idempotent metadata registration, not runtime work. Move call site from screen-bound singleton to repository init.
- `validateAll()` (LessonRegistry.kt:36) is debug-only RuntimeShader compile check; keep behind interface as `validate(): List<Failure>`.
- Koin 4 auto-detects context — no `KoinAndroidContext` needed.
- `checkModules()` runs at unit-test time → catches missing deps before app launch.

## 4. Requirements

### Functional
- `LessonRepository.all()`, `byCategory(c)`, `byId(id)`, `validate()` available via Koin.
- DI graph: Application → Koin → modules → repos. ViewModels inject repos via `viewModel { ... }` DSL (phase-03 wires VMs).
- App startup unchanged from user perspective.

### Non-Functional
- Zero global mutable singletons exposed to UI. `LessonRegistry` → `private` impl class behind `LessonRepository`.
- `checkModules()` test passes. No reflection-only deps.

## 5. Architecture

```
DreamsApp.onCreate()
  └── startKoin { androidContext(this@DreamsApp); modules(appModule, dataModule, featureModule) }

dataModule
  └── single<LessonRepository> { LessonRepositoryImpl(...).also { it.bootstrap() } }

featureModule (filled in phase-03)
  └── viewModel { GalleryViewModel(get()) }
  └── viewModel { LessonDetailViewModel(get(), get<SavedStateHandle>()) }
  └── ...

UI
  └── koinViewModel<X>() in @Composable
```

## 6. Related Code Files

### Modify
- `app/src/main/java/com/dantech/dreams/DreamsApp.kt` — call `startKoin {}` instead of `LessonRegistry.bootstrap()`
- `app/src/main/java/com/dantech/dreams/data/lesson/LessonRegistry.kt` — convert to `internal class LessonRegistryImpl` (still bootstrap-driven; called by `LessonRepositoryImpl`)
- `app/src/test/java/com/dantech/dreams/data/lesson/LessonRegistryTest.kt` — update to instantiate `LessonRepositoryImpl()` instead of using object

### Create
- `domain/lesson/LessonRepository.kt` — interface
- `data/lesson/LessonRepositoryImpl.kt` — wraps `LessonRegistryImpl`
- `core/di/AppModule.kt` — `val appModule = module { }` (placeholder for cross-cut deps)
- `core/di/DataModule.kt` — `val dataModule = module { single<LessonRepository> { LessonRepositoryImpl() } }`
- `core/di/FeatureModule.kt` — `val featureModule = module { /* phase-03 will fill */ }`
- `app/src/test/java/com/dantech/dreams/core/di/KoinModulesCheckTest.kt`

### Delete
- None this phase. (`LessonRegistry` object kept but downgraded to private impl.)

## 7. Implementation Steps

1. **Create `domain/lesson/LessonRepository.kt`:**
   ```kotlin
   package com.dantech.dreams.domain.lesson

   import com.dantech.dreams.data.lesson.LessonCategory
   import com.dantech.dreams.data.lesson.LessonModel
   import kotlinx.collections.immutable.ImmutableList

   interface LessonRepository {
       fun all(): ImmutableList<LessonModel>
       fun byCategory(category: LessonCategory): ImmutableList<LessonModel>
       fun byId(id: String): LessonModel?
       fun validate(): List<Pair<String, String>>  // (id, error message)
   }
   ```

2. **Refactor `data/lesson/LessonRegistry.kt`:** rename `object LessonRegistry` → `internal class LessonRegistryImpl`. Keep `register/all/byCategory/byId/bootstrap/validateAll` methods. Drop public visibility of `bootstrap()` — make it called once in `LessonRepositoryImpl` constructor or `init {}`.
   - Existing bootstrap callers (`BasicsBootstrap.touch()`, etc. — `data/lesson/source/*Bootstrap.kt` after phase-01 move) call `LessonRegistry.register(...)` from singletons. Either:
     - **Option A:** keep `object LessonRegistry` as private global (registration target), expose `LessonRepositoryImpl` as facade. Simpler; less invasive.
     - **Option B:** pass repo into bootstrap functions: `BasicsBootstrap.register(repo)`. Cleaner; touches every Bootstrap file (5 files).
   - **Pick Option A** — bootstrap pattern is locked decision; touch surface is minimal. Mark `object LessonRegistry` `internal` and put it inside `data/lesson/internal/`.

3. **Create `data/lesson/LessonRepositoryImpl.kt`:**
   ```kotlin
   class LessonRepositoryImpl : LessonRepository {
       init {
           // idempotent: bootstrap singletons register on first touch.
           // Bootstrap.touch() is safe to call multiple times because of duplicate-id guard.
           LessonRegistry.bootstrap()
       }
       override fun all() = LessonRegistry.all()
       override fun byCategory(c: LessonCategory) = LessonRegistry.byCategory(c)
       override fun byId(id: String) = LessonRegistry.byId(id)
       override fun validate() = LessonRegistry.validateAll()
   }
   ```
   Note: `LessonRegistry.register` raises on duplicate id; if `LessonRepositoryImpl` is `single { }`, it constructs once → safe.

4. **Create `core/di/DataModule.kt`:**
   ```kotlin
   val dataModule = module {
       single<LessonRepository> { LessonRepositoryImpl() }
   }
   ```

5. **Create `core/di/AppModule.kt`** + `core/di/FeatureModule.kt` as empty placeholders (`val appModule = module { }`).

6. **Edit `DreamsApp.kt`:**
   ```kotlin
   class DreamsApp : Application() {
       override fun onCreate() {
           super.onCreate()
           startKoin {
               androidLogger(if (BuildConfig.DEBUG) Level.INFO else Level.ERROR)
               androidContext(this@DreamsApp)
               modules(appModule, dataModule, featureModule)
           }
           if (BuildConfig.DEBUG) {
               val repo: LessonRepository = GlobalContext.get().get()
               repo.validate().forEach { (id, msg) -> Log.e("LessonRepo", "$id => $msg") }
           }
       }
   }
   ```

7. **Update screens that currently call `LessonRegistry.byId()` directly** (LessonDetailScreen.kt:44, ShowcaseScreen.kt:24) to inject `LessonRepository` via Koin. **Defer ViewModel introduction to phase-03** — for now just inject directly:
   ```kotlin
   val repo: LessonRepository = koinInject()
   val lesson = remember(lessonId) { repo.byId(lessonId) }
   ```
   This keeps phase-02 surgical.

8. **Create `app/src/test/java/com/dantech/dreams/core/di/KoinModulesCheckTest.kt`:**
   ```kotlin
   class KoinModulesCheckTest : KoinTest {
       @Test fun checkModules() {
           checkKoinModules(listOf(appModule, dataModule, featureModule)) {
               androidContext(mockk(relaxed = true)) // or use Koin's MockProviderRule
           }
       }
   }
   ```
   Adapt for Koin 4 API: use `verify()` extension if `checkKoinModules` deprecated.

9. **Update `LessonRegistryTest.kt`:** replace `LessonRegistry.bootstrap()` call sites with `LessonRepositoryImpl()` (constructor performs bootstrap). Assertions identical.

10. **Run `./gradlew test`** — both `LessonRegistryTest` (renamed `LessonRepositoryImplTest`) and `KoinModulesCheckTest` green. **Run `./gradlew :app:installDebug`** — app launches, lesson navigation works.

## 8. Todo
- [ ] `LessonRepository` interface created
- [ ] `LessonRegistry` made internal; `LessonRepositoryImpl` wraps it
- [ ] `appModule` / `dataModule` / `featureModule` files created
- [ ] `startKoin {}` in `DreamsApp.onCreate`
- [ ] `LessonDetailScreen` + `ShowcaseScreen` use injected repo via `koinInject()`
- [ ] `KoinModulesCheckTest` green
- [ ] Existing registry test renamed + green
- [ ] App launches, all 23 lessons render

## 9. Success Criteria
- `./gradlew test` passes.
- `./gradlew :app:installDebug` runs; manual smoke test: landing → gallery → 1 lesson per category renders.
- `grep -r "LessonRegistry\." app/src/main/java | grep -v "internal/LessonRegistry.kt\|LessonRepositoryImpl.kt"` → no UI-layer hits.

## 10. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Bootstrap singletons (`BasicsBootstrap` etc.) re-register on test re-instantiation | Med | High | `LessonRegistry.register` already throws on duplicate id; instantiate `LessonRepositoryImpl` once per test class with `@Before`-`object`-cached pattern, OR add `clear()` test helper |
| Koin `checkModules` API changed in 4.x | Med | Med | Use `koinApplication { modules(...) }.checkModules()` if `checkKoinModules` removed |
| Direct `koinInject()` in composables creates implicit testing pain | Low | Med | Phase-03 immediately replaces with VMs — this phase is transitional only |
| `androidContext()` not available in unit test JVM | High | Low | Use `koin-test` `MockProvider` or skip Android-context-requiring single in modules check (split modules so `dataModule` is JVM-pure) |

## 11. Security Considerations
- DI exposes no new attack surface. `LessonRepository` is read-only; impl has no I/O.

## 12. Next Steps
- Phase-03 fills `featureModule` with `viewModel { ... }` declarations and migrates screens off `koinInject()` to `koinViewModel()`.

## Unresolved Questions
- **Koin 4.x `checkModules` API:** confirm exact entry point name and signature for 4.0.0-stable; doc snippet from researcher-01 shows `checkModules()` but Koin renamed APIs between 3.5 and 4.0.
- **Bootstrap idempotency for tests:** if every test class instantiates `LessonRepositoryImpl()` and `register` throws on dup, do we need `LessonRegistry.clear()` before bootstrap? Decide in phase-07 fakes design.
