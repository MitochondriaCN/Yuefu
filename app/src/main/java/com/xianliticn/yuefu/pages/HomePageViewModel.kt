package com.xianliticn.yuefu.pages

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@HiltViewModel
class HomePageViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    fun handleImportFile(uri: Uri) {
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

            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {

        }
    }
}