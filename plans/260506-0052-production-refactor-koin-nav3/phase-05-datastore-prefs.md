# Phase 05 — DataStore Prefs

## 1. Context Links
- Parent: [plan.md](plan.md)
- Depends on: [phase-03-viewmodels-and-uistate.md](phase-03-viewmodels-and-uistate.md) (VMs exist) — can run parallel-ish to phase-04 once both phases settle on signatures
- Inputs: `research/researcher-01-koin-vm-datastore.md` §3
- Docs: https://developer.android.com/topic/libraries/architecture/datastore | https://github.com/Kotlin/kotlinx.serialization

## 2. Overview
- **Date:** 2026-05-06
- **Description:** Add `UserPrefsRepository` backed by Preferences DataStore. Persist: last-viewed lesson id (resume on cold start optional), favorites set, per-lesson Float param overrides (JSON via kotlinx.serialization), reduced-motion override toggle. Wire into VMs.
- **Priority:** P2 (no upstream blockers; UX nice-to-have, not core flow)
- **Implementation status:** pending
- **Review status:** pending

## 3. Key Insights
- Preferences DataStore (NOT Proto) — schema is small + flat, no need for proto codegen complexity (KISS).
- `Preferences` keys: `stringPreferencesKey("last_lesson_id")`, `stringSetPreferencesKey("favorites")`, `stringPreferencesKey("param_overrides")` (JSON-encoded `Map<String, Map<String, Float>>` → outer key = lessonId, inner = uniformName→value), `booleanPreferencesKey("reduced_motion")`.
- Flow exposed by repo: `Flow<UserPrefs>` (one cold flow, mapped from `dataStore.data`). VMs `stateIn(viewModelScope, WhileSubscribed(5_000), UserPrefs.DEFAULT)`.
- Param overrides JSON keeps schema flexible — add new uniform without migration. ~5 KB ceiling unless user touches every lesson.
- Reduced-motion override is OS-supplement: app honors `Settings.Global.ANIMATOR_DURATION_SCALE == 0` AND user toggle (OR-combined). Phase-06 reads the combined flag.
- Last-lesson resume: out of scope for navigation auto-replay (can introduce nav-loop bugs). Use only as Gallery "recent lessons" hint OR drop. **Decision:** persist but UI surface is just a "Recently viewed" subtitle on Gallery — no auto-nav.

## 4. Requirements

### Functional
- `UserPrefsRepository`:
  - `val prefsFlow: Flow<UserPrefs>` — emits whenever any pref changes
  - `suspend fun setLastLessonId(id: String)`
  - `suspend fun toggleFavorite(id: String): Boolean` — returns new state (true=now-favorite)
  - `suspend fun setParamOverride(lessonId: String, uniform: String, value: Float)`
  - `suspend fun clearLessonOverrides(lessonId: String)`
  - `suspend fun setReducedMotion(b: Boolean)`
- `UserPrefs` data class: `lastLessonId: String?`, `favorites: Set<String>`, `paramOverrides: Map<String, Map<String, Float>>`, `reducedMotion: Boolean`.
- `LessonDetailViewModel` writes `lastLessonId` on init (where lesson resolves), `paramOverride` on slider change.
- `GalleryViewModel.uiState` exposes `favorites: Set<String>` + `lastLessonId`.
- VM constructor takes `UserPrefsRepository`; populated via Koin in phase-02's `dataModule`.

### Non-Functional
- All writes are `suspend` and run on `Dispatchers.IO` (DataStore handles internally).
- Unbounded growth prevention: `paramOverrides` map shouldn't carry stale lessons. Document as known issue; cleanup is V2 (YAGNI).
- DataStore file name: `dreams_prefs`.

## 5. Architecture

```
DataStore<Preferences>            ← single in dataModule
       │
       ▼
UserPrefsRepository (Flow + suspend writes)
       │
       ├──► GalleryViewModel ─► UiState.favorites, .lastLessonId
       ├──► LessonDetailViewModel ─► hydrate paramOverrides on first compose
       │                       ─► writeOnControlChange(uniform, value)
       └──► AppMotionState (phase-06) ─► reducedMotion combined w/ system

UI
  Gallery card: ★ icon → vm.toggleFavorite(lesson.id)
  Detail slider: change → BOTH controlValuesMap[uniform]=v (per frame) AND vm.persistParam(uniform, v)
  Settings sheet (new, optional this phase): toggle reduced-motion override
```

## 6. Related Code Files

