package com.xianliticn.yuefu.pages.sheet

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.xianliticn.yuefu.R
import com.xianliticn.yuefu.entities.Sheet
import com.xianliticn.yuefu.ui.components.InputDialog
import com.xianliticn.yuefu.ui.components.animation.LottieEmptyState
import com.xianliticn.yuefu.ui.components.animation.SheetGridSkeleton
import com.xianliticn.yuefu.ui.components.animation.pressScaleEffect
import com.xianliticn.yuefu.ui.theme.ErrorRed
import com.xianliticn.yuefu.ui.theme.InkBrown
import com.xianliticn.yuefu.ui.theme.NotoSerifSc
import com.xianliticn.yuefu.ui.theme.SuccessGreen
import com.xianliticn.yuefu.ui.theme.YuefuTheme
import com.xianliticn.yuefu.utils.toFriendlyString
import com.xianliticn.yuefu.vo.TaskStatus
import java.time.Instant
import java.util.UUID

@Composable
fun SheetPage(viewModel: SheetPageViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    SheetPageContent(
        modifier = Modifier.padding(horizontal = 20.dp),
        sheets = uiState.sheets,
        sheetCovers = uiState.sheetCoverMap,
        downloadingSheet = uiState.downloadingSheet,
        onRefresh = { viewModel.refresh() },
        loading = uiState.loading,
        onItemClick = { viewModel.handleItemClick(it) },
        onSearchQueryChanged = { viewModel.handleSearchQueryChanged(it) },
        onSearch = { viewModel.handleSearch(it) },
        onDeleteSheet = { viewModel.handleDeleteSheet(it) },
        onRenameSheet = { sheet, newName ->
            viewModel.handleRenameSheet(sheet, newName)
        },
        onShareSheet = { viewModel.handleShareSheet(it) }
    )
}

