package com.xianliticn.yuefu.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.xianliticn.yuefu.R

@Composable
fun SheetPlayPage(
    viewModel: SheetPlayPageViewModel,
    sheetId: Int
) {
    LaunchedEffect(Unit) {
        viewModel.refresh(sheetId)
    }

    SheetPlayPageContent {
        viewModel.handlePlay()
    }
}

@Composable
fun SheetPlayPageContent(
    modifier: Modifier = Modifier,
    onPlayButtonClick: () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = {
            onPlayButtonClick()
        }) { Text(stringResource(R.string.play)) }
    }
}