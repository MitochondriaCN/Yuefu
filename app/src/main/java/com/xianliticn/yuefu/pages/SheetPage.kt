package com.xianliticn.yuefu.pages

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xianliticn.yuefu.R
import com.xianliticn.yuefu.entities.Sheet
import com.xianliticn.yuefu.ui.components.InputDialog
import com.xianliticn.yuefu.utils.toFriendlyString
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
    sheets: List<Sheet>,
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
                        text = it.sheetName,
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
        onConfirm = { onRenameSheet(renamingSheet!!, it) }
    )

    PullToRefreshBox(
        modifier = modifier.fillMaxSize(),
        isRefreshing = loading,
        onRefresh = onRefresh
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
        ) {
            stickyHeader {
                SearchBar(
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
            }
            items(count = sheets.size, key = { sheets[it].id }) {
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onItemClick(sheets[it])
                        },
                    headlineContent = {
                        Text(
                            text = sheets[it].sheetName,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = when (sheets[it].fileName.substringAfterLast('.')
                                .lowercase()) {
                                "musicxml", "xml" -> Icons.Default.LibraryMusic
                                "mid", "midi" -> Icons.Default.Piano
                                else -> Icons.Default.QuestionMark
                            },
                            contentDescription = null
                        )
                    },
                    supportingContent = {
                        Text(
                            "${stringResource(R.string.last_open_time)}${
                                Instant.ofEpochMilli(sheets[it].lastOpenTime).toFriendlyString()
                            }"
                        )
                    },
                    trailingContent = {
                        IconButton(onClick = {
                            optionSheet = sheets[it]
                        }) { Icon(Icons.Default.MoreVert, null) }
                    }
                )
            }
        }
    }
}