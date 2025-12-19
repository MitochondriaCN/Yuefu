package com.xianliticn.yuefu.music

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.billthefarmer.mididriver.MidiDriver

/**
 * 音序引擎。
 */
class SequenceEngine {
    private var sequence: List<MidiEvent>? = null
    private var isPlaying = false
    private var startTimeNano = 0L
    private var pausedProgressNano = 0L
    private var eventIndex = 0

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private var playJob: Job? = null
    private var progressJob: Job? = null

    private val midiDriver = MidiDriver.getInstance()

    private val _currentProgressMillis = MutableStateFlow(0L)
    val currentProgressMillis = _currentProgressMillis.asStateFlow()

    init {
        midiDriver.setOnMidiStartListener {
            Log.d("DEV", "MidiDriver started")
        }
        midiDriver.start()

        progressJob = scope.launch {
            while (true) {
                if (isPlaying) {
                    val now = System.nanoTime()
                    val elapsedMillis = (now - startTimeNano) / 1_000_000
                    _currentProgressMillis.emit(elapsedMillis)
                } else {
                    _currentProgressMillis.emit(pausedProgressNano / 1_000_000)
                }

                // 1000ms / 32ms = 31.25
                // 即以31.25Hz更新当前进度
                delay(32)
            }
        }
    }

    fun load(midiEvents: List<MidiEvent>) {
        sequence = midiEvents.sortedBy { it.timeNano }
    }

    fun play() {
        if (sequence == null)
            throw Exception("尚未加载音频序列")

        if (isPlaying)
            return

        isPlaying = true
        changeProgress(pausedProgressNano / 1_000_000)

        playJob = scope.launch {
            eventIndex = 0

            while (isPlaying && eventIndex < (sequence?.size ?: -1)) {
                val event = sequence?.get(eventIndex)

                if (event == null) {
                    break
                }
                if (event.isSent) {
                    eventIndex++
                    continue
                }

                val now = System.nanoTime()
                // 计算当前播放进度（已经播放了多久）
                val elapsedNano = now - startTimeNano

                // 如果当前进度还没追上事件的预定时间
                if (elapsedNano < event.timeNano) {
                    val waitNano = event.timeNano - elapsedNano
                    val waitMillis = waitNano / 1_000_000

                    // 如果等待时间比较长（比如大于2ms），让协程挂起，节省CPU
                    if (waitMillis > 2) {
                        delay(waitMillis)
                    } else {
                        // 如果等待时间很短（微秒级），用忙等待（Busy Wait）保证精度
                        // 这一步对于音乐节奏至关重要，Thread.sleep/delay 精度不够
                        var loopNow = System.nanoTime()
                        while (loopNow - startTimeNano < event.timeNano) {
                            loopNow = System.nanoTime()
                        }
                    }
                }

                // 时间到了，立即发送
                midiDriver.queueEvent(event.getMidiData())
                event.isSent = true
                eventIndex++
            }

            //全播完
            pause()
        }
    }

    fun stop() {
        //stop() = changeProgress(0L) + pause()
        changeProgress(0L)
        pause()
    }

    fun pause() {
        playJob?.cancel()
        isPlaying = false
        pausedProgressNano = System.nanoTime() - startTimeNano
    }

    fun changeProgress(targetMillis: Long) {
        //要使elapsedMillis = now - startMillis = targetMillis，
        //只需startMillis = now - targetMillis
        val now = System.nanoTime()
        startTimeNano = now - targetMillis * 1_000_000
        pausedProgressNano = targetMillis * 1_000_000

        //设置音符状态
        sequence?.forEach {
            if (it.timeNano >= targetMillis * 1_000_000) { //在指针后
                it.isSent = false
            } else { //在指针前
                it.isSent = true
            }
        }

        eventIndex = 0
    }
}