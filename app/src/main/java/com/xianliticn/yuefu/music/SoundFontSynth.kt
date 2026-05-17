package com.xianliticn.yuefu.music

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

class SoundFontSynth(
    private val context: Context,
    private val assetPath: String,
    private val sampleRate: Int = 44100
) {
    private val handle: Long = nativeCreate(sampleRate)
    private val isRunning = AtomicBoolean(false)
    private var renderThread: Thread? = null

    private val bufferFrames = 512
    private val buffer = ShortArray(bufferFrames * 2)

    private val audioTrack: AudioTrack by lazy {
        val minBufferBytes = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val frameSizeBytes = 2 * 2
        val targetBufferBytes = bufferFrames * frameSizeBytes * 4
        val bufferSizeBytes = maxOf(minBufferBytes, targetBufferBytes)

        AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSizeBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    fun loadSoundFont(): Boolean {
        val data = readAssetBytes(assetPath)
        return nativeLoadSoundFont(handle, data)
    }

    fun start() {
        if (isRunning.getAndSet(true)) return

        audioTrack.play()
        renderThread = Thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            while (isRunning.get()) {
                nativeRender(handle, buffer, bufferFrames)
                audioTrack.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
            }
        }.also { it.start() }
    }

    fun stop() {
        if (!isRunning.getAndSet(false)) return

        renderThread?.join(500)
        renderThread = null
        audioTrack.pause()
        audioTrack.flush()
    }

    fun release() {
        stop()
        audioTrack.release()
        nativeDestroy(handle)
    }

    fun setPreset(channel: Int, preset: Int) {
        nativeSetPreset(handle, channel, preset)
    }

    fun setVolume(volume: Float) {
        nativeSetVolume(handle, volume)
    }

    fun noteOn(channel: Int, key: Int, velocity: Int) {
        val normalizedVelocity = (velocity.coerceIn(0, 127)) / 127.0f
        nativeNoteOn(handle, channel, key, normalizedVelocity)
    }

    fun noteOff(channel: Int, key: Int) {
        nativeNoteOff(handle, channel, key)
    }

    fun allNotesOff() {
        nativeAllNotesOff(handle)
    }

    fun allSoundOff() {
        nativeAllSoundOff(handle)
    }

    private fun readAssetBytes(path: String): ByteArray {
        context.assets.open(path).use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                output.write(buffer, 0, read)
            }
            return output.toByteArray()
        }
    }

    private external fun nativeCreate(sampleRate: Int): Long
    private external fun nativeDestroy(handle: Long)
    private external fun nativeLoadSoundFont(handle: Long, data: ByteArray): Boolean
    private external fun nativeSetVolume(handle: Long, volume: Float)
    private external fun nativeSetPreset(handle: Long, channel: Int, preset: Int)
    private external fun nativeNoteOn(handle: Long, channel: Int, key: Int, velocity: Float)
    private external fun nativeNoteOff(handle: Long, channel: Int, key: Int)
    private external fun nativeAllNotesOff(handle: Long)
    private external fun nativeAllSoundOff(handle: Long)
    private external fun nativeRender(handle: Long, buffer: ShortArray, frames: Int): Int

    companion object {
        init {
            System.loadLibrary("yuefu_synth")
        }
    }
}
