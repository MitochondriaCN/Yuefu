package com.xianliticn.yuefu.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.xianliticn.yuefu.ui.components.PianoRollNoteFlow
import com.xianliticn.yuefu.ui.components.VisualNoteEvent

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
        currentProgressMillis = uiState.currentProgressMillis
    )
}

@Composable
fun SheetPlayPageContent(
    modifier: Modifier = Modifier,
    notes: List<VisualNoteEvent> = emptyList(),
    currentProgressMillis: Long = 0
) {
    Column(modifier = modifier.fillMaxSize()) {
        PianoRollNoteFlow(
            modifier = Modifier.weight(1f),
            isScrollMode = true,
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