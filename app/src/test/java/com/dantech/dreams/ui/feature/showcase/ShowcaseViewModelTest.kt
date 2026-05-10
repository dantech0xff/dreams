package com.dantech.dreams.ui.feature.showcase

import app.cash.turbine.test
import com.dantech.dreams.support.FakeLessonRepository
import com.dantech.dreams.support.MainCoroutineRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
            cancelAndIgnoreRemainingEvents()
        }
    }
}
