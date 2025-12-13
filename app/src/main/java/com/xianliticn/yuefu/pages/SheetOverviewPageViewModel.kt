package com.xianliticn.yuefu.pages

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xianliticn.yuefu.AppDatabase
import com.xianliticn.yuefu.R
import com.xianliticn.yuefu.utils.getSheetTitle
import com.xianliticn.yuefu.utils.readXml
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SheetOverviewPageViewModel @Inject constructor(
    private val appDatabase: AppDatabase
) : ViewModel() {

    @Inject
    @ApplicationContext
    lateinit var context: Context

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    fun refresh(sheetId: Int) {
        _uiState.update {
            _uiState.value.copy(
                loading = true
            )
        }
        viewModelScope.launch {
            val sheet = appDatabase.sheetDao().getById(sheetId)
            if (sheet == null) {
                _uiState.update {
                    _uiState.value.copy(
                        errorMessage = context.getString(R.string.sheet_not_found),
                        loading = false
                    )
                }
                return@launch
            }

            val sheetDoc = readXml(context.filesDir.resolve("import/${sheet.fileName}"))

            val sheetName = getSheetTitle(sheetDoc)
            _uiState.update {
                _uiState.value.copy(
                    sheetName = sheetName ?: context.getString(R.string.unknown),
                    loading = false
                )
            }
        }
    }

    data class UiState(
        val loading: Boolean = false,
        val errorMessage: String? = null,
        val sheetName: String = "",
    )
}