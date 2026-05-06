package com.dantech.dreams.ui.feature.landing

import app.cash.turbine.test
import com.dantech.dreams.support.MainCoroutineRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LandingViewModelTest {

    @get:Rule
    val main = MainCoroutineRule()

    @Test
    fun `setAboutOpen toggles state`() = runTest {
        val vm = LandingViewModel()
        vm.uiState.test {
            assertFalse(awaitItem().aboutOpen)
            vm.setAboutOpen(true)
            assertTrue(awaitItem().aboutOpen)
            vm.setAboutOpen(false)
            assertFalse(awaitItem().aboutOpen)
        }
    }
}