@SuppressLint("FrequentlyChangingValue")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetPageContent(
    modifier: Modifier = Modifier,
    sheets: List<Pair<Sheet, TaskStatus?>> = emptyList(),
    sheetCovers: Map<Sheet, Any?> = emptyMap(),
    downloadingSheet: Sheet? = null,
    loading: Boolean = false,
    onRefresh: () -> Unit = {},
    onItemClick: (Pair<Sheet, TaskStatus?>) -> Unit = {},
    onSearchQueryChanged: (String) -> Unit = {},
    onSearch: (String) -> Unit = {},
    onDeleteSheet: (Sheet) -> Unit = {},
    onRenameSheet: (Sheet, String) -> Unit = { _, _ -> },
    onShareSheet: (Sheet) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var optionSheet by remember { mutableStateOf<Sheet?>(null) }
    var renamingSheet by remember { mutableStateOf<Sheet?>(null) }
    var deletingSheet by remember { mutableStateOf<Sheet?>(null) }

    // Bottom Sheet: 操作菜单
    optionSheet?.let {
        ModalBottomSheet(
            onDismissRequest = { optionSheet = null },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = it.sheetName ?: stringResource(R.string.unknown_sheet),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .basicMarquee()
                )
                Spacer(Modifier.height(4.dp))
                SheetActionItem(
                    icon = Icons.Default.Edit,
                    title = stringResource(R.string.rename),
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = {
                        renamingSheet = it
                        optionSheet = null
                    }
                )
                if (it.isDownloaded)
                    SheetActionItem(
                        icon = Icons.Default.Share,
                        title = stringResource(R.string.share),
                        tint = MaterialTheme.colorScheme.tertiary,
                        onClick = {
                            onShareSheet(it)
                            optionSheet = null
                        }
                    )
                Spacer(Modifier.height(4.dp))
                SheetActionItem(
                    icon = Icons.Default.Delete,
                    title = stringResource(R.string.delete),
                    tint = ErrorRed,
                    isDestructive = true,
                    onClick = {
                        deletingSheet = it
                        optionSheet = null
                    }
                )
            }
        }
    }

    InputDialog(
        showDialog = renamingSheet != null,
        title = "${stringResource(R.string.rename)} ${renamingSheet?.sheetName}",
        placeholder = stringResource(R.string.enter_new_name),
        onDismiss = { renamingSheet = null },
        onConfirm = {
            onRenameSheet(renamingSheet!!, it)
            renamingSheet = null
        }
    )

    // Delete confirmation dialog
    deletingSheet?.let { sheet ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deletingSheet = null },
            title = { Text(stringResource(R.string.delete)) },
            text = {
                Text(
                    stringResource(R.string.delete_confirm_message, sheet.sheetName ?: stringResource(R.string.unknown_sheet))
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        onDeleteSheet(sheet)
                        deletingSheet = null
                    }
                ) {
                    Text(
                        stringResource(R.string.delete),
                        color = ErrorRed
                    )
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { deletingSheet = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 骨架屏：首次加载且无数据时显示
        if (loading && sheets.isEmpty()) {
            SheetGridSkeleton(
                modifier = modifier,
                count = 6
            )
        } else {
        PullToRefreshBox(
            modifier = modifier.fillMaxSize(),
            isRefreshing = loading,
            onRefresh = onRefresh
        ) {
            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize(),
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                // 大标题 — with bottom scrim so content scrolling underneath is dimmed
                stickyHeader {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surface,
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                                    ),
                                    startY = 0f,
                                    endY = 200f
                                )
                            )
                            .padding(top = 12.dp, bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = stringResource(R.string.sheet),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                fontFamily = NotoSerifSc
                            )
                            if (sheets.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                ) {
                                    Text(
                                        text = "${sheets.size}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        // 自定义搜索框
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            tonalElevation = 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    onSearchQueryChanged(it)
                                },
                                placeholder = {
                                    Text(
                                        stringResource(R.string.search_sheets),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // 网格卡片
                items(sheets.size, key = { sheets[it].first.id }) { index ->
                    SheetCard(
                        sheet = sheets[index],
                        onItemClick = onItemClick,
                        onItemHold = { optionSheet = it.first },
                        cover = sheetCovers[sheets[index].first]
                    )
                }
            }

            // 空状态
            if (sheets.isEmpty() && !loading) {
                LottieEmptyState(
                    modifier = Modifier.align(Alignment.Center),
                    title = stringResource(R.string.no_sheets),
                    rawRes = R.raw.empty_sheets
                )
            }
        }
        } // end else (not skeleton)

        // 下载指示器
        downloadingSheet?.let {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.inverseOnSurface
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.downloading),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = it.sheetName ?: stringResource(R.string.unknown_sheet),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    tint: Color,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isDestructive) ErrorRed.copy(alpha = 0.1f)
                else tint.copy(alpha = 0.1f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = tint
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDestructive) ErrorRed else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SheetCard(
    sheet: Pair<Sheet, TaskStatus?>,
    cover: Any? = null,
    onItemClick: (Pair<Sheet, TaskStatus?>) -> Unit,
    onItemHold: (Pair<Sheet, TaskStatus?>) -> Unit
) {
    ElevatedCard(
        onClick = { onItemClick(sheet) },
        modifier = Modifier
            .fillMaxWidth()
            .pressScaleEffect()
    ) {
        // 封面
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            if (cover == null)
                ScorePaperPlaceholder(modifier = Modifier.fillMaxSize())
            else
                AsyncImage(
                    model = cover,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

            // 状态徽章
            val statusBadge = when (sheet.second) {
                TaskStatus.PROCESSING -> StatusBadge(
                    color = MaterialTheme.colorScheme.primary,
                    label = stringResource(R.string.scanning_sheet_short)
                )
                TaskStatus.FAILED -> StatusBadge(
                    color = ErrorRed,
                    label = stringResource(R.string.scanning_failed_short)
                )
                TaskStatus.COMPLETED -> StatusBadge(
                    color = SuccessGreen,
                    label = stringResource(R.string.waiting_to_download_short)
                )
                else -> null
            }
            statusBadge?.let { badge ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badge.color.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = badge.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 标题 + 三点按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sheet.first.sheetName ?: stringResource(R.string.unknown_sheet),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .basicMarquee()
            )
            IconButton(
                onClick = { onItemHold(sheet) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.more_options),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 副标题
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (sheet.second == TaskStatus.PENDING || sheet.second == TaskStatus.PROCESSING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = if (sheet.first.isDownloaded)
                    Instant.ofEpochMilli(
                        sheet.first.lastOpenTime ?: sheet.first.createTime
                    ).toFriendlyString()
                else when (sheet.second) {
                    TaskStatus.PENDING -> stringResource(R.string.pending_to_scan)
                    TaskStatus.PROCESSING -> stringResource(R.string.scanning_sheet)
                    TaskStatus.COMPLETED -> stringResource(R.string.waiting_to_download)
                    TaskStatus.FAILED -> stringResource(R.string.scanning_failed)
                    else -> stringResource(R.string.unknown_status)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
        }
    }
}

private data class StatusBadge(val color: Color, val label: String)

@Composable
@Preview(showBackground = true)
fun SheetPagePreview() {
    YuefuTheme {
        SheetPageContent(
            sheets = listOf(
                Sheet(
                    id = 1,
                    isDownloaded = false,
                    taskId = UUID.randomUUID().toString(),
                    createTime = System.currentTimeMillis(),
                ) to TaskStatus.PENDING,
                Sheet(
                    id = 2,
                    isDownloaded = false,
                    taskId = UUID.randomUUID().toString(),
                    createTime = System.currentTimeMillis(),
                ) to TaskStatus.PENDING,
            ),
            downloadingSheet = Sheet(
                id = 1,
                isDownloaded = false,
                taskId = UUID.randomUUID().toString(),
                createTime = System.currentTimeMillis(),
            ),
        )
    }
}

@Composable
private fun ScorePaperPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(48.dp)
        ) {
            val lineColor = InkBrown.copy(alpha = 0.32f)
            val gap = size.height / 4f
            repeat(5) { i ->
                val y = i * gap
                drawLine(
                    color = lineColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
        Image(
            painter = painterResource(R.drawable.ic_treble_clef_outline),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(0.4f),
            colorFilter = ColorFilter.tint(InkBrown.copy(alpha = 0.45f))
        )
    }
}
