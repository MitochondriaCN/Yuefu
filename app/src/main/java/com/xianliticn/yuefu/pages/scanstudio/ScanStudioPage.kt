package com.xianliticn.yuefu.pages.scanstudio

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import coil.compose.AsyncImage
import com.xianliticn.yuefu.R
import com.xianliticn.yuefu.ui.components.animation.LottieLoadingView
import com.xianliticn.yuefu.ui.theme.BlueAccent
import kotlinx.coroutines.delay

@Composable
fun ScanStudioPage(
    viewModel: ScanStudioPageViewModel,
    imageUri: Uri,
    onFinished: () -> Unit
) {
    val imageParams by viewModel.imageParams.collectAsState()
    val imageModel by viewModel.imageModel.collectAsState()
    val finished by viewModel.finished.collectAsState()
    val omrRunning by viewModel.omrRunning.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initialize(imageUri)
    }

    LaunchedEffect(finished) {
        if (finished)
            onFinished()
    }

    ScanStudioContent(
        modifier = Modifier.fillMaxWidth(),
        imageModel = imageModel,
        imageParams = imageParams,
        omrRunning = omrRunning,
        onImageParamChange = { param, value ->
            viewModel.handleImageParamChange(param, value)
        },
        onCrop = { x, y -> viewModel.handleCrop(x, y) },
        onConfirm = { bitmap, model ->
            viewModel.handleConfirm(bitmap, model)
        }
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanStudioContent(
    modifier: Modifier = Modifier,
    imageModel: Any? = null,
    imageParams: Map<ScanStudioPageViewModel.ImageParam, Float> = emptyMap(),
    omrRunning: Boolean = false,
    onImageParamChange: (ScanStudioPageViewModel.ImageParam, Float) -> Unit = { _, _ -> },
    onConfirm: (Bitmap, OmrModel) -> Unit = { _, _ -> },
    onCrop: (xOffset: Float, yOffset: Float) -> Unit = { _, _ -> }
) {
    var selectedParam by remember { mutableStateOf<ScanStudioPageViewModel.ImageParam?>(null) }
    var tipIndex by remember { mutableIntStateOf(0) }
    val tips = listOf(
        R.string.scanning_tip_1,
        R.string.scanning_tip_2,
        R.string.scanning_tip_3,
        R.string.scanning_tip_4,
        R.string.scanning_tip_5,
        R.string.scanning_tip_6,
    )

    var clippingMode by remember { mutableStateOf(false) }
    var showModelMenu by remember { mutableStateOf(false) }
    var clipLeftLineX by remember { mutableFloatStateOf(0f) }
    var clipTopLineY by remember { mutableFloatStateOf(0f) }
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

    LaunchedEffect(Unit) {
        while (true) {
            delay(3000L)
            tipIndex = (tipIndex + 1) % tips.size
        }
    }

    if (omrRunning)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LottieLoadingView(
                    message = stringResource(R.string.updating_sheet)
                )
                AnimatedContent(
                    targetState = tipIndex,
                    transitionSpec = {
                        fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
                    }
                ) {
                    Text(stringResource(tips[it]))
                }
            }
        }
    else
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
                }) {
                    Icon(
                        imageVector = Icons.Default.Crop,
                        contentDescription = null,
                        tint = if (clippingMode) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                }
                Box {
                    IconButton(onClick = {
                        showModelMenu = true
                    }) { Icon(Icons.Default.Check, null) }
                    DropdownMenu(
                        expanded = showModelMenu,
                        onDismissRequest = { showModelMenu = false }
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            text = stringResource(R.string.choose_model),
                            style = MaterialTheme.typography.titleMedium
                        )
                        OmrModel.entries.forEach { model ->
                            DropdownMenuItem(
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                text = {
                                    Column {
                                        Text(
                                            text = model.label,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                        Text(
                                            text = model.description,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                },
                                onClick = {
                                    showModelMenu = false
                                    val bitmap = imageModel as? Bitmap ?: return@DropdownMenuItem
                                    onConfirm(bitmap, model)
                                }
                            )
                        }
                    }
                }
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
                                color = BlueAccent,
                                start = Offset(x = clipLeftLineX, y = 0f),
                                end = Offset(x = clipLeftLineX, y = scaledImageHeight),
                                strokeWidth = 10f
                            )
                            drawRect(
                                color = Color.Black.copy(alpha = 0.4f),
                                topLeft = if (clipLeftLineX < scaledImageWidth / 2) Offset.Zero else
                                    Offset(clipLeftLineX, 0f),
                                size = Size(
                                    if (clipLeftLineX < scaledImageWidth / 2) clipLeftLineX else
                                        (scaledImageWidth - clipLeftLineX),
                                    scaledImageHeight
                                )
                            )

                            drawLine(
                                color = BlueAccent,
                                start = Offset(x = 0f, y = clipTopLineY),
                                end = Offset(x = scaledImageWidth, y = clipTopLineY),
                                strokeWidth = 10f
                            )
                            drawRect(
                                color = Color.Black.copy(alpha = 0.4f),
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
                    clipLeftLineX = 0f
                    clipTopLineY = 0f
                }) { Text(stringResource(R.string.crop)) }
        }
}

@Preview(showBackground = true)
@Composable
fun ScanStudioContentPreview() {
    ScanStudioContent(
        omrRunning = false
    )
}
