package com.xianliticn.yuefu.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xianliticn.yuefu.R
import com.xianliticn.yuefu.music.VisualNoteEvent
import com.xianliticn.yuefu.ui.components.PianoRollNoteFlow
import com.xianliticn.yuefu.ui.theme.Clouds

@Composable
fun SheetPlayPage(
    viewModel: SheetPlayPageViewModel,
    sheetId: Int
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh(sheetId)
    }

    SheetPlayPageContent(
        modifier = Modifier.fillMaxSize(),
        notes = uiState.notes,
        currentProgressMillis = uiState.currentProgressMillis,
        isPlaying = uiState.isPlaying,
        onPlayButtonClick = { viewModel.handlePlayOrPause() },
        onProgressChange = { viewModel.handleProgressChange(it) }
    )
}

@Composable
fun SheetPlayPageContent(
    modifier: Modifier = Modifier,
    notes: List<VisualNoteEvent> = emptyList(),
    currentProgressMillis: Long = 0,
    isPlaying: Boolean = false,
    onPlayButtonClick: () -> Unit = {},
    onProgressChange: (Float) -> Unit = {}
) {
    var isScrollMode by remember { mutableStateOf(false) }
    val keyboardHeight = LocalConfiguration.current.screenHeightDp.dp * 0.2f

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Black,
            contentColor = Clouds,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.keyboard_scrolling),
                    style = MaterialTheme.typography.labelMedium
                )
                Switch(
                    checked = isScrollMode,
                    onCheckedChange = { isScrollMode = it })
                Spacer(Modifier.width(20.dp))
                Slider(
                    value = currentProgressMillis.toFloat(),
                    valueRange = 0f..(notes.lastOrNull()?.endTimeMillis?.toFloat() ?: 0f),
                    modifier = Modifier.weight(1f),
                    onValueChange = { onProgressChange(it) }
                )
                IconButton(
                    onClick = { onPlayButtonClick() }
                ) {
                    Icon(
                        if (!isPlaying) Icons.Default.PlayArrow else Icons.Default.Pause,
                        null
                    )
                }
            }
        }

        PianoRollNoteFlow(
            modifier = Modifier.weight(1f),
            isScrollMode = isScrollMode,
            keyboardHeight = keyboardHeight,
            notes = notes,
            currentProgressMillis = currentProgressMillis
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SheetPlayPagePreview() {
    SheetPlayPageContent()
}
