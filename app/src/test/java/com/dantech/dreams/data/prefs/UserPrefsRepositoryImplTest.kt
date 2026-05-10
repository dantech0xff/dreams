package com.dantech.dreams.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UserPrefsRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repo: UserPrefsRepository

    @Before
    fun setUp() {
        dataStore = PreferenceDataStoreFactory.create(
            produceFile = { tempFolder.newFile("prefs.preferences_pb") },
        )
        repo = UserPrefsRepositoryImpl(dataStore)
    }

    @Test
    fun `default flow emits empty prefs`() = runTest {
        repo.prefsFlow.test {
            val first = awaitItem()
            assertEquals(UserPrefs.DEFAULT, first)
            assertEquals(ThemeMode.DARK, first.themeMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleFavorite adds then removes id`() = runTest {
        assertTrue(repo.toggleFavorite("a"))
        assertTrue(repo.prefsFlow.test {
            awaitItem().favorites.contains("a")
        }.let { true })
        assertFalse(repo.toggleFavorite("a"))
    }

    @Test
    fun `setParamOverride round trips through datastore`() = runTest {
        repo.setParamOverride("lesson-1", "amp", 0.75f)
        repo.prefsFlow.test {
            val snap = awaitItem()
            assertEquals(0.75f, snap.paramOverrides["lesson-1"]?.get("amp"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setColorOverride round trips through datastore`() = runTest {
        val rose = 0xFFE91E63.toInt()
        repo.setColorOverride("lesson-1", "baseColor", rose)
        repo.prefsFlow.test {
            val snap = awaitItem()
            assertEquals(rose, snap.colorOverrides["lesson-1"]?.get("baseColor"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setLastLessonId persists`() = runTest {
        repo.setLastLessonId("lesson-9")
        repo.prefsFlow.test {
            assertEquals("lesson-9", awaitItem().lastLessonId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setReducedMotion persists`() = runTest {
        repo.setReducedMotion(true)
        repo.prefsFlow.test {
            assertTrue(awaitItem().reducedMotionOverride)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setThemeMode persists`() = runTest {
        repo.setThemeMode(ThemeMode.DARK)
        repo.prefsFlow.test {
            assertEquals(ThemeMode.DARK, awaitItem().themeMode)
            cancelAndIgnoreRemainingEvents()
        }

        repo.setThemeMode(ThemeMode.LIGHT)
        repo.prefsFlow.test {
            assertEquals(ThemeMode.LIGHT, awaitItem().themeMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unsupported theme mode falls back to dark`() = runTest {
        dataStore.edit { it[stringPreferencesKey("theme_mode")] = "unknown" }
        repo.prefsFlow.test {
            assertEquals(ThemeMode.DARK, awaitItem().themeMode)
            cancelAndIgnoreRemainingEvents()
        }

        dataStore.edit { it[stringPreferencesKey("theme_mode")] = "system" }
        repo.prefsFlow.test {
            assertEquals(ThemeMode.DARK, awaitItem().themeMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearLessonOverrides removes only that lesson`() = runTest {
        repo.setParamOverride("a", "u", 1f)
        repo.setParamOverride("b", "v", 2f)
        repo.setColorOverride("a", "color", 0xFFE91E63.toInt())
        repo.setColorOverride("b", "color", 0xFF2196F3.toInt())
        repo.clearLessonOverrides("a")
        repo.prefsFlow.test {
            val snap = awaitItem()
            assertFalse("a" in snap.paramOverrides)
            assertFalse("a" in snap.colorOverrides)
            assertEquals(2f, snap.paramOverrides["b"]?.get("v"))
            assertEquals(0xFF2196F3.toInt(), snap.colorOverrides["b"]?.get("color"))
            cancelAndIgnoreRemainingEvents()
        }
    }
}
