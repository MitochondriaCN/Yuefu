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
import com.xianliticn.yuefu.utils.getAbsoluteImportFilePath
import com.xianliticn.yuefu.utils.getAbsoluteImportPath
import com.xianliticn.yuefu.utils.getHash
import com.xianliticn.yuefu.utils.getTitle
import com.xianliticn.yuefu.utils.isValidMusicXml
import com.xianliticn.yuefu.utils.readXml
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipInputStream

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
            val recent4 = appDatabase.sheetDao().getAllOpenTimeDesc().take(4)
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

    /**
     * 导入MusicXML文件。
     * @param uri MusicXML文件的Uri，需要用[android.content.ContentResolver]解析。
     */
    fun handleImportFile(uri: Uri) {
        _uiState.update {
            _uiState.value.copy(
                loading = true,
                loadingMessage = context.getString(R.string.importing_file)
            )
        }

        //创建临时导入文件
        val file = File(context.cacheDir, "${System.currentTimeMillis()}-import-tmp")

        //复制文件
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        viewModelScope.launch {
            try {
                importMusicXmlFile(file)

                Toast.makeText(context, R.string.import_success, Toast.LENGTH_LONG).show()
                refresh()
            } catch (e: Exception) {
                Log.e("DEV", "handleImportFile: ${e.message}", e)
                Toast.makeText(context, R.string.import_failed, Toast.LENGTH_LONG).show()
            } finally {
                file.delete()
                _uiState.update {
                    _uiState.value.copy(
                        loading = false
                    )
                }
            }
        }

    }

    /**
     * 将外部文件导入为MusicXML。
     * @param file 外部文件。
     * @return 导入后的文件。
     */
    private suspend fun importMusicXmlFile(file: File): File {
        //创建导入子目录
        val importDir = File(context.getAbsoluteImportPath())
        if (!importDir.exists()) {
            importDir.mkdirs()
        }

        //创建导入文件
        val inFile = File(
            context.getAbsoluteImportFilePath(
                "${
                    DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                        .format(LocalDateTime.now())
                }-import.xml"
            )
        )

        //判断是否被压缩
        //被压缩的MusicXML本质上是zip文件，因此
        //前两字节必然为50 4B
        //据此判断
        if (file.readBytes().sliceArray(0..1).contentEquals(byteArrayOf(0x50, 0x4B))) {
            //解压
            ZipInputStream(file.inputStream()).use { zis ->
                var entry = zis.nextEntry
                var foundMusicXml = false
                while (entry != null) {
                    //不要META-INF/container.xml
                    if (!entry.isDirectory && (entry.name.endsWith(".xml")
                                || entry.name.endsWith(".musicxml"))
                        && !entry.name.startsWith("META-INF")
                    ) {
                        //找到了，将其内容写入目标文件
                        FileOutputStream(inFile).use { fos ->
                            zis.copyTo(fos)
                        }
                        foundMusicXml = true
                        break // 假设一个.mxl中只有一个主要的musicxml文件
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
                if (!foundMusicXml) {
                    throw Exception("在压缩包中未找到有效的MusicXML文件。")
                }
            }
        } else { //不是压缩过的
            //直接写入
            inFile.writeText(file.readText())
        }

        //打印前几行看看内容
        inFile.readLines().forEachIndexed { index, s ->
            if (index < 10) {
                Log.d("DEV", "importMusicXmlFile: $s")
            }
        }

        //检查合法性
        if (!isValidMusicXml(inFile)) {
            //不合法
            inFile.delete()
            throw Exception(context.getString(R.string.invalid_sheet))
        }
        //合法
        //查重
        appDatabase.sheetDao().getSameHash(inFile.getHash()).takeIf { it.isNotEmpty() }
            ?.let {
                inFile.delete()
                throw Exception(context.getString(R.string.import_duplicate))
            }
        //插入数据条目
        appDatabase.sheetDao().insert(
            Sheet(
                fileName = inFile.name,
                sheetName = readXml(inFile).getTitle() ?: "Unknown",
                lastOpenTime = System.currentTimeMillis(),
                hash = inFile.getHash()
            )
        )

        return inFile
    }

    data class HomePageState(
        val loading: Boolean = false,
        val loadingMessage: String = "",
        val recent4: List<Sheet> = emptyList()
    )
}