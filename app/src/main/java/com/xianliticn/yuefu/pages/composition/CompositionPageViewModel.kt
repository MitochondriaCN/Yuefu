package com.xianliticn.yuefu.pages.composition

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xianliticn.yuefu.R
import com.xianliticn.yuefu.modules.NetworkModule
import com.xianliticn.yuefu.webapi.lyria.ClientContent
import com.xianliticn.yuefu.webapi.lyria.ConfigMessage
import com.xianliticn.yuefu.webapi.lyria.LyriaResponse
import com.xianliticn.yuefu.webapi.lyria.MusicGenerationConfig
import com.xianliticn.yuefu.webapi.lyria.PlaybackControl
import com.xianliticn.yuefu.webapi.lyria.PlaybackMessage
import com.xianliticn.yuefu.webapi.lyria.PromptMessage
import com.xianliticn.yuefu.webapi.lyria.Scale
import com.xianliticn.yuefu.webapi.lyria.WeightedPrompt
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

//TODO: 增加异常处理防崩溃
@HiltViewModel
class CompositionPageViewModel @Inject constructor(
    private val client: OkHttpClient
) : ViewModel() {

    @Inject
    @ApplicationContext
    lateinit var context: Context

    private var ws: WebSocket? = null
    private val isWsReady = MutableStateFlow(false)

    private var audioTrack: AudioTrack? = null

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private var currentWeight = 1.0
    private var isPlaying = false

    private val fftSize = 1024
    private val bandCount = 16
    private val fftBuffer = FloatArray(fftSize)
    private val fftReal = FloatArray(fftSize)
    private val fftImag = FloatArray(fftSize)
    private val window = FloatArray(fftSize) { index ->
        (0.5f - 0.5f * cos(2.0 * PI * index / (fftSize - 1))).toFloat()
    }
    private var fftWriteIndex = 0
    private var fftFilled = false
    private var spectrum = FloatArray(bandCount)

    private val _uiState = MutableStateFlow(
        CompositionPageState(
            instruments = instruments,
            keys = keys
        )
    )
    val uiState: StateFlow<CompositionPageState> = _uiState

    fun handlePromptChange(prompt: String) {
        _uiState.value = _uiState.value.copy(prompt = prompt)
    }

    fun handleSendClick() {
        val prompt = _uiState.value.prompt ?: return
        _uiState.value = _uiState.value.copy(
            prompt = null,
            messages = _uiState.value.messages + PromptMessage(prompt)
        )

        viewModelScope.launch {
            if (ws == null) {
                connectWebSocket()
            }

            // 等待 websocket 准备好
            isWsReady.first { it }

            ws?.let {
                it.sendPrompt(prompt, currentWeight)
                currentWeight /= 2
                if (!isPlaying) {
                    it.sendPlayback(PlaybackControl.PLAY)
                    isPlaying = true
                }
            }
        }
    }

    private fun connectWebSocket() {
        val request = Request.Builder()
            .url(NetworkModule.WS_URL)
            .build()

        isWsReady.value = false
        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("YF", "onOpen: $response")
                initAudioTrack()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("YF", "onMessage (text): $text")
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d("YF", "onMessage (bytes): ${bytes.size} bytes")
                val messageText = bytes.utf8()

                // 检查风控
                if (messageText.contains("We couldn't create what you asked for")) {
                    viewModelScope.launch {
                        Toast.makeText(
                            context,
                            R.string.comp_risk_control,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    handleStopClick()
                    return
                }

                // 检查是否是 setupComplete 信号
                if (messageText.contains("setupComplete")) {
                    // 设置配置
                    webSocket.sendConfig(
                        MusicGenerationConfig(
                            bpm = 120,
                            scale = Scale.C_MAJOR_A_MINOR,
                        )
                    )
                    // 重置上下文
                    webSocket.sendPlayback(PlaybackControl.RESET_CONTEXT)

                    isWsReady.value = true
                    return
                }

                try {
                    val response = json.decodeFromString<LyriaResponse>(messageText)
                    response.serverContent?.audioChunks?.forEach { chunk ->
                        Log.d("YF", "LyriaResponse format: ${chunk.mimeType}")
                        val audioData = Base64.decode(chunk.data, Base64.DEFAULT)

                        updateAudioSpectrum(audioData)

                        // 处理二进制数据（仅用于 UI 显示）
                        _uiState.value = _uiState.value.copy(
                            messages = _uiState.value.messages + BinaryResponseMessage(bytes)
                        )

                        audioTrack?.write(audioData, 0, audioData.size)
                    }
                } catch (e: Exception) {
                    Log.e("YF", "Failed to parse LyriaResponse", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w("YF", "onFailure: ${t.message}", t)
                viewModelScope.launch {
                    Toast.makeText(
                        context,
                        R.string.connection_lost,
                        Toast.LENGTH_LONG
                    ).show()
                }
                resetWsState()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("YF", "onClosed: $code $reason")
                resetWsState()
            }
        })
    }

    private fun resetWsState() {
        ws = null
        isWsReady.value = false
        isPlaying = false
        currentWeight = 1.0
        fftWriteIndex = 0
        fftFilled = false
        spectrum = FloatArray(bandCount)
        _uiState.value = _uiState.value.copy(
            messages = emptyList(),
            audioLevel = 0f,
            spectrum = List(bandCount) { 0f }
        )
    }

    fun handleKeyChange(key: String) {
        _uiState.value = _uiState.value.copy(selectedKey = key)
    }

    fun handleInstrumentChange(instrument: String) {
        _uiState.value = _uiState.value.copy(selectedInstrument = instrument)
    }

    fun WebSocket.sendPrompt(prompt: String, weight: Double = 1.0) {
        send(
            json.encodeToString(
                PromptMessage(
                    ClientContent(
                        listOf(WeightedPrompt(prompt, weight))
                    )
                )
            ).also {
                Log.d("YF", "Prompt sent: $it")
            }
        )
    }

    fun WebSocket.sendPlayback(playbackControl: PlaybackControl) {
        send(
            json.encodeToString(
                PlaybackMessage(playbackControl)
            ).also {
                Log.d("YF", "Playback sent: $it")
            }
        )
    }

    fun WebSocket.sendConfig(config: MusicGenerationConfig) {
        send(
            json.encodeToString(
                ConfigMessage(config)
            ).also {
                Log.d("YF", "Config sent: $it")
            }
        )
    }

    private fun initAudioTrack() {
        if (audioTrack != null) return

        val config = AudioTrackConfig()

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(config.audioFormat)
                    .setSampleRate(config.sampleRate)
                    .setChannelMask(config.channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(config.bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
    }

    fun handleStopClick() {
        ws?.sendPlayback(PlaybackControl.STOP)

        ws?.close(1000, "User requested stop")
        ws = null

        audioTrack?.let {
            try {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            } catch (e: Exception) {
                Log.e("YF", "Error releasing AudioTrack", e)
            }
        }
        audioTrack = null

        resetWsState()
    }

    private fun updateAudioSpectrum(audioData: ByteArray) {
        val samples = extractMonoSamples(audioData)
        if (samples.isEmpty()) return

        pushSamples(samples)
        val bands = computeSpectrumBands() ?: return

        val attack = 0.55f
        val decay = 0.12f
        for (index in 0 until bandCount) {
            val target = bands[index]
            val current = spectrum[index]
            spectrum[index] = if (target > current) {
                current + (target - current) * attack
            } else {
                current + (target - current) * decay
            }
        }

        val bass = (spectrum[0] + spectrum[1] + spectrum[2]) / 3f
        val level = (0.2f + bass * 0.8f).coerceIn(0f, 1f)

        _uiState.value = _uiState.value.copy(
            audioLevel = level,
            spectrum = spectrum.toList()
        )
    }

    private fun extractMonoSamples(audioData: ByteArray): FloatArray {
        if (audioData.size < 2) return FloatArray(0)

        val isStereo = audioData.size % 4 == 0
        val sampleCount = if (isStereo) audioData.size / 4 else audioData.size / 2
        val samples = FloatArray(sampleCount)

        var index = 0
        var sampleIndex = 0
        while (index + 1 < audioData.size && sampleIndex < sampleCount) {
            val low = audioData[index].toInt() and 0xFF
            val high = audioData[index + 1].toInt()
            val left = ((high shl 8) or low).toShort().toInt()

            if (isStereo && index + 3 < audioData.size) {
                val lowR = audioData[index + 2].toInt() and 0xFF
                val highR = audioData[index + 3].toInt()
                val right = ((highR shl 8) or lowR).toShort().toInt()
                samples[sampleIndex] = ((left + right) * 0.5f / 32768f).coerceIn(-1f, 1f)
                index += 4
            } else {
                samples[sampleIndex] = (left / 32768f).coerceIn(-1f, 1f)
                index += 2
            }
            sampleIndex += 1
        }

        return samples
    }

    private fun pushSamples(samples: FloatArray) {
        for (sample in samples) {
            fftBuffer[fftWriteIndex] = sample
            fftWriteIndex = (fftWriteIndex + 1) % fftSize
            if (fftWriteIndex == 0) {
                fftFilled = true
            }
        }
    }

    private fun computeSpectrumBands(): FloatArray? {
        if (!fftFilled) return null

        for (index in 0 until fftSize) {
            val bufferIndex = (fftWriteIndex + index) % fftSize
            fftReal[index] = fftBuffer[bufferIndex] * window[index]
            fftImag[index] = 0f
        }

        fft(fftReal, fftImag)

        val magnitudes = FloatArray(fftSize / 2)
        var maxMag = 1e-6f
        for (index in 1 until fftSize / 2) {
            val mag = sqrt(fftReal[index] * fftReal[index] + fftImag[index] * fftImag[index])
            magnitudes[index] = mag
            if (mag > maxMag) maxMag = mag
        }

        val bands = FloatArray(bandCount)
        val maxBin = fftSize / 2 - 1
        val bandWidth = max(1, maxBin / bandCount)
        val logBase = 1f + 12f * maxMag

        for (band in 0 until bandCount) {
            val startBin = max(1, band * bandWidth)
            val endBin = min(maxBin, (band + 1) * bandWidth)

            var peak = 0f
            var bin = startBin
            while (bin <= endBin) {
                val value = magnitudes[bin]
                if (value > peak) peak = value
                bin += 1
            }

            val normalized = (kotlin.math.log10(1f + 12f * peak) / kotlin.math.log10(logBase))
                .coerceIn(0f, 1f)
            bands[band] = normalized
        }

        return bands
    }

    private fun fft(real: FloatArray, imag: FloatArray) {
        val n = real.size
        var j = 0
        var i = 1
        while (i < n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tempReal = real[i]
                val tempImag = imag[i]
                real[i] = real[j]
                imag[i] = imag[j]
                real[j] = tempReal
                imag[j] = tempImag
            }
            i += 1
        }

        var len = 2
        while (len <= n) {
            val ang = (-2.0 * PI / len)
            val wLenReal = cos(ang).toFloat()
            val wLenImag = sin(ang).toFloat()
            var start = 0
            while (start < n) {
                var wReal = 1f
                var wImag = 0f
                var k = 0
                while (k < len / 2) {
                    val uReal = real[start + k]
                    val uImag = imag[start + k]
                    val vReal = real[start + k + len / 2] * wReal - imag[start + k + len / 2] * wImag
                    val vImag = real[start + k + len / 2] * wImag + imag[start + k + len / 2] * wReal

                    real[start + k] = uReal + vReal
                    imag[start + k] = uImag + vImag
                    real[start + k + len / 2] = uReal - vReal
                    imag[start + k + len / 2] = uImag - vImag

                    val nextWReal = wReal * wLenReal - wImag * wLenImag
                    val nextWImag = wReal * wLenImag + wImag * wLenReal
                    wReal = nextWReal
                    wImag = nextWImag
                    k += 1
                }
                start += len
            }
            len = len shl 1
        }
    }
}

data class CompositionPageState(
    val instruments: List<String> = emptyList(),
    val keys: List<String> = emptyList(),
    val selectedKey: String? = keys.firstOrNull(),
    val selectedInstrument: String? = instruments.firstOrNull(),
    val prompt: String? = null,
    val messages: List<LyriaMessage> = emptyList(),
    val audioLevel: Float = 0f,
    val spectrum: List<Float> = List(16) { 0f }
)

private data class AudioTrackConfig(
    val sampleRate: Int = 48000,
    val channelConfig: Int = AudioFormat.CHANNEL_OUT_STEREO,
    val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
    val bufferSize: Int = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 4
)

private val keys = listOf(
    "C", "C#/Db", "D", "D#/Eb", "E", "F", "F#/Gb", "G", "G#/Ab", "A", "A#/Bb", "B"
)

private val instruments = listOf(
    "钢琴", "小提琴", "长笛", "小号"
)
