package com.xianliticn.yuefu.pages.composition

import android.util.Log
import androidx.lifecycle.ViewModel
import com.xianliticn.yuefu.webapi.lyria.ClientContent
import com.xianliticn.yuefu.webapi.lyria.PromptMessage
import com.xianliticn.yuefu.webapi.lyria.WeightedPrompt
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

@HiltViewModel
class CompositionPageViewModel @Inject constructor(
    private val client: OkHttpClient
) : ViewModel() {

    private var ws: WebSocket? = null

    fun handlePromptChange(prompt: String) {
        _uiState.value = _uiState.value.copy(prompt = prompt)
    }

    fun handleSendClick() {
        val prompt = _uiState.value.prompt ?: return

        val request = Request.Builder()
            .url("wss://yf.qingshuige.ink/api/ws/lyria")
            .build()

        when (val currentWs = ws) {
            null -> ws = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d("YF", "onOpen: $response")
                    webSocket.sendPrompt(prompt)
                    _uiState.value = _uiState.value.copy(playbackMode = true)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    Log.d("YF", "onMessage: $text")
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    _uiState.value = _uiState.value.copy(playbackMode = false)
                }
            })

            else -> currentWs.sendPrompt(prompt)
        }
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
    }

    private val _uiState = MutableStateFlow(
        CompositionPageState(
            instruments = instruments,
            keys = keys
        )
    )
    val uiState: StateFlow<CompositionPageState> = _uiState
}

data class CompositionPageState(
    val instruments: List<String> = emptyList(),
    val keys: List<String> = emptyList(),
    val selectedKey: String? = keys.firstOrNull(),
    val selectedInstrument: String? = instruments.firstOrNull(),
    val prompt: String? = null,
    val playbackMode: Boolean = false
)

private val keys = listOf(
    "C", "C#/Db", "D", "D#/Eb", "E", "F", "F#/Gb", "G", "G#/Ab", "A", "A#/Bb", "B"
)

private val instruments = listOf(
    "钢琴", "小提琴", "长笛", "小号"
)