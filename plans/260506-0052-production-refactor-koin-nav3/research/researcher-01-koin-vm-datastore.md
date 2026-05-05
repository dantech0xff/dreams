---
name: Koin 4.x, ViewModel+UiState, DataStore for Compose Refactor
description: Production patterns for DI, state mgmt, prefs storage in Compose-only app (minSdk 33, Compose BOM 2026.02.01)
type: reference
---

# Koin 4.x + ViewModel + DataStore Research

## 1. Koin 4.x for Compose

**Module structure:** Organize as `appModule` (entry point), `dataModule` (repos, DataStore), `featureModule` (VMs, feature-specific singletons). Declare with `module { }` DSL.

**ViewModel injection:**
- Use `viewModel { MyViewModel(get(), get()) }` for constructor injection of dependencies
- `koinViewModel()` in @Composable replaces nav-scoped VM instantiation; automatically tied to Compose lifecycle
- `koin-androidx-compose` package includes both `koin-compose` + `koin-compose-viewmodel` — simplifies transitive deps
- **No longer need `KoinAndroidContext`** — context auto-detected at runtime (Koin 4.0+)
- For nav-entry-scoped VMs: use explicit scope wrappers with `scope(named("navEntry")) { viewModel { } }` if needed, but `koinViewModel()` already handles lifecycle correctly for most cases

**Constructor vs `inject()`:**
- Constructor injection preferred for DI testing + explicit deps; `inject()` delegate for lazy initialization in singletons/companion objects
- For VMs: always constructor inject via `viewModel { }` DSL — avoids hidden dependencies

## 2. ViewModel + UiState Pattern

**Core structure:**
```kotlin
data class LessonUiState(
    val lessons: List<Lesson> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class LessonViewModel(private val lessonRepo: LessonRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(LessonUiState())
    val uiState: StateFlow<LessonUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            lessonRepo.getLessons()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = emptyList()
                )
                .collect { lessons -> _uiState.update { it.copy(lessons = lessons) } }
        }
    }
}
```

**SavedStateHandle for process death:**
- Inject `SavedStateHandle` in VM constructor (provided by AndroidX/Hilt/Koin automatically)
- Use `savedStateHandle.getStateFlow<T>(key, defaultValue)` for reactive persistence
- Pattern: `val selectedId: StateFlow<Int> = savedStateHandle.getStateFlow("selectedId", 0)`
- Values survive process termination; scope tied to task stack (lost on force-stop)

**`WhileSubscribed(5_000)` stop-timeout:**
- Prevents upstream Flow re-collection when config change pauses Compose; 5s grace period for recomposition
- If no subscriber for 5s, upstream stops; resumes when re-subscribed
- Balances memory (stop upstream) vs latency (quick restart after brief pause)

## 3. DataStore Preferences

**Koin singleton wiring:**
```kotlin
val dataModule = module {
    single<DataStore<Preferences>> {
        createDataStore(
            context = androidContext(),
            name = "app_prefs"
        )
    }
    single<UserPrefsRepository> {
        UserPrefsRepositoryImpl(dataStore = get())
    }
}
```

**Repository expose as Flow:**
```kotlin
class UserPrefsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : UserPrefsRepository {
    override fun getUserPrefs(): Flow<UserPrefs> =
        dataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences())
                else throw exception
            }
            .map { prefs ->
                UserPrefs(
                    themeDark = prefs[THEME_DARK_KEY] ?: false
                )
            }
}
```

**Testing:** Use in-memory DataStore via `FakeDataStore` or `PreferenceDataStoreFactory.create(scope = testScope, name = "test")` in test context. Default values inlined in `.map {}` blocks.

**Migration:** DataStore handles backward compat; for SharedPreferences → DataStore, provide custom migration logic in catch block or use DataStore's built-in migration (if available in version).

## 4. Testing

**Turbine + JUnit4 for StateFlow:**
```kotlin
@RunWith(JUnit4::class)
class LessonViewModelTest {
    private lateinit var vm: LessonViewModel
    private val fakeRepo = FakeLessonRepository()
    
    @Before
    fun setup() {
        vm = LessonViewModel(lessonRepo = fakeRepo)
    }
    
    @Test
    fun testLoadLessons() = runTest {
        turbineScope {
            val uiState = vm.uiState.testIn(backgroundScope)
            uiState.awaitItem() // initial
            fakeRepo.emitLessons(listOf(...))
            val loaded = uiState.awaitItem()
            assertTrue(loaded.lessons.isNotEmpty())
        }
    }
}
```

**Koin module verification:** `checkModules()` scans module graph for missing deps at test startup (before any VM instantiation). Early catch of DI errors.

**MainCoroutineRule:** Set test dispatcher via `Dispatchers.setMain(UnconfinedTestDispatcher())` in `@Before`, reset in `@After`. Ensures VM viewModelScope uses test dispatcher.

**Skip screenshot tests** for shader gallery output — too brittle; unit test UiState transitions instead. Functional tests on shader output belong in integration suite.

---

## Unresolved Questions
1. **Koin 4.x BOM alignment:** Does Compose BOM 2026.02.01 pin a specific Koin 4.x version? Confirm transitive dep compatibility.
2. **koin-compose-viewmodel artifact naming:** Is it `koin-androidx-compose` or separate `koin-compose-viewmodel` in latest releases? Verify against Compose BOM.
3. **DataStore serialization for complex UiState:** If UiState contains non-Parcelable objects, does repository pattern shield VM from serialization overhead, or serialize entire state object?
4. **Nav-scoped VM lifecycle with Koin:** Does `koinViewModel()` respect nav-entry scope or only Compose lifecycle? May need explicit scope wrapping for multi-screen galleries.

**Status:** DONE  
**Summary:** Koin 4.x + ViewModel StateFlow + SavedStateHandle + DataStore repository pattern is production-ready; auto-lifecycle detection removes boilerplate vs 3.x. WhileSubscribed(5_000) balances memory/latency. Testing via Turbine + MainCoroutineRule covers async state; skip shader output tests.  
**Concerns:** BOM version alignment and nav-scope lifecycle binding need confirmation before implementation.
