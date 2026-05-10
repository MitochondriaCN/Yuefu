package com.xianliticn.yuefu.pages.sheetoverview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import com.xianliticn.yuefu.ui.components.animation.LottieLoadingView
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.xianliticn.yuefu.R
import com.xianliticn.yuefu.ui.components.InfoBox
import com.xianliticn.yuefu.ui.theme.NotoSerifSc

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetOverviewPage(
    viewModel: SheetOverviewPageViewModel,
    sheetId: Int,
    onBackPress: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val imageModel by viewModel.sheetPicture.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh(sheetId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBackPress) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.loading)
            LottieLoadingView()
        else
            SheetOverviewPageContent(
                modifier = Modifier.padding(paddingValues),
                sheetName = uiState.sheetTitle ?: stringResource(R.string.unknown_sheet),
                sheetAuthor = uiState.sheetAuthor ?: stringResource(R.string.unknown_author),
                sheetCreatedTime = uiState.sheetCreatedTime,
                sheetMeasureCount = uiState.sheetMeasureCount,
                sheetModel = uiState.sheetModel,
                sheetPicture = imageModel
            )
    }
}

@Composable
fun SheetOverviewPageContent(
    modifier: Modifier = Modifier,
    sheetName: String,
    sheetAuthor: String,
    sheetCreatedTime: String? = null,
    sheetMeasureCount: Int? = null,
    sheetModel: String? = null,
    sheetPicture: Any? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Spacer(Modifier.height(8.dp))


        Column(
            modifier= Modifier.fillMaxWidth()
        ) {
            Text(
                text = sheetName,
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = NotoSerifSc,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = sheetAuthor,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(4.dp))

        InfoBox(
            title = stringResource(R.string.created_time),
            content = sheetCreatedTime ?: stringResource(R.string.unknown),
            icon = Icons.Default.DateRange
        )
        InfoBox(
            title = stringResource(R.string.measure_count),
            content = sheetMeasureCount?.toString() ?: stringResource(R.string.unknown),
            icon = Icons.Default.VerticalSplit
        )
        InfoBox(
            title = stringResource(R.string.model),
            content = sheetModel ?: stringResource(R.string.unknown),
            icon = Icons.Default.Psychology
        )

        if (sheetPicture == null)
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 20.dp)
            )

        AsyncImage(
            model = sheetPicture,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Fit
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun SheetOverviewPagePreview() {
    SheetOverviewPageContent(
        sheetName = "Rhapsody in The Blue in B Flat, Op. 21",
        sheetAuthor = "P. I. Tchaikovsky",
    )
}