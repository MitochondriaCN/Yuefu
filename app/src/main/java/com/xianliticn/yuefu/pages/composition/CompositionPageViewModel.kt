package com.xianliticn.yuefu.pages.composition

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xianliticn.yuefu.webapi.lyria.ClientContent
import com.xianliticn.yuefu.webapi.lyria.PlaybackControl
import com.xianliticn.yuefu.webapi.lyria.PlaybackMessage
import com.xianliticn.yuefu.webapi.lyria.PromptMessage
import com.xianliticn.yuefu.webapi.lyria.WeightedPrompt
import dagger.hilt.android.lifecycle.HiltViewModel
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

@HiltViewModel
class CompositionPageViewModel @Inject constructor(
    private val client: OkHttpClient
) : ViewModel() {

    private var ws: WebSocket? = null
    private val isWsReady = MutableStateFlow(false)

    private var audioTrack: AudioTrack? = null

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

        viewModelScope.launch {
            if (ws == null) {
                connectWebSocket()
            }

            // 非阻塞等待 websocket 准备好 (通过 StateFlow 的挂起函数 first)
            isWsReady.first { it }

            ws?.let {
                it.sendPrompt(prompt)
                _uiState.value =
                    _uiState.value.copy(messages = _uiState.value.messages + PromptMessage(prompt))
                it.sendPlayback(PlaybackControl.PLAY)
            }
        }
    }

    private fun connectWebSocket() {
        val request = Request.Builder()
            .url("wss://yf.qingshuige.ink/api/ws/lyria")
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
                val byteArray = bytes.toByteArray()

                // 检查是否是 setupComplete 信号
                if (byteArray.decodeToString().contains("setupComplete")) {
                    isWsReady.value = true
                    return
                }

                // 处理二进制数据
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + BinaryResponseMessage(bytes)
                )
                audioTrack?.write(byteArray, 0, byteArray.size)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("YF", "onFailure: ${t.message}", t)
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
        _uiState.value = _uiState.value.copy(messages = emptyList())
    }

    fun handleKeyChange(key: String) {
        _uiState.value = _uiState.value.copy(selectedKey = key)
    }

    fun handleInstrumentChange(instrument: String) {
        _uiState.value = _uiState.value.copy(selectedInstrument = instrument)
    }

    fun WebSocket.sendPrompt(prompt: String) {
        send(
            Json.encodeToString(
                PromptMessage(
                    ClientContent(
                        listOf(WeightedPrompt(prompt, 1.0))
                    )
                )
            )
        )
        Log.d("YF", "Prompt sent: $prompt")
    }

    fun WebSocket.sendPlayback(playbackControl: PlaybackControl) {
        send(
            Json.encodeToString(
                PlaybackMessage(playbackControl)
            )
        )
        Log.d("YF", "Playback sent: $playbackControl")
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
}

data class CompositionPageState(
    val instruments: List<String> = emptyList(),
    val keys: List<String> = emptyList(),
    val selectedKey: String? = keys.firstOrNull(),
    val selectedInstrument: String? = instruments.firstOrNull(),
    val prompt: String? = null,
    val messages: List<LyriaMessage> = emptyList()
)

private data class AudioTrackConfig(
    val sampleRate: Int = 44100,
    val channelConfig: Int = AudioFormat.CHANNEL_OUT_STEREO,
    val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
    val bufferSize: Int = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
)

private val keys = listOf(
    "C", "C#/Db", "D", "D#/Eb", "E", "F", "F#/Gb", "G", "G#/Ab", "A", "A#/Bb", "B"
)

private val instruments = listOf(
    "钢琴", "小提琴", "长笛", "小号"
)
