package com.xianliticn.yuefu.pages

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.xianliticn.yuefu.R
import com.xianliticn.yuefu.pages.ScanStudioPageViewModel.ImageParam

@Composable
fun ScanStudioPage(
    viewModel: ScanStudioPageViewModel,
    imageUri: Uri
) {
    val imageParams by viewModel.imageParams.collectAsState()
    val imageModel by viewModel.imageModel.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initialize(imageUri)
    }

    ScanStudioContent(
        modifier = Modifier.fillMaxWidth(),
        imageModel = imageModel,
        imageParams = imageParams,
        onImageParamChange = { param, value ->
            viewModel.handleImageParamChange(param, value)
        }
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanStudioContent(
    modifier: Modifier = Modifier,
    imageModel: Any? = null,
    imageParams: Map<ImageParam, Float> = emptyMap(),
    onImageParamChange: (ImageParam, Float) -> Unit = { _, _ -> },
    onConfirm: (Bitmap) -> Unit = {}
) {
    var selectedParam by remember { mutableStateOf<ImageParam?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                stringResource(R.string.optimize_sheet),
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                onConfirm(imageModel as Bitmap)
            }) { Icon(Icons.Default.Check, null) }
        }
        Spacer(Modifier.height(20.dp))

        AsyncImage(
            model = imageModel,
            contentDescription = null,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(20.dp))

        //参数调整栏
        selectedParam?.let { param ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        modifier = Modifier.widthIn(min = 68.dp),
                        text = stringResource(param.nameResId),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = imageParams[param] ?: 0f,
                        valueRange = param.range,
                        onValueChange = {
                            onImageParamChange(param, it)
                        })
                }
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(param.descResId),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        //参数选择栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            imageParams.forEach {
                IconButton(
                    onClick = {
                        selectedParam = it.key
                    }
                ) {
                    Icon(
                        imageVector = it.key.icon,
                        contentDescription = null,
                        tint = if (selectedParam == it.key) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            LocalContentColor.current
                        }
                    )
                }
            }
        }
    }
}