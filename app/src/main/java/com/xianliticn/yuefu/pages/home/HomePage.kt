package com.xianliticn.yuefu.pages.home

import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.xianliticn.yuefu.R
import com.xianliticn.yuefu.entities.Sheet
import com.xianliticn.yuefu.ui.components.ScanningTutorialBottomSheet
import com.xianliticn.yuefu.ui.theme.InkBrown
import com.xianliticn.yuefu.ui.theme.InkSoft
import com.xianliticn.yuefu.ui.theme.NotoSerifSc
import com.xianliticn.yuefu.ui.theme.Ochre
import com.xianliticn.yuefu.ui.theme.PaleOchre
import com.xianliticn.yuefu.ui.theme.WarmBg
import com.xianliticn.yuefu.utils.toFriendlyString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.Instant

@Composable
fun HomePage(
    viewModel: HomePageViewModel,
    onImageSelected: (Uri) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imageUri?.let {
                onImageSelected(it)
            }
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            scope.launch {
                val safeUri = withContext(Dispatchers.IO) {
                    copyToAppCache(context, it) ?: it
                }
                onImageSelected(safeUri)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    HomePageContent(
        modifier = Modifier,
        loading = uiState.loading,
        loadingMessage = uiState.loadingMessage,
        recent4 = uiState.recent4,
        onTutorialShown = viewModel::handleTutorialShown,
        onTakePhotoClick = {
            val file = File(context.cacheDir, "${System.currentTimeMillis()}-img")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            imageUri = uri
            takePictureLauncher.launch(uri)
        },
        onPickImageClick = {
            pickImageLauncher.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        },
        onRecentSheetClick = {
            viewModel.handleRecentSheetClick(it)
        },
        showTutorial = uiState.showTutorial
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePageContent(
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    loadingMessage: String? = null,
    recent4: List<Sheet> = emptyList(),
    showTutorial: Boolean = true,
    onTutorialShown: () -> Unit = {},
    onTakePhotoClick: () -> Unit = {},
    onPickImageClick: () -> Unit = {},
    onRecentSheetClick: (Sheet) -> Unit = {}
) {
    var showingTutorial by remember { mutableStateOf(false) }
    var gettingImage by remember { mutableStateOf(false) }

    if (loading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Ochre)
                Text(
                    text = loadingMessage.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSoft,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        if (gettingImage)
            ModalBottomSheet(
                onDismissRequest = { gettingImage = false }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.select_image_source),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee()
                    )
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(stringResource(R.string.take_photo)) },
                        leadingContent = { Icon(Icons.Default.PhotoCamera, null) },
                        modifier = Modifier.clickable {
                            gettingImage = false
                            onTakePhotoClick()
                        }
                    )
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(stringResource(R.string.pick_image)) },
                        leadingContent = { Icon(Icons.Default.Photo, null) },
                        modifier = Modifier.clickable {
                            gettingImage = false
                            onPickImageClick()
                        }
                    )
                }
            }
        if (showingTutorial)
            ScanningTutorialBottomSheet(
                onDismissRequest = {
                    showingTutorial = false
                    gettingImage = true
                }
            )

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(28.dp))

                // ── Hero ──
                Text(
                    text = stringResource(R.string.app_name),
                    style = TextStyle(
                        fontFamily = NotoSerifSc,
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                        letterSpacing = 0.15.em,
                        color = InkBrown
                    )
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.home_greeting),
                    style = TextStyle(
                        fontFamily = NotoSerifSc,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = InkSoft,
                        letterSpacing = 0.05.em
                    )
                )

                Spacer(Modifier.height(20.dp))

                // ── Scan CTA ──
                ScanCard {
                    if (showTutorial) {
                        showingTutorial = true
                        onTutorialShown()
                    } else
                        gettingImage = true
                }

                Spacer(Modifier.height(8.dp))

                // ── Section header ──
                SectionHeader(text = stringResource(R.string.recently_used))

                // ── Recent file cards ──
                if (recent4.isNotEmpty()) {
                    recent4.forEachIndexed { index, sheet ->
                        AnimatedFileCard(
                            index = index,
                            label = sheet.sheetName ?: stringResource(R.string.unknown_sheet),
                            lastOpenTime = Instant.ofEpochMilli(sheet.lastOpenTime ?: sheet.createTime)
                                .toFriendlyString(),
                            onClick = { onRecentSheetClick(sheet) }
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.no_recently_used_sheets),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        style = TextStyle(
                            fontFamily = NotoSerifSc,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = InkSoft
                        )
                    )
                }
            }
        }
    }
}

