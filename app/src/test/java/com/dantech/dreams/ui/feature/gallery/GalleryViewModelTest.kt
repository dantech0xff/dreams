package com.dantech.dreams.ui.feature.gallery

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.support.FakeLessonRepository
import com.dantech.dreams.support.FakeUserPrefsRepository
import com.dantech.dreams.support.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryViewModelTest {

    @get:Rule
    val main = MainCoroutineRule()

    @Test
    fun `initial state seeds basics tab`() = runTest {
        val vm = GalleryViewModel(FakeLessonRepository(), FakeUserPrefsRepository(), SavedStateHandle())
        runCurrent()
        vm.uiState.test {
            val state = awaitItem()
            assertEquals(0, state.selectedTabIndex)
            assertEquals(LessonCategory.BASICS, state.categories[0])
            assertTrue(state.lessons.any { it.id == "test-basics-1" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selectTab swaps lessons`() = runTest {
        val vm = GalleryViewModel(FakeLessonRepository(), FakeUserPrefsRepository(), SavedStateHandle())
        vm.selectTab(LessonCategory.SHOWCASE.ordinal)
        runCurrent()
        vm.uiState.test {
            val state = awaitItem()
            assertEquals(LessonCategory.SHOWCASE, state.categories[state.selectedTabIndex])
            assertTrue(state.lessons.any { it.id == "showcase-test" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleFavorite reflects in uiState`() = runTest {
        val prefs = FakeUserPrefsRepository()
        val vm = GalleryViewModel(FakeLessonRepository(), prefs, SavedStateHandle())
        runCurrent()
        vm.toggleFavorite("test-basics-1")
        runCurrent()
        vm.uiState.test {
            assertTrue("test-basics-1" in awaitItem().favorites)
            cancelAndIgnoreRemainingEvents()
        }
        vm.toggleFavorite("test-basics-1")
        runCurrent()
        vm.uiState.test {
            assertFalse("test-basics-1" in awaitItem().favorites)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
