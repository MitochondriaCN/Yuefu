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
                it.sendPrompt(prompt)
                it.sendPlayback(PlaybackControl.PLAY)
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
                Log.e("YF", "onFailure: ${t.message}", t)
                Toast.makeText(
                    context,
                    R.string.connection_lost,
                    Toast.LENGTH_LONG
                ).show()
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
            json.encodeToString(
                PromptMessage(
                    ClientContent(
                        listOf(WeightedPrompt(prompt, 1.0))
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