### Modify
- `core/di/DataModule.kt` — add DataStore single + UserPrefsRepository single
- `core/di/FeatureModule.kt` — VMs that need prefs gain `get<UserPrefsRepository>()` constructor arg
- `ui/feature/gallery/GalleryViewModel.kt` — combine repo lessons + prefs.favorites into UiState
- `ui/feature/gallery/GalleryUiState.kt` — add `favorites: PersistentSet<String>`, `lastLessonId: String?`
- `ui/feature/gallery/LessonCard.kt` — add ★ favorite toggle button
- `ui/feature/lesson/LessonDetailViewModel.kt` — hydrate from prefs + persist on `setFloat`
- `ui/feature/lesson/LessonDetailUiState.kt` — `paramOverrides` already there, hydrated from prefs

### Create
- `data/prefs/UserPrefs.kt` — data class + DEFAULT
- `data/prefs/UserPrefsRepository.kt` — interface
- `data/prefs/UserPrefsRepositoryImpl.kt` — Preferences DataStore impl
- `data/prefs/UserPrefsKeys.kt` — `stringPreferencesKey` etc. (private inside impl OK)
- `data/prefs/ParamOverridesCodec.kt` — kotlinx.serialization encode/decode of `Map<String, Map<String, Float>>`

### Delete
- None

## 7. Implementation Steps

1. **Create `data/prefs/UserPrefs.kt`:**
   ```kotlin
   data class UserPrefs(
       val lastLessonId: String? = null,
       val favorites: Set<String> = emptySet(),
       val paramOverrides: Map<String, Map<String, Float>> = emptyMap(),
       val reducedMotionOverride: Boolean = false
   ) {
       companion object { val DEFAULT = UserPrefs() }
   }
   ```

2. **Create `data/prefs/UserPrefsRepository.kt`:**
   ```kotlin
   interface UserPrefsRepository {
       val prefsFlow: Flow<UserPrefs>
       suspend fun setLastLessonId(id: String)
       suspend fun toggleFavorite(id: String): Boolean
       suspend fun setParamOverride(lessonId: String, uniform: String, value: Float)
       suspend fun clearLessonOverrides(lessonId: String)
       suspend fun setReducedMotion(b: Boolean)
   }
   ```

3. **Create `data/prefs/ParamOverridesCodec.kt`:**
   ```kotlin
   private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
   fun encodeOverrides(m: Map<String, Map<String, Float>>): String =
       json.encodeToString(MapSerializer(String.serializer(), MapSerializer(String.serializer(), Float.serializer())), m)
   fun decodeOverrides(s: String): Map<String, Map<String, Float>> = runCatching {
       json.decodeFromString(MapSerializer(String.serializer(), MapSerializer(String.serializer(), Float.serializer())), s)
   }.getOrDefault(emptyMap())
   ```

4. **Create `data/prefs/UserPrefsRepositoryImpl.kt`:**
   ```kotlin
   class UserPrefsRepositoryImpl(
       private val dataStore: DataStore<Preferences>
   ) : UserPrefsRepository {
       private object Keys {
           val LAST = stringPreferencesKey("last_lesson_id")
           val FAVS = stringSetPreferencesKey("favorites")
           val OVERRIDES = stringPreferencesKey("param_overrides")
           val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
       }
       override val prefsFlow: Flow<UserPrefs> = dataStore.data
           .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
           .map { p ->
               UserPrefs(
                   lastLessonId = p[Keys.LAST],
                   favorites = p[Keys.FAVS] ?: emptySet(),
                   paramOverrides = decodeOverrides(p[Keys.OVERRIDES] ?: ""),
                   reducedMotionOverride = p[Keys.REDUCED_MOTION] ?: false,
               )
           }
       override suspend fun setLastLessonId(id: String) {
           dataStore.edit { it[Keys.LAST] = id }
       }
       override suspend fun toggleFavorite(id: String): Boolean {
           var now = false
           dataStore.edit { p ->
               val cur = p[Keys.FAVS] ?: emptySet()
               now = id !in cur
               p[Keys.FAVS] = if (now) cur + id else cur - id
           }
           return now
       }
       override suspend fun setParamOverride(lessonId: String, uniform: String, value: Float) {
           dataStore.edit { p ->
               val cur = decodeOverrides(p[Keys.OVERRIDES] ?: "").toMutableMap()
               val inner = cur[lessonId].orEmpty().toMutableMap().apply { put(uniform, value) }
               cur[lessonId] = inner
               p[Keys.OVERRIDES] = encodeOverrides(cur)
           }
       }
       override suspend fun clearLessonOverrides(lessonId: String) {
           dataStore.edit { p ->
               val cur = decodeOverrides(p[Keys.OVERRIDES] ?: "").toMutableMap()
               cur.remove(lessonId)
               p[Keys.OVERRIDES] = encodeOverrides(cur)
           }
       }
       override suspend fun setReducedMotion(b: Boolean) {
           dataStore.edit { it[Keys.REDUCED_MOTION] = b }
       }
   }
   ```

