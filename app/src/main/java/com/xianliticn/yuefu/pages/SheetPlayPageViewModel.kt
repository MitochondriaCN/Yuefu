package com.xianliticn.yuefu.pages

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xianliticn.yuefu.AppDatabase
import com.xianliticn.yuefu.music.MidiEvent
import com.xianliticn.yuefu.music.Parser
import com.xianliticn.yuefu.music.SequenceEngine
import com.xianliticn.yuefu.ui.components.VisualNoteEvent
import com.xianliticn.yuefu.ui.theme.Orange800
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

            _uiState.emit(uiState.value.copy(notes = generateVisualNoteEvents(events)))

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

    private fun generateVisualNoteEvents(events: List<MidiEvent>): List<VisualNoteEvent> {
        val visualNotes = mutableListOf<VisualNoteEvent>()

        events.forEach { event ->
            // 计算在一个八度内的相对音高 (0-11)
            val noteInOctave = event.pitch % 12
            // 计算所在的八度 (假设 MIDI 0 是第 -1 个八度的 C，或者直接作为偏移量)
            val octave = event.pitch / 12

            // 一个八度有 7 个白键。
            // currentOctaveStart 是当前八度第一个白键(C)的全局索引
            val currentOctaveStart = octave * 7

            // 定义八度内每个半音相对于 C 的白键偏移量
            // C=0, C#=0.5, D=1, D#=1.5, E=2, F=3, F#=3.5, G=4, G#=4.5, A=5, A#=5.5, B=6
            val offset = when (noteInOctave) {
                0 -> 0f     // C
                1 -> 0.5f   // C#
                2 -> 1f     // D
                3 -> 1.5f   // D#
                4 -> 2f     // E
                5 -> 3f     // F
                6 -> 3.5f   // F#
                7 -> 4f     // G
                8 -> 4.5f   // G#
                9 -> 5f     // A
                10 -> 5.5f  // A#
                11 -> 6f    // B
                else -> 0f
            }

            visualNotes.add(
                VisualNoteEvent(
                    startTimeMillis = event.timeNano / 1_000_000,
                    endTimeMillis = event.timeNano / 1_000_000 + 500,
                    keyIndex = currentOctaveStart + offset,
                    color = Orange800
                )
            )
        }

        return visualNotes
    }

    data class UiState(
        val notes: List<VisualNoteEvent> = emptyList(),
        val currentProgressMillis: Long = 0,
        val isPlaying: Boolean = false
    )
}