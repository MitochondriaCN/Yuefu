package com.xianliticn.yuefu.pages.sheet

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xianliticn.yuefu.AppDatabase
import com.xianliticn.yuefu.R
import com.xianliticn.yuefu.SheetActivity
import com.xianliticn.yuefu.entities.Sheet
import com.xianliticn.yuefu.utils.getAbsoluteImportFilePath
import com.xianliticn.yuefu.utils.getAbsoluteImportPath
import com.xianliticn.yuefu.utils.getHash
import com.xianliticn.yuefu.utils.getTitle
import com.xianliticn.yuefu.utils.isValidMusicXml
import com.xianliticn.yuefu.utils.readXml
import com.xianliticn.yuefu.vo.TaskStatus
import com.xianliticn.yuefu.webapi.omr.OmrApi
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
class SheetPageViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    private val omrApi: OmrApi
) : ViewModel() {
    @Inject
    @ApplicationContext
    lateinit var context: Context

    private var searchJob: Job? = null
    private var deleteJob: Job? = null
    private var downloadJob: Job? = null

    private val _uiState = MutableStateFlow(SheetPageState())
    val uiState: StateFlow<SheetPageState> = _uiState

    init {
        // 定时刷新
        viewModelScope.launch {
            while (true) {
                delay(5000)
                refresh()
            }
        }
    }

    fun refresh() {
        _uiState.update {
            _uiState.value.copy(
                loading = true
            )
        }
        viewModelScope.launch {
            //获取所有乐谱
            val downloaded = appDatabase.sheetDao().getAllDownloaded().map { it to null }
            val notDownloaded = appDatabase.sheetDao().getAllNotDownloaded()
            //获取所有未下载乐谱的识别状态，并map到TaskStatus
            val notDownloadedWithStatus = notDownloaded.map {
                it to try {
                    omrApi.getTaskStatus(it.taskId).data ?: TaskStatus.FAILED
                } catch (_: Exception) {
                    Toast.makeText(context, R.string.network_error, Toast.LENGTH_SHORT)
                        .show()
                    TaskStatus.FAILED
                }
            }
            //合并
            val sheets = downloaded + notDownloadedWithStatus
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

    fun handleItemClick(sheet: Pair<Sheet, TaskStatus?>) {
        if (sheet.first.isDownloaded) {
            val intent = Intent(context, SheetActivity::class.java)
            intent.putExtra("sheetId", sheet.first.id)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else if (sheet.second == TaskStatus.COMPLETED) { //未下载且已完成识别
            //防止多线程下载
            downloadJob?.let {
                Toast.makeText(context, R.string.already_downloading, Toast.LENGTH_SHORT).show()
                return
            }

            //更新UI状态
            _uiState.update {
                _uiState.value.copy(
                    downloadingSheet = sheet.first
                )
            }

            //开始下载并导入
            downloadJob = viewModelScope.launch {
                downloadSheetAndImport(sheet.first)
                refresh()
                //完成下载
                _uiState.update {
                    _uiState.value.copy(
                        downloadingSheet = null
                    )
                }
                //提示用户下载完成
                Toast.makeText(
                    context,
                    context.getString(R.string.download_complete) +
                            (appDatabase.sheetDao().getById(sheet.first.id)?.sheetName
                                ?: "Unknown"),
                    Toast.LENGTH_SHORT
                ).show()
                downloadJob = null
            }
        }
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
            val result = (appDatabase.sheetDao().getLikeFileName(keyword).map { it to null } +
                    appDatabase.sheetDao().getLikeSheetName(keyword).map { it to null }).distinct()
            _uiState.update {
                _uiState.value.copy(
                    sheets = result
                )
            }
        }
    }

    fun handleDeleteSheet(sheet: Sheet) {
        if (deleteJob?.isActive == true) return
        deleteJob = viewModelScope.launch {
            appDatabase.sheetDao().delete(sheet)
            refresh()
        }
    }

    fun handleRenameSheet(sheet: Sheet, newName: String) {
        if (newName.isEmpty()) {
            Toast.makeText(context, R.string.name_could_not_be_empty, Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            appDatabase.sheetDao().update(sheet.copy(sheetName = newName))
            refresh()
        }
    }

    /**
     * 下载乐谱并导入到数据库
     *
     * @throws Exception 下载失败
     */
    suspend fun downloadSheetAndImport(sheet: Sheet) {
        if (sheet.isDownloaded)
            throw Exception("乐谱已下载")

        val resp = omrApi.downloadSheet(sheet.taskId)
        if (resp.isSuccessful) {
            resp.body()?.let { body ->
                //存到临时文件
                val file =
                    File(context.cacheDir, "${System.currentTimeMillis()}-download.mxl")
                body.byteStream().use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                //创建导入目录
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

                file.delete()

                //打印前几行看看内容
                inFile.readLines().forEachIndexed { index, s ->
                    if (index < 10) {
                        Log.d("DEV", "importMusicXmlFile: $s")
                    }
                }

                //导入数据库
                appDatabase.sheetDao().update(
                    sheet.copy(
                        isDownloaded = true,
                        fileName = inFile.name,
                        sheetName = readXml(inFile).getTitle() ?: "Unknown",
                        lastOpenTime = System.currentTimeMillis(),
                        hash = inFile.getHash(),
                    )
                )
            }
        } else {
            throw Exception("下载乐谱失败")
        }
    }

    fun handleShareSheet(sheet: Sheet) {
        sheet.fileName?.let {
            val file = File(context.getAbsoluteImportFilePath(it))
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/xml"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, sheet.sheetName)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, context.getString(R.string.share)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } else {
                Toast.makeText(context, R.string.sheet_not_found, Toast.LENGTH_SHORT).show()
            }
        }
    }

    data class SheetPageState(
        val loading: Boolean = false,
        val downloadingSheet: Sheet? = null,
        val sheets: List<Pair<Sheet, TaskStatus?>> = emptyList()
    )
}
