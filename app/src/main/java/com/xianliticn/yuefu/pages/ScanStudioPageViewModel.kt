package com.xianliticn.yuefu.pages

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.FilterTiltShift
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material.icons.filled.Tonality
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.size.Size
import com.xianliticn.yuefu.AppDatabase
import com.xianliticn.yuefu.R
import com.xianliticn.yuefu.entities.Sheet
import com.xianliticn.yuefu.utils.ColorFilterTransformation
import com.xianliticn.yuefu.utils.getAbsoluteImportFilePath
import com.xianliticn.yuefu.utils.getAbsoluteImportPath
import com.xianliticn.yuefu.utils.getHash
import com.xianliticn.yuefu.utils.getTitle
import com.xianliticn.yuefu.utils.isValidMusicXml
import com.xianliticn.yuefu.utils.readXml
import com.xianliticn.yuefu.webapi.OmrApi
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipInputStream

@HiltViewModel
class ScanStudioPageViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    private val omrApi: OmrApi
) : ViewModel() {
    @Inject
    @ApplicationContext
    lateinit var context: Context

    private val _imageParams = MutableStateFlow(
        mapOf(
            ImageParam(
                R.string.contrast,
                R.string.contrast_desc,
                Icons.Default.Contrast,
                0f..2f
            ) to 1f,
            ImageParam(
                R.string.saturation,
                R.string.saturation_desc,
                Icons.Default.Colorize,
                0f..2f
            ) to 0f,
            ImageParam(
                R.string.sharpen,
                R.string.sharpen_desc,
                Icons.Default.FilterTiltShift,
                0f..1f
            ) to 0f,
            ImageParam(
                R.string.shadows,
                R.string.shadows_desc,
                Icons.Default.Tonality,
                0f..1f
            ) to 0f,
            ImageParam(
                R.string.highlights,
                R.string.highlights_desc,
                Icons.Default.Flare,
                0f..1f
            ) to 0f,
            ImageParam(
                R.string.brightness,
                R.string.brightness_desc,
                Icons.Default.BrightnessMedium,
                -1f..1f
            ) to 0f,
        )
    )
    val imageParams: StateFlow<Map<ImageParam, Float>> = _imageParams

    private val _imageModel = MutableStateFlow<Any?>(null)
    val imageModel: StateFlow<Any?> = _imageModel

    private val _omrRunning = MutableStateFlow(false)
    val omrRunning: StateFlow<Boolean> = _omrRunning

    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished

    private lateinit var _originalBitmap: Bitmap
    private var transformationJob: Job? = null
    private var omrJob: Job? = null

    fun initialize(imageUri: Uri) {
        viewModelScope.launch {
            val source = ImageDecoder.createSource(
                context.contentResolver,
                imageUri
            )
            val bitmap = ImageDecoder.decodeBitmap(source)
            _originalBitmap = bitmap
            _imageModel.value = bitmap
        }
    }

    fun handleImageParamChange(param: ImageParam, value: Float) {
        _imageParams.value = _imageParams.value.toMutableMap().apply {
            this[param] = value
        }

        transformationJob?.cancel()
        transformationJob = viewModelScope.launch {
            //防抖
            delay(200)

            val brightnessValue =
                _imageParams.value.entries.find { it.key.nameResId == R.string.brightness }?.value
                    ?: 0f
            val contrastValue =
                _imageParams.value.entries.find { it.key.nameResId == R.string.contrast }?.value
                    ?: 1f
            val saturationValue =
                _imageParams.value.entries.find { it.key.nameResId == R.string.saturation }?.value
                    ?: 1f
            val transformation = ColorFilterTransformation(
                brightness = brightnessValue,
                contrast = contrastValue,
                saturation = saturationValue
            )
            _originalBitmap.let { original ->
                val transformedBitmap = transformation.transform(original, Size.ORIGINAL)
                _imageModel.value = transformedBitmap
            }
        }
    }

    fun handleCrop(xOffset: Float, yOffset: Float) {
        val currentBitmap = _imageModel.value as? Bitmap ?: return
        val originalWidth = currentBitmap.width
        val originalHeight = currentBitmap.height

        // 确定水平裁切的起始点和宽度
        val (offsetX, croppedWidth) = if (xOffset < originalWidth / 2) {
            // 从左侧开始裁切，保留右侧部分
            Pair(xOffset.toInt(), originalWidth - xOffset.toInt())
        } else {
            // 从右侧开始裁切，保留左侧部分
            Pair(0, xOffset.toInt())
        }

        // 确定垂直裁切的起始点和高度
        val (offsetY, croppedHeight) = if (yOffset < originalHeight / 2) {
            // 从顶部开始裁切，保留下半部分
            Pair(yOffset.toInt(), originalHeight - yOffset.toInt())
        } else {
            // 从底部开始裁切，保留上半部分
            Pair(0, yOffset.toInt())
        }

        // 确保所有裁切参数不会超出图片边界
        val finalOffsetX = offsetX.coerceIn(0, originalWidth)
        val finalOffsetY = offsetY.coerceIn(0, originalHeight)
        val finalCroppedWidth = croppedWidth.coerceIn(0, originalWidth - finalOffsetX)
        val finalCroppedHeight = croppedHeight.coerceIn(0, originalHeight - finalOffsetY)

        // 只有当裁切后的尺寸大于0时才创建新的Bitmap
        if (finalCroppedWidth > 0 && finalCroppedHeight > 0) {
            // 创建一个新的、经过裁切的 Bitmap
            val croppedBitmap = Bitmap.createBitmap(
                currentBitmap,
                finalOffsetX,
                finalOffsetY,
                finalCroppedWidth,
                finalCroppedHeight
            )
            // 更新图像
            _originalBitmap = croppedBitmap
            _imageModel.value = croppedBitmap
        }
    }

    fun handleConfirm(image: Bitmap) {
        _omrRunning.value = true

        omrJob?.cancel()
        omrJob = viewModelScope.launch {
            delay(1000)
            try {
                val part = image.toMultipartBodyPart()
                val resp = omrApi.getMusicXml(part)

                if (resp.isSuccessful) {
                    val file = File(context.cacheDir, "${System.currentTimeMillis()}-download.mxl")
                    resp.body()?.byteStream()?.use { inputStream ->
                        file.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    if (file.exists() && file.length() > 0) {
                        importMusicXmlFile(file)
                        file.delete()
                        Toast.makeText(context, R.string.import_success, Toast.LENGTH_LONG).show()
                        _omrRunning.value = false
                        _finished.value = true
                    } else {
                        Toast.makeText(context, R.string.failed_to_omr, Toast.LENGTH_LONG).show()
                        _omrRunning.value = false
                    }
                } else {
                    Toast.makeText(context, R.string.failed_to_omr, Toast.LENGTH_LONG).show()
                    _omrRunning.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, R.string.failed_to_omr, Toast.LENGTH_LONG).show()
                _omrRunning.value = false
            }
        }
    }

    /**
     * 将外部文件导入为MusicXML。
     * @param file 外部文件。
     * @return 导入后的文件。
     * @throws Exception 如果导入失败，则抛出异常。
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

    private suspend fun Bitmap.toMultipartBodyPart(): MultipartBody.Part =
        withContext(Dispatchers.IO) {
            val stream = ByteArrayOutputStream()
            this@toMultipartBodyPart.compress(Bitmap.CompressFormat.PNG, 100, stream)
            // 将输出流中的数据转换为字节数组
            val byteArray = stream.toByteArray()
            val requestBody = byteArray.toRequestBody("image/png".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("file", "image.jpg", requestBody)
        }

    data class ImageParam(
        val nameResId: Int,
        val descResId: Int,
        val icon: ImageVector,
        val range: ClosedFloatingPointRange<Float>
    )
}