package com.xianliticn.yuefu.pages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xianliticn.yuefu.AppDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SheetOverviewPageViewModel @Inject constructor(
    private val appDatabase: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    fun refresh(sheetId: Int) {
        viewModelScope.launch {
            val sheet = appDatabase.sheetDao().getById(sheetId)
            _uiState.value = UiState(
                sheetName = sheet?.sheetName ?: ""
            )
        }
    }

    data class UiState(
        val loading: Boolean = false,
        val sheetName: String = "",
    )
}