package com.xianliticn.yuefu.pages

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xianliticn.yuefu.R

@Composable
fun SettingsPage(viewModel: SettingsPageViewModel) {
    SettingsPageContent()
}

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun SettingsPageContent(
    modifier: Modifier = Modifier
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
            .verticalScroll(rememberScrollState())
    ) {
        Image(
            modifier = Modifier.fillMaxWidth(),
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null
        )
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
                    text = "线粒体XianlitiCN",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            supportingContent = { Text("中南大学法学院") },
            trailingContent = {
                Image(
                    painter = painterResource(R.drawable.fyx),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(48.dp)
                )
            }
        )
        ListItem(
            headlineContent = {
                Text(
                    text = "Flutter-Misdreavus",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            supportingContent = { Text("中南大学计算机学院") },
            trailingContent = {
                Image(
                    painter = painterResource(R.drawable.ctm),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(48.dp)
                )
            }
        )
        ListItem(
            headlineContent = {
                Text(
                    text = "velixun",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            supportingContent = { Text("中南大学计算机学院") },
            trailingContent = {
                Image(
                    painter = painterResource(R.drawable.qhm),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(48.dp)
                )
            }
        )
        ListItem(
            headlineContent = {
                Text(
                    text = "QiChen",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            supportingContent = { Text("中南大学计算机学院") },
            trailingContent = {
                Image(
                    painter = painterResource(R.drawable.lcr),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(48.dp)
                )
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsPagePreview() {
    SettingsPageContent(modifier = Modifier.fillMaxSize())
}