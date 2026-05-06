package com.dantech.dreams.ui.feature.showcase

import app.cash.turbine.test
import com.dantech.dreams.support.FakeLessonRepository
import com.dantech.dreams.support.MainCoroutineRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ShowcaseViewModelTest {

    @get:Rule
    val main = MainCoroutineRule()

    @Test
    fun `init resolves showcase lesson`() = runTest {
        val vm = ShowcaseViewModel(FakeLessonRepository(), "showcase-test")
        vm.uiState.test {
            val state = awaitItem()
            assertNotNull(state.lesson)
            assertEquals("Test Showcase", state.lesson?.title)
            assertFalse(state.hideUi)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleUi flips hideUi`() = runTest {
        val vm = ShowcaseViewModel(FakeLessonRepository(), "showcase-test")
        vm.toggleUi()
        vm.uiState.test {
            assertTrue(awaitItem().hideUi)
            cancelAndIgnoreRemainingEvents()
        }
        vm.toggleUi()
        vm.uiState.test {
            assertFalse(awaitItem().hideUi)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
