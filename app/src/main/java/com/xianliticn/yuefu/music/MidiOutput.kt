package com.xianliticn.yuefu.music

import android.content.Context
import android.util.Log
import org.billthefarmer.mididriver.MidiDriver

interface MidiOutput {
    fun init()
    fun start()
    fun stop()
    fun setVolume(volume: Int)
    fun setPreset(channel: Int, preset: Int)
    fun noteOn(channel: Int, pitch: Int, velocity: Int)
    fun noteOff(channel: Int, pitch: Int)
    fun allNotesOff()
    fun allSoundOff()
    fun release()
}

class MidiDriverOutput : MidiOutput {
    private val midiDriver = MidiDriver.getInstance()

    init {
        midiDriver.setOnMidiStartListener {
            Log.d("MidiDriverOutput", "MidiDriver started")
        }
        midiDriver.start()
    }

    override fun init() {
        // GM System On
        midiDriver.queueEvent(
            byteArrayOf(
                0xF0.toByte(), 0x7E.toByte(), 0x7F.toByte(),
                0x09.toByte(), 0x01.toByte(), 0xF7.toByte()
            )
        )
    }

    override fun start() = Unit

    override fun stop() = Unit

    override fun setVolume(volume: Int) {
        midiDriver.setVolume(volume)
    }

    override fun setPreset(channel: Int, preset: Int) {
        val status = (0xC0 or (channel and 0x0F)).toByte()
        midiDriver.queueEvent(byteArrayOf(status, (preset and 0x7F).toByte()))
    }

    override fun noteOn(channel: Int, pitch: Int, velocity: Int) {
        val status = (0x90 or (channel and 0x0F)).toByte()
        midiDriver.queueEvent(byteArrayOf(status, (pitch and 0x7F).toByte(), (velocity and 0x7F).toByte()))
    }

    override fun noteOff(channel: Int, pitch: Int) {
        val status = (0x80 or (channel and 0x0F)).toByte()
        midiDriver.queueEvent(byteArrayOf(status, (pitch and 0x7F).toByte(), 0x00))
    }

    override fun allNotesOff() {
        midiDriver.queueEvent(byteArrayOf(0xB0.toByte(), 0x7B.toByte(), 0x00.toByte()))
    }

    override fun allSoundOff() {
        midiDriver.queueEvent(byteArrayOf(0xB0.toByte(), 0x78.toByte(), 0x00.toByte()))
    }

    override fun release() = Unit
}

class SoundFontOutput(
    private val context: Context,
    private val assetPath: String
) : MidiOutput {
    private val synth = SoundFontSynth(context, assetPath)
    private var ready = false

    override fun init() {
        ready = try {
            synth.loadSoundFont()
        } catch (e: Exception) {
            Log.w("SoundFontOutput", "SoundFont load failed: ${e.message}")
            false
        }
        if (ready) {
            for (channel in 0 until 16) {
                synth.setPreset(channel, 0)
            }
            synth.setVolume(1.0f)
        }
    }

    override fun start() {
        if (ready) {
            synth.start()
        }
    }

    override fun stop() {
        if (ready) {
            synth.stop()
        }
    }

    override fun setVolume(volume: Int) {
        if (ready) {
            val normalized = (volume.coerceIn(0, 127)) / 127.0f
            synth.setVolume(normalized)
        }
    }

    override fun setPreset(channel: Int, preset: Int) {
        if (ready) {
            synth.setPreset(channel, preset)
        }
    }

    override fun noteOn(channel: Int, pitch: Int, velocity: Int) {
        if (ready) {
            synth.noteOn(channel, pitch, velocity)
        }
    }

    override fun noteOff(channel: Int, pitch: Int) {
        if (ready) {
            synth.noteOff(channel, pitch)
        }
    }

    override fun allNotesOff() {
        if (ready) {
            synth.allNotesOff()
        }
    }

    override fun allSoundOff() {
        if (ready) {
            synth.allSoundOff()
        }
    }

    override fun release() {
        if (ready) {
            synth.release()
        }
    }

    fun isReady(): Boolean = ready
}
