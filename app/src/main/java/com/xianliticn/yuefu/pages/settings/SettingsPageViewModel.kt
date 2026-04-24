package com.xianliticn.yuefu.pages.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xianliticn.yuefu.modules.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsPageViewModel @Inject constructor(
    private val settingsManager: SettingsManager
) : ViewModel() {

    val isTutorialShown: StateFlow<Boolean> = settingsManager.isShowingTutorial
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setTutorialShown(shown: Boolean) {
        viewModelScope.launch {
            settingsManager.setIsShowingTutorial(shown)
        }
    }
}
