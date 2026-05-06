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
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Piano
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.xianliticn.yuefu.R
import com.xianliticn.yuefu.entities.Sheet
import com.xianliticn.yuefu.ui.components.ScanningTutorialBottomSheet
import com.xianliticn.yuefu.ui.theme.NotoSerifSc
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
        modifier = Modifier.padding(horizontal = 20.dp),
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

    // One-shot staggered animation — only plays once per session, not on every navigation
    var animationPlayed by rememberSaveable { mutableStateOf(false) }
    var headerVisible by remember { mutableStateOf(animationPlayed) }
    var actionsVisible by remember { mutableStateOf(animationPlayed) }
    var recentVisible by remember { mutableStateOf(animationPlayed) }

    LaunchedEffect(Unit) {
        if (!animationPlayed) {
            headerVisible = true
            kotlinx.coroutines.delay(100)
            actionsVisible = true
            kotlinx.coroutines.delay(150)
            recentVisible = true
            animationPlayed = true
        }
    }

    if (loading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = loadingMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        leadingContent = { Icon(Icons.Default.CameraAlt, null) },
                        modifier = Modifier.clickable {
                            gettingImage = false
                            onTakePhotoClick()
                        }
                    )
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(stringResource(R.string.pick_image)) },
                        leadingContent = { Icon(Icons.Default.Image, null) },
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

        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Hero Header
            item {
                AnimatedVisibility(
                    visible = headerVisible,
                    enter = fadeIn(tween(500)) + slideInVertically(
                        animationSpec = tween(500),
                        initialOffsetY = { it / 3 }
                    )
                ) {
                    Column {
                        Spacer(Modifier.height(32.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            fontFamily = NotoSerifSc
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.home_subtitle),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }

            // Action Cards
            item {
                AnimatedVisibility(
                    visible = actionsVisible,
                    enter = fadeIn(tween(500)) + slideInVertically(
                        animationSpec = tween(500),
                        initialOffsetY = { it / 3 }
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.CameraAlt,
                            title = stringResource(R.string.shot_sheet),
                            subtitle = stringResource(R.string.shot_sheet_desc),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            onClick = {
                                if (showTutorial) {
                                    showingTutorial = true
                                    onTutorialShown()
                                } else
                                    gettingImage = true
                            }
                        )
                        ActionCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Image,
                            title = stringResource(R.string.pick_image),
                            subtitle = stringResource(R.string.pick_image_desc),
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            onClick = {
                                if (showTutorial) {
                                    showingTutorial = true
                                    onTutorialShown()
                                } else
                                    gettingImage = true
                            }
                        )
                    }
                }
            }

            // Recent Section Header
            item {
                AnimatedVisibility(
                    visible = recentVisible,
                    enter = fadeIn(tween(500)) + slideInVertically(
                        animationSpec = tween(500),
                        initialOffsetY = { it / 4 }
                    )
                ) {
                    Column {
                        Spacer(Modifier.height(32.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.recently_used),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (recent4.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Outlined.FolderOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            // Recent Items
            if (recent4.isEmpty()) {
                item {
                    EmptyRecentState()
                }
            } else {
                itemsIndexed(recent4, key = { _, sheet -> sheet.id }) { index, sheet ->
                    FileCard(
                        label = sheet.sheetName ?: stringResource(R.string.unknown_sheet),
                        lastOpenTime = Instant.ofEpochMilli(
                            sheet.lastOpenTime ?: sheet.createTime
                        ).toFriendlyString(),
                        onClick = { onRecentSheetClick(sheet) }
                    )
                    if (index < recent4.lastIndex) {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                modifier = Modifier.size(32.dp),
                contentDescription = null
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.7f)
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (type) {
                            FileCardType.MusicXml -> Icons.Default.LibraryMusic
                            FileCardType.Midi -> Icons.Outlined.Piano
                        },
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Text(
                    text = lastOpenTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
    }
}

@Composable
private fun EmptyRecentState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Text(
            text = stringResource(R.string.no_recently_used_sheets),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

enum class FileCardType {
    MusicXml,
    Midi
}

@Preview(showBackground = true, locale = "en", showSystemUi = true)
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
