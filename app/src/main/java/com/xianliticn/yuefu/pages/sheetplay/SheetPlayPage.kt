package com.xianliticn.yuefu.pages.sheetplay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xianliticn.yuefu.R
import com.xianliticn.yuefu.music.VisualNoteEvent
import com.xianliticn.yuefu.ui.components.EffectLevel
import com.xianliticn.yuefu.ui.components.PianoRollNoteFlow
import com.xianliticn.yuefu.ui.theme.ControlBarBlurEdge
import com.xianliticn.yuefu.ui.theme.ControlBarGlassBottom
import com.xianliticn.yuefu.ui.theme.ControlBarGlassTop
import com.xianliticn.yuefu.ui.theme.ControlBarSeparator

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
        currentMeasure = uiState.currentMeasure,
        maxMeasure = uiState.maxMeasure,
        effectLevel = uiState.effectLevel,
        onPlayButtonClick = { viewModel.handlePlayOrPause() },
        onProgressChange = { viewModel.handleProgressChange(it) },
        onMeasureChange = { viewModel.handleMeasureChange(it) }
    )
}

@Composable
fun SheetPlayPageContent(
    modifier: Modifier = Modifier,
    notes: List<VisualNoteEvent> = emptyList(),
    currentProgressMillis: Long = 0,
    isPlaying: Boolean = false,
    currentMeasure: Int = 1,
    maxMeasure: Int = 1,
    effectLevel: EffectLevel = EffectLevel.HIGH,
    onPlayButtonClick: () -> Unit = {},
    onProgressChange: (Float) -> Unit = {},
    onMeasureChange: (Int) -> Unit = {}
) {
    var isScrollMode by remember { mutableStateOf(false) }
    val keyboardHeight = LocalConfiguration.current.screenHeightDp.dp * 0.2f

    Column(modifier = modifier.fillMaxSize()) {
        // 磨砂玻璃控制栏
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(ControlBarGlassTop, ControlBarGlassBottom)
                    )
                )
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
                Text(
                    text = stringResource(R.string.measure_format, currentMeasure, maxMeasure),
                    style = MaterialTheme.typography.labelMedium
                )
                Slider(
                    value = currentMeasure.toFloat(),
                    valueRange = 1f..maxMeasure.toFloat(),
                    steps = (maxMeasure - 2).coerceAtLeast(0),
                    modifier = Modifier.width(120.dp),
                    onValueChange = { onMeasureChange(it.toInt()) }
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

            // 底部分隔线
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.BottomCenter)
                    .background(ControlBarSeparator)
            )
        }

        // 控制栏下方模糊边缘
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(ControlBarBlurEdge, Color.Transparent)
                    )
                )
        )

        PianoRollNoteFlow(
            modifier = Modifier.weight(1f),
            isScrollMode = isScrollMode,
            keyboardHeight = keyboardHeight,
            notes = notes,
            currentProgressMillis = currentProgressMillis,
            effectLevel = effectLevel
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SheetPlayPagePreview() {
    SheetPlayPageContent()
}
