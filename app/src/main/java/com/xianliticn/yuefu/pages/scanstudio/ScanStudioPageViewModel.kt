package com.xianliticn.yuefu.pages.scanstudio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
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
                //提交OMR任务
                omrApi.submitSheetImage(part).data?.let { vo ->
                    //创建乐谱实体
                    val sheet = Sheet(
                        taskId = vo.taskId,
                        isDownloaded = false,
                        createTime = System.currentTimeMillis(),
                    )
                    appDatabase.sheetDao().insert(sheet)
                    Toast.makeText(context, R.string.omr_task_submitted, Toast.LENGTH_LONG).show()
                    _omrRunning.value = false
                    _finished.value = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, R.string.failed_to_omr, Toast.LENGTH_LONG).show()
                _omrRunning.value = false
            }
        }
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