// ── Scan Card ──

@Composable
private fun ScanCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(WarmBg, PaleOchre)))
            .border(1.dp, Ochre.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(24.dp)
    ) {
        // 顶部高光
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
                .align(Alignment.TopCenter)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Ochre.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    modifier = Modifier.size(28.dp),
                    tint = Ochre,
                    contentDescription = null
                )
            }

            Spacer(Modifier.width(16.dp))

            Column {
                Text(
                    text = stringResource(R.string.shot_sheet),
                    style = TextStyle(
                        fontFamily = NotoSerifSc,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        letterSpacing = 0.04.em,
                        color = InkBrown
                    )
                )
                Text(
                    text = stringResource(R.string.scan_sheet_subtitle),
                    style = TextStyle(fontSize = 13.sp, color = InkSoft)
                )
            }
        }

        // 铜印
        BronzeSeal(modifier = Modifier.align(Alignment.BottomEnd))
    }
}

@Composable
private fun BronzeSeal(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(34.dp)
            .rotate(-12f)
            .border(1.5.dp, Ochre, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "乐",
            style = TextStyle(
                fontFamily = NotoSerifSc,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Ochre.copy(alpha = 0.55f)
            )
        )
    }
}

// ── Section Header ──

@Composable
private fun SectionHeader(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(14.dp)
                .background(Ochre, RoundedCornerShape(1.dp))
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = TextStyle(
                fontFamily = NotoSerifSc,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Ochre,
                letterSpacing = 0.3.em
            )
        )
    }
}

// ── File Card ──

@Composable
private fun AnimatedFileCard(
    index: Int,
    label: String,
    lastOpenTime: String,
    onClick: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(500, delayMillis = 100 * index)) +
                slideInVertically(tween(500, delayMillis = 100 * index)) { it / 4 }
    ) {
        FileCard(
            label = label,
            lastOpenTime = lastOpenTime,
            onClick = onClick
        )
    }
}

@Composable
fun FileCard(
    modifier: Modifier = Modifier,
    label: String,
    type: FileCardType = FileCardType.MusicXml,
    lastOpenTime: String,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Ochre.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Ochre.copy(alpha = 0.10f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (type) {
                        FileCardType.MusicXml -> Icons.Default.LibraryMusic
                        FileCardType.Midi -> Icons.Default.Piano
                    },
                    modifier = Modifier.size(22.dp),
                    tint = Ochre,
                    contentDescription = null
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = InkBrown,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee()
                )
                Text(
                    text = lastOpenTime,
                    fontSize = 13.sp,
                    color = InkSoft,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
    }
}

enum class FileCardType {
    MusicXml,
    Midi
}

@Preview(showBackground = true, locale = "zh", showSystemUi = true)
@Composable
fun HomePagePreview() {
    HomePageContent(
        loading = false,
        loadingMessage = "AI识别乐谱中\n可能需要约1分钟。"
    )
}

private fun copyToAppCache(context: android.content.Context, sourceUri: Uri): Uri? {
    val resolver = context.contentResolver
    val mimeType = resolver.getType(sourceUri).orEmpty()
    val extension = MimeTypeMap.getSingleton()
        .getExtensionFromMimeType(mimeType)
        ?.takeIf { it.isNotBlank() }
        ?: "jpg"
    val targetFile = File(context.cacheDir, "${System.currentTimeMillis()}-picked.$extension")

    return runCatching {
        resolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        } ?: return null

        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            targetFile
        )
    }.getOrNull()
}
