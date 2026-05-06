package com.dantech.dreams.ui.feature.lesson

import app.cash.turbine.test
import com.dantech.dreams.data.prefs.UserPrefs
import com.dantech.dreams.support.FakeLessonRepository
import com.dantech.dreams.support.FakeUserPrefsRepository
import com.dantech.dreams.support.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
    fun `init hydrates from saved overrides`() = runTest {
        val prefs = FakeUserPrefsRepository(
            UserPrefs(paramOverrides = mapOf("test-basics-1" to mapOf("amplitude" to 0.9f))),
        )
        val vm = LessonDetailViewModel(FakeLessonRepository(), prefs, "test-basics-1")
        runCurrent()
        vm.uiState.test {
            assertEquals(0.9f, awaitItem().paramOverrides["amplitude"])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `resetOverrides clears local and remote`() = runTest {
        val prefs = FakeUserPrefsRepository(
            UserPrefs(paramOverrides = mapOf("test-basics-1" to mapOf("amplitude" to 0.5f))),
        )
        val vm = LessonDetailViewModel(FakeLessonRepository(), prefs, "test-basics-1")
        runCurrent()
        vm.resetOverrides()
        runCurrent()
        vm.uiState.test {
            assertEquals(0, awaitItem().paramOverrides.size)
            cancelAndIgnoreRemainingEvents()
        }
        assertNull(prefs.prefsFlow.first().paramOverrides["test-basics-1"])
    }
}
