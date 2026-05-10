package com.dantech.dreams.ui.feature.lesson

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.cash.turbine.test
import com.dantech.dreams.data.prefs.UserPrefs
import com.dantech.dreams.support.FakeLessonRepository
import com.dantech.dreams.support.FakeUserPrefsRepository
import com.dantech.dreams.support.MainCoroutineRule
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LessonDetailViewModelTest {

    @get:Rule
    val main = MainCoroutineRule()

    @Test
    fun `init resolves lesson by id and writes lastLessonId`() = runTest {
        val prefs = FakeUserPrefsRepository()
        val vm = LessonDetailViewModel(FakeLessonRepository(), prefs, "test-basics-1")
        runCurrent()
        vm.uiState.test {
            val state = awaitItem()
            assertNotNull(state.lesson)
            assertEquals("Test Basics", state.lesson?.title)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals("test-basics-1", prefs.prefsFlow.first().lastLessonId)
    }

    @Test
    fun `setFloat updates uiState immediately`() = runTest {
        val vm = LessonDetailViewModel(
            FakeLessonRepository(),
            FakeUserPrefsRepository(),
            "test-basics-1",
        )
        runCurrent()
        vm.setFloat("amplitude", 0.5f)
        vm.uiState.test {
            assertEquals(0.5f, awaitItem().paramOverrides["amplitude"])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setColor updates uiState immediately`() = runTest {
        val vm = LessonDetailViewModel(
            FakeLessonRepository(),
            FakeUserPrefsRepository(),
            "test-basics-1",
        )
        runCurrent()
        val color = Color(0xFF2196F3)
        vm.setColor("baseColor", color)
        vm.uiState.test {
            assertEquals(color.toArgb(), awaitItem().colorOverrides["baseColor"])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `init hydrates from saved overrides`() = runTest {
        val color = Color(0xFF2196F3).toArgb()
        val prefs = FakeUserPrefsRepository(
            UserPrefs(
                paramOverrides = persistentMapOf("test-basics-1" to persistentMapOf("amplitude" to 0.9f)),
                colorOverrides = persistentMapOf("test-basics-1" to persistentMapOf("baseColor" to color)),
            ),
        )
        val vm = LessonDetailViewModel(FakeLessonRepository(), prefs, "test-basics-1")
        runCurrent()
        vm.uiState.test {
            val state = awaitItem()
            assertEquals(0.9f, state.paramOverrides["amplitude"])
            assertEquals(color, state.colorOverrides["baseColor"])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `resetOverrides clears local and remote`() = runTest {
        val prefs = FakeUserPrefsRepository(
            UserPrefs(
                paramOverrides = persistentMapOf("test-basics-1" to persistentMapOf("amplitude" to 0.5f)),
                colorOverrides = persistentMapOf("test-basics-1" to persistentMapOf("baseColor" to 0xFFE91E63.toInt())),
            ),
        )
        val vm = LessonDetailViewModel(FakeLessonRepository(), prefs, "test-basics-1")
        runCurrent()
        vm.resetOverrides()
        runCurrent()
        vm.uiState.test {
            val state = awaitItem()
            assertEquals(0, state.paramOverrides.size)
            assertEquals(0, state.colorOverrides.size)
            cancelAndIgnoreRemainingEvents()
        }
        assertNull(prefs.prefsFlow.first().paramOverrides["test-basics-1"])
        assertNull(prefs.prefsFlow.first().colorOverrides["test-basics-1"])
    }

    @Test
    fun `resetOverrides ignores pending debounced float persist`() = runTest {
        val prefs = FakeUserPrefsRepository()
        val vm = LessonDetailViewModel(FakeLessonRepository(), prefs, "test-basics-1")
        runCurrent()

        vm.setFloat("amplitude", 0.5f)
        vm.resetOverrides()
        runCurrent()
        advanceTimeBy(201)
        runCurrent()

        assertNull(prefs.prefsFlow.first().paramOverrides["test-basics-1"])
    }
}
