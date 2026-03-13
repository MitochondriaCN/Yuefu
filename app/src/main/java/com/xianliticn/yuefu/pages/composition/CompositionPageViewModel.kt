package com.xianliticn.yuefu.pages.composition

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class CompositionPageViewModel @Inject constructor() : ViewModel() {
    fun handlePromptChange(prompt: String) {
        _uiState.value = _uiState.value.copy(prompt = prompt)
    }

    fun handleSendClick() {
        TODO("Not yet implemented")
    }

    fun handleKeyChange(key: String) {
        _uiState.value = _uiState.value.copy(selectedKey = key)
    }

    fun handleInstrumentChange(instrument: String) {
        _uiState.value = _uiState.value.copy(selectedInstrument = instrument)
    }

    private val _uiState = MutableStateFlow(
        CompositionPageState(
            instruments = instruments,
            keys = keys
        )
    )
    val uiState: StateFlow<CompositionPageState> = _uiState
}

data class CompositionPageState(
    val instruments: List<String> = emptyList(),
    val keys: List<String> = emptyList(),
    val selectedKey: String? = keys.firstOrNull(),
    val selectedInstrument: String? = instruments.firstOrNull(),
    val prompt: String? = null
)

private val keys = listOf(
    "C", "C#/Db", "D", "D#/Eb", "E", "F", "F#/Gb", "G", "G#/Ab", "A", "A#/Bb", "B"
)

private val instruments = listOf(
    "钢琴", "小提琴", "长笛", "小号"
)