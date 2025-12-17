package com.xianliticn.yuefu.pages

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xianliticn.yuefu.AppDatabase
import com.xianliticn.yuefu.music.MidiEvent
import com.xianliticn.yuefu.music.Parser
import com.xianliticn.yuefu.music.SequenceEngine
import com.xianliticn.yuefu.music.VisualNoteEvent
import com.xianliticn.yuefu.utils.getAbsoluteImportFilePath
import com.xianliticn.yuefu.utils.readXml
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File

@HiltViewModel
class SheetPlayPageViewModel @Inject constructor(
    private val appDatabase: AppDatabase
) : ViewModel() {
    @Inject
    @ApplicationContext
    lateinit var context: Context

    private var sheetId: Int = 0
    private var se = SequenceEngine()
    private var events: List<MidiEvent> = emptyList()

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    fun refresh(sheetId: Int) {
        this.sheetId = sheetId

        viewModelScope.launch {
            val sheet = appDatabase.sheetDao().getById(sheetId)
            val sheetDoc =
                readXml(File(context.getAbsoluteImportFilePath(sheet!!.fileName)))
            events = Parser().generateMidiEvents(sheetDoc)
            se.load(events)

            _uiState.emit(uiState.value.copy(notes = Parser().generateVisualNoteEvents(events)))

            //持续更新进度
            se.currentProgressMillis.onEach { p ->
                _uiState.emit(uiState.value.copy(currentProgressMillis = p))
            }.launchIn(viewModelScope)
        }
    }

    fun handlePlayOrPause() {
        viewModelScope.launch {
            if (_uiState.value.isPlaying) {
                _uiState.emit(uiState.value.copy(isPlaying = false))
                se.pause()
            } else {
                _uiState.emit(uiState.value.copy(isPlaying = true))
                se.play()
            }
        }
    }

    fun handleProgressChange(progress: Float) {
        viewModelScope.launch {
            _uiState.emit(uiState.value.copy(currentProgressMillis = progress.toLong()))
            se.changeProgress(progress.toLong())
        }
    }

    data class UiState(
        val notes: List<VisualNoteEvent> = emptyList(),
        val currentProgressMillis: Long = 0,
        val isPlaying: Boolean = false
    )
}