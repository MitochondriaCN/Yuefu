package com.xianliticn.yuefu.pages.about

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xianliticn.yuefu.R
import com.xianliticn.yuefu.modules.NetworkModule
import com.xianliticn.yuefu.ui.components.scorePaperTexture
import com.xianliticn.yuefu.ui.theme.Ochre

@Composable
fun AboutPage(viewModel: AboutPageViewModel) {
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    val uiState = viewModel.uiState.collectAsState()

    AboutPageContent(
        backendOnline = uiState.value.backendOnline,
        backendTimestamp = uiState.value.backendTimestamp,
        backendTmpSize = uiState.value.backendTmpSize
    )
}

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun AboutPageContent(
    modifier: Modifier = Modifier,
    backendOnline: Boolean,
    backendTimestamp: String? = null,
    backendTmpSize: String? = null
) {
    val context = LocalContext.current
    val versionName = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (_: Exception) {
            context.getString(R.string.unknown)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .scorePaperTexture(
                color = MaterialTheme.colorScheme.onSurface,
                alpha = 0.04f,
            )
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(32.dp))
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(112.dp)
                .border(1.dp, Ochre.copy(alpha = 0.45f), CircleShape)
                .padding(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.app_name),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = versionName.toString(),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.developers),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.primary
        )
        ListItem(
            headlineContent = {
                Text(
                    text = "线粒体",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            trailingContent = {
                Image(
                    painter = painterResource(R.drawable.ctm),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(1.dp, Ochre.copy(alpha = 0.32f), CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        )
        ListItem(
            headlineContent = {
                Text(
                    text = "发发",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            trailingContent = {
                Image(
                    painter = painterResource(R.drawable.fyx),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(1.dp, Ochre.copy(alpha = 0.32f), CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        )
        ListItem(
            headlineContent = {
                Text(
                    text = "冰寻卿",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            trailingContent = {
                Image(
                    painter = painterResource(R.drawable.lcr),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(1.dp, Ochre.copy(alpha = 0.32f), CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        )
        ListItem(
            headlineContent = {
                Text(
                    text = "柒晨",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            trailingContent = {
                Image(
                    painter = painterResource(R.drawable.qhm),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(1.dp, Ochre.copy(alpha = 0.32f), CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        )
        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.backend_status),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (backendOnline) Color.Green else Color.Red,
                        shape = CircleShape
                    )
            )
            Text(
                text = NetworkModule.BASE_URL,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Text(
            text = "${stringResource(R.string.timestamp)}${backendTimestamp ?: "-"}",
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Text(
            text = "${stringResource(R.string.cache_size)}${backendTmpSize ?: "-"}",
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun AboutPagePreview() {
    AboutPageContent(
        modifier = Modifier.fillMaxSize(),
        backendOnline = true,
        backendTimestamp = "2023-05-05 12:00:00",
        backendTmpSize = "28 MB",
    )
}