5. **Edit `core/di/DataModule.kt`:**
   ```kotlin
   val dataModule = module {
       single<DataStore<Preferences>> {
           PreferenceDataStoreFactory.create(produceFile = { androidContext().preferencesDataStoreFile("dreams_prefs") })
       }
       single<LessonRepository> { LessonRepositoryImpl() }
       single<UserPrefsRepository> { UserPrefsRepositoryImpl(get()) }
   }
   ```

6. **Edit `GalleryViewModel`:** combine `LessonRepository` data with `UserPrefsRepository.prefsFlow`. Use `combine(lessonsFlow, prefsFlow) { ... }.stateIn(viewModelScope, WhileSubscribed(5_000), GalleryUiState())`. (`lessonsFlow` is `flowOf(repo.byCategory(...))` keyed by tab — use `_selectedTab.flatMapLatest`.)

7. **Edit `LessonDetailViewModel`:** in `init`, `viewModelScope.launch { prefsRepo.prefsFlow.first().paramOverrides[lessonId]?.let { hydrate it into _ui } }`. In `setFloat()`, call `_ui.update { ... }` AND `viewModelScope.launch { prefsRepo.setParamOverride(lessonId, uniform, value) }`. Debounce slider with 200ms `flow.debounce` if jank observed (defer optimization).
   - Also call `prefsRepo.setLastLessonId(lessonId)` in `init`.

8. **Edit `LessonCard.kt`** (gallery): pass `isFavorite: Boolean` + `onToggleFavorite: () -> Unit` props from `GalleryScreen`. Render filled vs outlined ★ icon. (Use Material `Icons.Outlined.StarBorder`/`Icons.Filled.Star`.)

9. **Edit `GalleryScreen.kt`:** pass favorite state + click handler from `vm.uiState.favorites` + `vm.toggleFavorite(id)`.

10. **Build + smoke-test:** install, toggle favorite on a lesson, force-stop app via `adb shell am force-stop com.dantech.dreams`, relaunch, verify ★ persists. Open lesson, change slider, back, reopen, verify slider position restored.

## 8. Todo
- [ ] `UserPrefs` data class + DEFAULT
- [ ] `UserPrefsRepository` interface + Preferences DataStore impl
- [ ] `ParamOverridesCodec` JSON round-trip works
- [ ] `dataModule` exposes DataStore + UserPrefsRepository singles
- [ ] `GalleryViewModel` consumes prefs (favorites in UiState)
- [ ] `LessonDetailViewModel` hydrates + persists overrides
- [ ] `LessonCard` ★ toggle UI
- [ ] Force-stop+relaunch test confirms favorites + overrides survive
- [ ] Reduced-motion toggle persists (consumed in phase-06)

## 9. Success Criteria
- App-killed-and-relaunched: favorite ★ remains, slider position remains.
- `prefsFlow` emits new value within ~200ms of any write (DataStore commit latency).
- `./gradlew test` includes `UserPrefsRepositoryTest` (in phase-07) verifying JSON round-trip + favorites toggle.
- No `runBlocking` calls in UI code.

## 10. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| DataStore corrupt/IOException blocks app launch | Low | High | `catch { if IOException emit emptyPreferences() }` already in flow. Repository never throws on read |
| JSON encoding of overrides grows unbounded across many lessons | Low | Med | Document as V2 cleanup. Realistic ceiling: 23 lessons × 5 floats × 16 bytes ≈ 2KB |
| Slider write rate (every drag tick) hammers DataStore | High | Med | Debounce in VM: `slider events → MutableSharedFlow → debounce(200ms) → persist`. Defer to phase-06 if jank observed |
| `combine()` stateIn restart on rotation drops in-flight write | Low | Low | Writes go through repo on `viewModelScope`, not the state flow chain |
| kotlinx.serialization plugin not applied (forgot in phase-01) | Med | High | Phase-01 todo gates this; verify codec test green before proceeding |

## 11. Security Considerations
- DataStore is internal app storage (sandbox); no encryption needed for non-sensitive prefs (no PII).
- JSON deserialization wrapped in `runCatching` — malformed pref string never crashes app.
- No network sync. No exfiltration vector.

## 12. Next Steps
- Phase-06 reads `UserPrefs.reducedMotionOverride` to control transition duration.
- Phase-07 unit-tests `UserPrefsRepositoryImpl` against a test DataStore + Turbine.

## Unresolved Questions
- **DataStore preferencesDataStoreFile delegate vs factory:** confirm `PreferenceDataStoreFactory.create(produceFile = { ctx.preferencesDataStoreFile("name") })` is the right API for 1.1.1 — alt is `Context.dataStore by preferencesDataStore(name = "...")` extension.
- **Slider debounce strategy:** push every event vs debounce 200ms vs sample on `up` event? Decide after measuring write rate on physical device.
- **Last-lesson resume UX:** persist `lastLessonId` is in scope; using it for auto-replay on cold launch is OUT (UX risk). Ship as data only — UI surface deferred.
