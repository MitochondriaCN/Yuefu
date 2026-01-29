package com.xianliticn.yuefu.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults.InputField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xianliticn.yuefu.R
import com.xianliticn.yuefu.entities.Sheet
import com.xianliticn.yuefu.ui.components.InputDialog
import com.xianliticn.yuefu.ui.theme.Grey800
import com.xianliticn.yuefu.ui.theme.YuefuTheme
import com.xianliticn.yuefu.utils.toFriendlyString
import com.xianliticn.yuefu.vo.TaskStatus
import java.time.Instant

@Composable
fun SheetPage(viewModel: SheetPageViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    SheetPageContent(
        modifier = Modifier.padding(16.dp),
        sheets = uiState.sheets,
        onRefresh = { viewModel.refresh() },
        loading = uiState.loading,
        onItemClick = { viewModel.handleItemClick(it) },
        onSearchQueryChanged = { viewModel.handleSearchQueryChanged(it) },
        onSearch = { viewModel.handleSearch(it) },
        onDeleteSheet = { viewModel.handleDeleteSheet(it) },
        onRenameSheet = { sheet, newName ->
            viewModel.handleRenameSheet(sheet, newName)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetPageContent(
    modifier: Modifier = Modifier,
    sheets: List<Pair<Sheet, TaskStatus?>> = emptyList(),
    loading: Boolean = false,
    onRefresh: () -> Unit = {},
    onItemClick: (Sheet) -> Unit = {},
    onSearchQueryChanged: (String) -> Unit = {},
    onSearch: (String) -> Unit = {},
    onDeleteSheet: (Sheet) -> Unit = {},
    onRenameSheet: (Sheet, String) -> Unit = { _, _ -> }
) {
    var searchQuery by remember { mutableStateOf("") }
    var optionSheet by remember { mutableStateOf<Sheet?>(null) }
    var renamingSheet by remember { mutableStateOf<Sheet?>(null) }

    optionSheet?.let {
        ModalBottomSheet(
            onDismissRequest = { optionSheet = null },
            content = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = it.sheetName ?: stringResource(R.string.unknown_sheet),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee()
                    )
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(stringResource(R.string.delete)) },
                        leadingContent = { Icon(Icons.Default.Delete, null) },
                        modifier = Modifier.clickable {
                            onDeleteSheet(it)
                            optionSheet = null
                        }
                    )
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(stringResource(R.string.rename)) },
                        leadingContent = { Icon(Icons.Default.Edit, null) },
                        modifier = Modifier.clickable {
                            renamingSheet = it
                            optionSheet = null
                        }
                    )
                }
            }
        )
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

    PullToRefreshBox(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        isRefreshing = loading,
        onRefresh = onRefresh
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SearchBar(
                modifier = Modifier.fillMaxWidth(),
                state = rememberSearchBarState(),
                inputField = {
                    InputField(
                        query = searchQuery,
                        onQueryChange = {
                            searchQuery = it
                            onSearchQueryChanged(it)
                        },
                        onSearch = { onSearch(searchQuery) },
                        expanded = false,
                        onExpandedChange = {},
                        placeholder = { Text(stringResource(R.string.search_sheets)) },
                        leadingIcon = { Icon(Icons.Default.Search, null) }
                    )
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 第一列元素数为：总数/2并向上取整
                    items(
                        count = (sheets.size + 1) / 2,
                        key = { sheets[it].first }) { index ->
                        // 只取偶数索引的元素
                        val sheetIndex = index * 2
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onItemClick(sheets.toList()[sheetIndex].first) },
                        ) {
                            // 封面
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.fyx),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // 标题
                            Text(
                                text = sheets[sheetIndex].first.sheetName
                                    ?: stringResource(R.string.unknown_sheet),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .basicMarquee()
                            )
                            //上次打开日期或识别状态
                            Text(
                                text = if (sheets[sheetIndex].first.isDownloaded)
                                    Instant.ofEpochMilli(
                                        sheets[sheetIndex].first.lastOpenTime
                                            ?: sheets[sheetIndex].first.createTime
                                    ).toFriendlyString()
                                else when (sheets[sheetIndex].second) {
                                    TaskStatus.PENDING -> stringResource(R.string.pending_to_scan)
                                    TaskStatus.PROCESSING -> stringResource(R.string.scanning_sheet)
                                    TaskStatus.COMPLETED -> stringResource(R.string.waiting_to_download)
                                    TaskStatus.FAILED -> stringResource(R.string.scanning_failed)
                                    else -> stringResource(R.string.unknown_status)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Grey800,
                                maxLines = 1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .basicMarquee()
                            )
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 第二列元素数为：总数/2并向下取整
                    items(count = sheets.size / 2) { index ->
                        // 只取奇数索引的元素
                        val sheetIndex = index * 2 + 1
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onItemClick(sheets[sheetIndex].first) },
                        ) {
                            // 封面
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.fyx),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // 标题
                            Text(
                                text = sheets[sheetIndex].first.sheetName
                                    ?: stringResource(R.string.unknown_sheet),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .basicMarquee()
                            )
                            //上次打开日期或识别状态
                            Text(
                                text = if (sheets[sheetIndex].first.isDownloaded)
                                    Instant.ofEpochMilli(
                                        sheets[sheetIndex].first.lastOpenTime
                                            ?: sheets[sheetIndex].first.createTime
                                    ).toFriendlyString()
                                else when (sheets[sheetIndex].second) {
                                    TaskStatus.PENDING -> stringResource(R.string.pending_to_scan)
                                    TaskStatus.PROCESSING -> stringResource(R.string.scanning_sheet)
                                    TaskStatus.COMPLETED -> stringResource(R.string.waiting_to_download)
                                    TaskStatus.FAILED -> stringResource(R.string.scanning_failed)
                                    else -> stringResource(R.string.unknown_status)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Grey800,
                                maxLines = 1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .basicMarquee()
                            )
                        }

                    }
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun SheetPagePreview() {
    YuefuTheme {
        SheetPageContent(
            sheets = listOf(
                Sheet(
                    id = 1,
                    isDownloaded = false,
                    taskId = 1,
                    createTime = System.currentTimeMillis(),
                ) to TaskStatus.PENDING
            )
        )
    }
}
