package com.xianliticn.yuefu.pages

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xianliticn.yuefu.AppDatabase
import com.xianliticn.yuefu.R
import com.xianliticn.yuefu.entities.Sheet
import com.xianliticn.yuefu.utils.getHash
import com.xianliticn.yuefu.utils.isValidMusicXml
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@HiltViewModel
class HomePageViewModel @Inject constructor(
    private val appDatabase: AppDatabase
) : ViewModel() {
    @Inject
    @ApplicationContext
    lateinit var context: Context

    private val _uiState = MutableStateFlow(HomePageState())
    val uiState: StateFlow<HomePageState> = _uiState

    fun handleImportFile(uri: Uri) {
        _uiState.value = _uiState.value.copy(
            loading = true
        )

        try {
            //创建导入子目录
            val importDir = File(context.filesDir, "import")
            if (!importDir.exists()) {
                importDir.mkdirs()
            }

            //创建导入文件
            val file = File(
                importDir,
                "${
                    DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now())
                }-import.xml"
            )

            //复制文件
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            //检查合法性
            viewModelScope.launch {
                if (!isValidMusicXml(file)) {
                    //不合法
                    Toast.makeText(context, R.string.invalid_sheet, Toast.LENGTH_LONG).show()
                    file.delete()
                    _uiState.value = _uiState.value.copy(
                        loading = false
                    )
                } else {
                    //合法
                    //查重
                    appDatabase.sheetDao().getSameHash(file.getHash())?.let {
                        Toast.makeText(context, R.string.import_duplicate, Toast.LENGTH_LONG).show()
                        file.delete()
                        _uiState.value = _uiState.value.copy(
                            loading = false
                        )
                        return@launch
                    }
                    Toast.makeText(context, R.string.import_success, Toast.LENGTH_LONG).show()
                    //插入数据条目
                    appDatabase.sheetDao().insert(
                        Sheet(
                            fileName = file.name,
                            sheetName = file.nameWithoutExtension,
                            lastOpenTime = System.currentTimeMillis(),
                            hash = file.getHash()
                        )
                    )
                    _uiState.value = _uiState.value.copy(
                        loading = false
                    )
                }
            }
        } catch (e: Exception) {
            Log.d("DEV", "Failed to import: " + e.message.toString())
            Toast.makeText(context, R.string.import_failed, Toast.LENGTH_LONG).show()
            _uiState.value = _uiState.value.copy(
                loading = false
            )
        }
    }

    data class HomePageState(
        val loading: Boolean = false
    )
}