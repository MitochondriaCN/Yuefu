package com.xianliticn.yuefu.pages

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import coil.compose.AsyncImage
import com.xianliticn.yuefu.R
import com.xianliticn.yuefu.pages.ScanStudioPageViewModel.ImageParam
import com.xianliticn.yuefu.ui.theme.Orange800

@Composable
fun ScanStudioPage(
    viewModel: ScanStudioPageViewModel,
    imageUri: Uri
) {
    val imageParams by viewModel.imageParams.collectAsState()
    val imageModel by viewModel.imageModel.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initialize(imageUri)
    }

    ScanStudioContent(
        modifier = Modifier.fillMaxWidth(),
        imageModel = imageModel,
        imageParams = imageParams,
        onImageParamChange = { param, value ->
            viewModel.handleImageParamChange(param, value)
        },
        onCrop = { x, y -> viewModel.handleCrop(x, y) }
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanStudioContent(
    modifier: Modifier = Modifier,
    imageModel: Any? = null,
    imageParams: Map<ImageParam, Float> = emptyMap(),
    onImageParamChange: (ImageParam, Float) -> Unit = { _, _ -> },
    onConfirm: (Bitmap) -> Unit = {},
    onCrop: (xOffset: Float, yOffset: Float) -> Unit = { _, _ -> }
) {
    var selectedParam by remember { mutableStateOf<ImageParam?>(null) }
    var clippingMode by remember { mutableStateOf(false) }
    var clipLeftLineX by remember { mutableStateOf(0f) }
    var clipTopLineY by remember { mutableStateOf(0f) }

    // AsyncImage大小
    var imageComponentSize by remember { mutableStateOf(IntSize.Zero) }
    // 加载后的图片原始大小
    var originalImageSize by remember { mutableStateOf(IntSize.Zero) }
    val scaleFactor = ContentScale.Fit.computeScaleFactor(
        srcSize = originalImageSize.toSize(),
        dstSize = imageComponentSize.toSize()
    )
    // 缩放后图片的实际尺寸
    val scaledImageWidth = originalImageSize.width * scaleFactor.scaleX
    val scaledImageHeight = originalImageSize.height * scaleFactor.scaleY
    // 图片在 AsyncImage 组件内的左上角偏移量
    val imageOffsetX = (imageComponentSize.width - scaledImageWidth) / 2
    val imageOffsetY = (imageComponentSize.height - scaledImageHeight) / 2

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                stringResource(R.string.optimize_sheet),
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                clippingMode = !clippingMode
            }) { Icon(Icons.Default.Crop, null) }
            IconButton(onClick = {
                onConfirm(imageModel as Bitmap)
            }) { Icon(Icons.Default.Check, null) }
        }
        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
        ) {
            AsyncImage(
                model = imageModel,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        imageComponentSize = coordinates.size
                    },
                onSuccess = { state ->
                    originalImageSize = state.result.drawable.intrinsicWidth.let {
                        IntSize(it, state.result.drawable.intrinsicHeight)
                    }
                },
                contentScale = ContentScale.Fit
            )

            if (clippingMode)
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    translate(left = imageOffsetX, top = imageOffsetY) {
                        drawLine(
                            color = Orange800,
                            start = Offset(x = clipLeftLineX, y = 0f),
                            end = Offset(x = clipLeftLineX, y = scaledImageHeight),
                            strokeWidth = 10f
                        )
                        drawRect(
                            color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.7f),
                            topLeft = if (clipLeftLineX < scaledImageWidth / 2) Offset.Zero else
                                Offset(clipLeftLineX, 0f),
                            size = Size(
                                if (clipLeftLineX < scaledImageWidth / 2) clipLeftLineX else
                                    (scaledImageWidth - clipLeftLineX),
                                scaledImageHeight
                            )
                        )

                        drawLine(
                            color = Orange800,
                            start = Offset(x = 0f, y = clipTopLineY),
                            end = Offset(x = scaledImageWidth, y = clipTopLineY),
                            strokeWidth = 10f
                        )
                        drawRect(
                            color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.7f),
                            topLeft = if (clipTopLineY < scaledImageHeight / 2) Offset.Zero else
                                Offset(0f, clipTopLineY),
                            size = Size(
                                scaledImageWidth,
                                if (clipTopLineY < scaledImageHeight / 2) clipTopLineY else
                                    (scaledImageHeight - clipTopLineY)
                            )
                        )
                    }
                }
        }

        Spacer(Modifier.height(20.dp))

        //参数调整栏
        if (!clippingMode)
            selectedParam?.let { param ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            modifier = Modifier.widthIn(min = 68.dp),
                            text = stringResource(param.nameResId),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Slider(
                            value = imageParams[param] ?: 0f,
                            valueRange = param.range,
                            onValueChange = {
                                onImageParamChange(param, it)
                            })
                    }
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(param.descResId),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        else
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Slider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp),
                    value = clipLeftLineX,
                    valueRange = 0f..scaledImageWidth,
                    onValueChange = {
                        clipLeftLineX = it
                    })
                Slider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp),
                    value = clipTopLineY,
                    valueRange = 0f..scaledImageHeight,
                    onValueChange = {
                        clipTopLineY = it
                    })
            }

        Spacer(Modifier.height(12.dp))

        //参数选择栏
        if (!clippingMode)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                imageParams.forEach {
                    IconButton(
                        onClick = {
                            selectedParam = it.key
                        }
                    ) {
                        Icon(
                            imageVector = it.key.icon,
                            contentDescription = null,
                            tint = if (selectedParam == it.key) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                LocalContentColor.current
                            }
                        )
                    }
                }
            }
        else
            Button(onClick = {
                clippingMode = !clippingMode
                val ratio = originalImageSize.width / scaledImageWidth
                val actualX = clipLeftLineX * ratio
                val actualY = clipTopLineY * ratio
                onCrop(actualX, actualY)
            }) { Text(stringResource(R.string.crop)) }
    }
}