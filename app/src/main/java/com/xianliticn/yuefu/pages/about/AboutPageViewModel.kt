package com.xianliticn.yuefu.pages.about

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xianliticn.yuefu.webapi.SystemInfoApi
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@HiltViewModel
class AboutPageViewModel @Inject constructor(
    private val systemInfoApi: SystemInfoApi
) : ViewModel() {

    @Inject
    @ApplicationContext
    lateinit var context: Context

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    fun refresh() {
        viewModelScope.launch {
            val systemInfo = runCatching { systemInfoApi.getSystemInfo().data }.getOrNull()
            _uiState.value = UiState(
                backendOnline = systemInfo != null,
                backendTimestamp = systemInfo?.time?.let {
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(it)
                },
                backendTmpSize = "${systemInfo?.tmpSize ?: "-"} B"
            )
        }
    }

    data class UiState(
        val backendOnline: Boolean = false,
        val backendTimestamp: String? = null,
        val backendTmpSize: String? = null
    )
}