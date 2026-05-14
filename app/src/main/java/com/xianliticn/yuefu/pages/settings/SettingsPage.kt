package com.xianliticn.yuefu.pages.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwitchLeft
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xianliticn.yuefu.R
import com.xianliticn.yuefu.ui.components.EffectLevel
import com.xianliticn.yuefu.ui.theme.YuefuTheme

@Composable
fun SettingsPage(
    viewModel: SettingsPageViewModel,
    onAboutClick: () -> Unit = {}
) {
    val isTutorialShown by viewModel.isTutorialShown.collectAsState()
    val effectLevel by viewModel.effectLevel.collectAsState()

    SettingsPageContent(
        isTutorialShown = isTutorialShown,
        onTutorialShownChange = viewModel::setTutorialShown,
        effectLevel = effectLevel,
        onEffectLevelChange = viewModel::setEffectLevel,
        onAboutClick = onAboutClick,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun SettingsPageContent(
    isTutorialShown: Boolean,
    onTutorialShownChange: (Boolean) -> Unit,
    effectLevel: EffectLevel = EffectLevel.HIGH,
    onEffectLevelChange: (EffectLevel) -> Unit = {},
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (_: Exception) { null }

    // One-shot staggered animation
    var animationPlayed by rememberSaveable { mutableStateOf(false) }
    var titleVisible by remember { mutableStateOf(animationPlayed) }
    var generalVisible by remember { mutableStateOf(animationPlayed) }
    var aboutVisible by remember { mutableStateOf(animationPlayed) }

    LaunchedEffect(Unit) {
        if (!animationPlayed) {
            titleVisible = true
            kotlinx.coroutines.delay(100)
            generalVisible = true
            kotlinx.coroutines.delay(150)
            aboutVisible = true
            animationPlayed = true
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        AnimatedVisibility(
            visible = titleVisible,
            enter = fadeIn(tween(500)) + slideInVertically(
                animationSpec = tween(500),
                initialOffsetY = { it / 4 }
            )
        ) {
            Column {
                Spacer(Modifier.height(32.dp))
                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(24.dp))
            }
        }

        AnimatedVisibility(
            visible = generalVisible,
            enter = fadeIn(tween(500)) + slideInVertically(
                animationSpec = tween(500),
                initialOffsetY = { it / 4 }
            )
        ) {
            Column {
                // General Section
                SettingsSection(title = stringResource(R.string.general)) {
                    SettingsSwitchItem(
                        title = stringResource(R.string.auto_show_tutorial),
                        description = stringResource(R.string.auto_show_tutorial_desc),
                        icon = Icons.Default.Tune,
                        checked = isTutorialShown,
                        onCheckedChange = onTutorialShownChange
                    )
                }
                Spacer(Modifier.height(16.dp))

                SettingsSection(title = stringResource(R.string.visual_effects)) {
                    EffectLevelSelector(
                        currentLevel = effectLevel,
                        onLevelChange = onEffectLevelChange
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        AnimatedVisibility(
            visible = aboutVisible,
            enter = fadeIn(tween(500)) + slideInVertically(
                animationSpec = tween(500),
                initialOffsetY = { it / 4 }
            )
        ) {
            Column {
                // About Section
                SettingsSection(title = stringResource(R.string.about)) {
                    SettingsClickItem(
                        title = stringResource(R.string.about),
                        description = "${stringResource(R.string.app_name)} ${versionName ?: ""}",
                        icon = Icons.Default.Info,
                        onClick = onAboutClick
                    )
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 0.dp
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingsClickItem(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EffectLevelSelector(
    currentLevel: EffectLevel,
    onLevelChange: (EffectLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    val levels = listOf(EffectLevel.LOW, EffectLevel.MEDIUM, EffectLevel.HIGH)
    val labels = mapOf(
        EffectLevel.LOW to stringResource(R.string.effect_low),
        EffectLevel.MEDIUM to stringResource(R.string.effect_medium),
        EffectLevel.HIGH to stringResource(R.string.effect_high)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.effect_level),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = stringResource(R.string.effect_level_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            levels.forEach { level ->
                val selected = level == currentLevel
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onLevelChange(level) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = labels[level] ?: "",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun SettingsPagePreview() {
    YuefuTheme {
        SettingsPageContent(
            onAboutClick = {},
            modifier = Modifier.fillMaxSize(),
            isTutorialShown = true,
            onTutorialShownChange = {},
            effectLevel = EffectLevel.HIGH,
            onEffectLevelChange = {}
        )
    }
}
