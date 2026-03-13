package com.xianliticn.yuefu.pages.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.xianliticn.yuefu.R
import com.xianliticn.yuefu.entities.Sheet
import com.xianliticn.yuefu.ui.components.ScanningTutorialBottomSheet
import com.xianliticn.yuefu.ui.theme.Blue800
import com.xianliticn.yuefu.ui.theme.Clouds
import com.xianliticn.yuefu.ui.theme.Grey800
import com.xianliticn.yuefu.utils.toFriendlyString
import java.io.File
import java.time.Instant

@Composable
fun HomePage(
    viewModel: HomePageViewModel,
    onImageSelected: (Uri) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
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
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            onImageSelected(it)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    HomePageContent(
        modifier = Modifier.padding(horizontal = 16.dp),
        loading = uiState.loading,
        loadingMessage = uiState.loadingMessage,
        recent4 = uiState.recent4,
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
            pickImageLauncher.launch("image/*")
        },
        onRecentSheetClick = {
            viewModel.handleRecentSheetClick(it)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePageContent(
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    loadingMessage: String? = null,
    recent4: List<Sheet> = emptyList(),
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
                CircularProgressIndicator()
                Text(
                    text = loadingMessage.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        if (gettingImage)
            ModalBottomSheet(
                onDismissRequest = {
                    gettingImage = false
                }
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
        Column(
            modifier = modifier.fillMaxWidth(),
        ) {
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ButtonCard(
                    icon = Icons.Default.Camera,
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.shot_sheet),
                    bgColor = Blue800
                ) { showingTutorial = true }
            }
            Spacer(Modifier.height(20.dp))
            RecentlyUsedFile(
                modifier = Modifier.fillMaxWidth(),
                recent4 = recent4,
                onItemClick = { onRecentSheetClick(it) }
            )
        }
    }
}

@Composable
fun RecentlyUsedFile(
    modifier: Modifier = Modifier,
    recent4: List<Sheet> = emptyList(),
    onItemClick: (Sheet) -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.recently_used),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 0..1)
                recent4.getOrNull(i)?.let {
                    FileCard(
                        modifier = Modifier.weight(1f),
                        label = it.sheetName ?: stringResource(R.string.unknown_sheet),
                        lastOpenTime = Instant.ofEpochMilli(it.lastOpenTime ?: it.createTime)
                            .toFriendlyString(),
                        onClick = { onItemClick(it) }
                    )
                }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 2..3)
                recent4.getOrNull(i)?.let {
                    FileCard(
                        modifier = Modifier.weight(1f),
                        label = it.sheetName ?: stringResource(R.string.unknown_sheet),
                        lastOpenTime = Instant.ofEpochMilli(it.lastOpenTime ?: it.createTime)
                            .toFriendlyString(),
                        onClick = { onItemClick(it) }
                    )
                }
        }
    }
}

@Composable
private fun ButtonCard(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    bgColor: Color,
    onClick: () -> Unit = {}
) {
    ElevatedCard(
        modifier = modifier,
        onClick = { onClick() },
        colors = CardDefaults.elevatedCardColors(
            containerColor = bgColor,
            contentColor = Clouds
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                modifier = Modifier.size(40.dp),
                contentDescription = null
            )
            Spacer(Modifier.height(16.dp))
            Text(
                label,
                style = MaterialTheme.typography.headlineSmall
            )
        }
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
    ElevatedCard(
        modifier = modifier,
        onClick = { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = when (type) {
                    FileCardType.MusicXml -> Icons.Default.LibraryMusic
                    FileCardType.Midi -> Icons.Default.Piano
                },
                tint = Grey800,
                contentDescription = null
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Text(
                    text = lastOpenTime,
                    style = MaterialTheme.typography.bodySmall,
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

@Preview(showBackground = true, locale = "en")
@Composable
fun HomePagePreview() {
    HomePageContent(
        loading = true,
        loadingMessage = "AI识别乐谱中\n可能需要约1分钟。"
    )
}