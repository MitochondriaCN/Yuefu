package com.xianliticn.yuefu.pages.sheetplay

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xianliticn.yuefu.AppDatabase
import com.xianliticn.yuefu.music.MidiEvent
import com.xianliticn.yuefu.music.Parser
import com.xianliticn.yuefu.music.SequenceEngine
import com.xianliticn.yuefu.music.VisualNoteEvent
import com.xianliticn.yuefu.ui.components.EffectLevel
import com.xianliticn.yuefu.modules.SettingsManager
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
import kotlin.math.abs

@HiltViewModel
class SheetPlayPageViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    private val settingsManager: SettingsManager
) : ViewModel() {
    @Inject
    @ApplicationContext
    lateinit var context: Context

    private var sheetId: Int = 0
    private var se = SequenceEngine()
    private var events: List<MidiEvent> = emptyList()
    private var measureTimeline: List<Pair<Int, Long>> = emptyList()
    private var measureStartMillisByMeasure: Map<Int, Long> = emptyMap()

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsManager.effectLevel.collect { level ->
                _uiState.emit(uiState.value.copy(effectLevel = level))
            }
        }
    }

    fun refresh(sheetId: Int) {
        this.sheetId = sheetId

        viewModelScope.launch {
            val sheet = appDatabase.sheetDao().getById(sheetId)
            val sheetDoc =
                readXml(File(context.getAbsoluteImportFilePath(sheet!!.fileName!!)))
            val parser = Parser()
            events = parser.generateMidiEvents(sheetDoc)
            se.load(events)
            measureTimeline = buildMeasureTimeline(events)
            measureStartMillisByMeasure = measureTimeline.toMap()

            _uiState.emit(
                uiState.value.copy(
                    notes = parser.generateVisualNoteEvents(events),
                    currentMeasure = resolveCurrentMeasure(0L),
                    maxMeasure = measureTimeline.maxOfOrNull { it.first } ?: 1
                )
            )

            //持续更新进度
            se.currentProgressMillis.onEach { p ->
                _uiState.emit(
                    uiState.value.copy(
                        currentProgressMillis = p,
                        currentMeasure = resolveCurrentMeasure(p)
                    )
                )
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
            _uiState.emit(
                uiState.value.copy(
                    currentProgressMillis = progress.toLong(),
                    currentMeasure = resolveCurrentMeasure(progress.toLong())
                )
            )
            se.changeProgress(progress.toLong())
        }
    }

    fun handleMeasureChange(measure: Int) {
        val target = measureStartMillisByMeasure[measure]
            ?: measureStartMillisByMeasure.entries
                .minByOrNull { (m, _) -> abs(m - measure) }
                ?.value
            ?: 0L
        handleProgressChange(target.toFloat())
    }

    private fun buildMeasureTimeline(events: List<MidiEvent>): List<Pair<Int, Long>> {
        val measureToStartNano = mutableMapOf<Int, Long>()
        events.forEach { event ->
            val measure = event.measure ?: return@forEach
            val currentStart = measureToStartNano[measure]
            if (currentStart == null || event.timeNano < currentStart) {
                measureToStartNano[measure] = event.timeNano
            }
        }
        return measureToStartNano
            .map { (measure, startNano) -> measure to (startNano / 1_000_000) }
            .sortedBy { it.second }
    }

    private fun resolveCurrentMeasure(progressMillis: Long): Int {
        if (measureTimeline.isEmpty()) return 1
        var currentMeasure = measureTimeline.first().first
        for ((measure, startMillis) in measureTimeline) {
            if (progressMillis >= startMillis) {
                currentMeasure = measure
            } else {
                break
            }
        }
        return currentMeasure
    }

    data class UiState(
        val notes: List<VisualNoteEvent> = emptyList(),
        val currentProgressMillis: Long = 0,
        val isPlaying: Boolean = false,
        val currentMeasure: Int = 1,
        val maxMeasure: Int = 1,
        val effectLevel: EffectLevel = EffectLevel.HIGH
    )
}
