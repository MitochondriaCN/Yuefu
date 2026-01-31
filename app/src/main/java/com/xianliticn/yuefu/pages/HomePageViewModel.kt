package com.xianliticn.yuefu.pages

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xianliticn.yuefu.AppDatabase
import com.xianliticn.yuefu.R
import com.xianliticn.yuefu.SheetActivity
import com.xianliticn.yuefu.entities.Sheet
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomePageViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
) : ViewModel() {
    @Inject
    @ApplicationContext
    lateinit var context: Context

    private val _uiState = MutableStateFlow(HomePageState())
    val uiState: StateFlow<HomePageState> = _uiState

    fun refresh() {
        _uiState.update {
            _uiState.value.copy(
                loading = true,
                loadingMessage = context.getString(R.string.loading)
            )
        }

        viewModelScope.launch {
            //获取最近用过的四个文件
            val recent4 = appDatabase.sheetDao().getAllDownloaded().take(4)
            _uiState.update {
                it.copy(
                    recent4 = recent4
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

    fun handleRecentSheetClick(sheet: Sheet) {
        val intent = Intent(context, SheetActivity::class.java)
        intent.putExtra("sheetId", sheet.id)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    data class HomePageState(
        val loading: Boolean = false,
        val loadingMessage: String = "",
        val recent4: List<Sheet> = emptyList()
    )
}