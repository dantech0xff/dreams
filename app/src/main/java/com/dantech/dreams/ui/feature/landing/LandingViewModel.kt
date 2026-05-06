package com.dantech.dreams.ui.feature.landing

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LandingViewModel : ViewModel() {

    private val _ui = MutableStateFlow(LandingUiState())
    val uiState: StateFlow<LandingUiState> = _ui.asStateFlow()

    fun setAboutOpen(open: Boolean) {
        _ui.update { it.copy(aboutOpen = open) }
    }
}
