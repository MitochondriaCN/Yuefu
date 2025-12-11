package com.xianliticn.yuefu.pages

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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xianliticn.yuefu.R
import com.xianliticn.yuefu.entities.Sheet
import com.xianliticn.yuefu.ui.theme.Amber800
import com.xianliticn.yuefu.ui.theme.Blue800
import com.xianliticn.yuefu.ui.theme.Clouds
import com.xianliticn.yuefu.ui.theme.Grey800
import com.xianliticn.yuefu.utils.toFriendlyString
import java.time.Instant

@Composable
fun HomePage(viewModel: HomePageViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.handleImportFile(uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    HomePageContent(
        loading = uiState.loading,
        recent4 = uiState.recent4,
        onShotSheetClick = { /*TODO*/ },
        onImportFileClick = {
            filePickerLauncher.launch(
                arrayOf(
                    "text/xml",
                    "application/xml",
                    "application/octet-stream" //MIDI
                )
            )
        }
    )
}

@Composable
fun HomePageContent(
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    recent4: List<Sheet> = emptyList(),
    onShotSheetClick: () -> Unit = {},
    onImportFileClick: () -> Unit = {}
) {
    if (loading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
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
                ) { onShotSheetClick() }
                ButtonCard(
                    icon = Icons.Default.AttachFile,
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.import_file),
                    bgColor = Amber800
                ) { onImportFileClick() }
            }
            Spacer(Modifier.height(20.dp))
            RecentlyUsedFile(
                modifier = Modifier.fillMaxWidth(),
                recent4 = recent4
            )
        }
    }
}

@Composable
fun RecentlyUsedFile(
    modifier: Modifier = Modifier,
    recent4: List<Sheet> = emptyList()
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
                        label = it.sheetName,
                        lastOpenTime = Instant.ofEpochMilli(it.lastOpenTime).toFriendlyString()
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
                        label = it.sheetName,
                        lastOpenTime = Instant.ofEpochMilli(it.lastOpenTime).toFriendlyString()
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
        modifier = modifier.clickable { onClick() },
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
    lastOpenTime: String
) {
    ElevatedCard(modifier = modifier) {
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
    HomePageContent()
}