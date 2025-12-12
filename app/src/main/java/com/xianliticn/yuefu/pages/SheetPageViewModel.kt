package com.xianliticn.yuefu.pages

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xianliticn.yuefu.AppDatabase
import com.xianliticn.yuefu.entities.Sheet
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SheetPageViewModel @Inject constructor(
    private val appDatabase: AppDatabase
) : ViewModel() {
    @Inject
    @ApplicationContext
    lateinit var context: Context

    private var searchJob: Job? = null

    private val _uiState = MutableStateFlow(SheetPageState())
    val uiState: StateFlow<SheetPageState> = _uiState


    fun refresh() {
        _uiState.update {
            _uiState.value.copy(
                loading = true
            )
        }
        viewModelScope.launch {
            //乐谱
            val sheets = appDatabase.sheetDao().getAllOpenTimeDesc()
            _uiState.update {
                _uiState.value.copy(
                    sheets = sheets
                )
            }

            //完成
            _uiState.update {
                _uiState.value.copy(
                    loading = false
                )
            }
        }
    }

    fun handleItemClick() {
        //TODO
    }

    fun handleSearchQueryChanged(keyword: String) {
        if (keyword.isEmpty()) {
            searchJob?.cancel()
            refresh()
        } else
            handleSearch(keyword)
    }

    fun handleSearch(keyword: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) //防抖动
            val result = appDatabase.sheetDao().getLikeFileName(keyword)
            _uiState.update {
                _uiState.value.copy(
                    sheets = result
                )
            }
        }
    }

    data class SheetPageState(
        val loading: Boolean = false,
        val sheets: List<Sheet> = emptyList()
    )
}