package com.xianliticn.yuefu.music

import android.media.midi.MidiReceiver
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 音序引擎。
 */
class SequenceEngine(private val midiReceiver: MidiReceiver) {
    private var sequence: List<MidiEvent>? = null
    private var isPlaying = false
    private var startTimeNano = 0L
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var playJob: Job? = null

    private val LOOK_AHEAD_TIME_MILLIS = 50L //前瞻50ms
    private val LOOP_INTERVAL_MILLIS = 20L   //循环间隔20ms

    fun play(midiEvents: List<MidiEvent>) {
        sequence = ArrayList(midiEvents)
        isPlaying = true
        startTimeNano = System.nanoTime()

        playJob?.cancel()

        playJob = scope.launch {
            Log.d("DEV", "Sequence engine started")
            while (isPlaying) {
                val now = System.nanoTime()
                val scheduleUntil = now - startTimeNano + (LOOK_AHEAD_TIME_MILLIS * 1_000_000)

                val events = midiEvents.filter {
                    it.timeNano <= scheduleUntil && !it.isSent
                }

                events.forEach { event ->
                    Log.d("DEV", "Sending midi event: ${event}")
                    val timestamp = startTimeNano + event.timeNano
                    midiReceiver.send(event.getMidiData(), 0, event.getMidiData().size, timestamp)
                    event.isSent = true
                }

                delay(LOOP_INTERVAL_MILLIS)
            }
        }
    }

    fun stop() {
        isPlaying = false
        playJob?.cancel()
    }
}