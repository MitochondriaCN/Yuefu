package com.xianliticn.yuefu.pages.about

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xianliticn.yuefu.R
import com.xianliticn.yuefu.modules.NetworkModule
import com.xianliticn.yuefu.ui.theme.BlueAccentContainer
import com.xianliticn.yuefu.ui.theme.ErrorRed
import com.xianliticn.yuefu.ui.theme.SuccessGreen
import com.xianliticn.yuefu.ui.theme.YellowContainer

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

    // One-shot staggered animation
    var animationPlayed by rememberSaveable { mutableStateOf(false) }
    var headerVisible by remember { mutableStateOf(animationPlayed) }
    var devVisible by remember { mutableStateOf(animationPlayed) }
    var statusVisible by remember { mutableStateOf(animationPlayed) }

    LaunchedEffect(Unit) {
        if (!animationPlayed) {
            headerVisible = true
            kotlinx.coroutines.delay(100)
            devVisible = true
            kotlinx.coroutines.delay(150)
            statusVisible = true
            animationPlayed = true
        }
    }

    val developers = listOf("线粒体", "发发", "冰寻卿", "柒晨")
    val devColors = listOf(
        BlueAccentContainer,
        YellowContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.secondaryContainer
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        AnimatedVisibility(
            visible = headerVisible,
            enter = fadeIn(tween(500)) + slideInVertically(
                animationSpec = tween(500),
                initialOffsetY = { it / 4 }
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(40.dp))
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.size(100.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            modifier = Modifier.size(64.dp),
                            painter = painterResource(R.mipmap.ic_launcher_foreground),
                            contentDescription = null
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "v${versionName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(32.dp))
            }
        }

        AnimatedVisibility(
            visible = devVisible,
            enter = fadeIn(tween(500)) + slideInVertically(
                animationSpec = tween(500),
                initialOffsetY = { it / 4 }
            )
        ) {
            Column {
                Text(
                    text = stringResource(R.string.developers),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp
                ) {
                    Column {
                        developers.forEachIndexed { index, name ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = devColors[index % devColors.size],
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(Modifier.width(14.dp))
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        AnimatedVisibility(
            visible = statusVisible,
            enter = fadeIn(tween(500)) + slideInVertically(
                animationSpec = tween(500),
                initialOffsetY = { it / 4 }
            )
        ) {
            Column {
                Text(
                    text = stringResource(R.string.backend_status),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        color = if (backendOnline) SuccessGreen else ErrorRed,
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                text = NetworkModule.BASE_URL,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.timestamp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = backendTimestamp ?: "-",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.cache_size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = backendTmpSize ?: "-",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
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
