package com.xianliticn.yuefu.pages.composition

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.xianliticn.yuefu.R
import com.xianliticn.yuefu.ui.theme.AccentAmber
import com.xianliticn.yuefu.ui.theme.AccentAmberSoft
import com.xianliticn.yuefu.ui.theme.BgLight
import com.xianliticn.yuefu.ui.theme.Bronze
import com.xianliticn.yuefu.ui.theme.InkBrown
import com.xianliticn.yuefu.ui.theme.InkSoft
import com.xianliticn.yuefu.ui.theme.Moonlight
import com.xianliticn.yuefu.ui.theme.Ochre
import com.xianliticn.yuefu.ui.theme.PaleOchre
import com.xianliticn.yuefu.ui.theme.YuefuTheme
import okio.Buffer
import okio.ByteString
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun CompositionPage(viewModel: CompositionPageViewModel = hiltViewModel()) {

    val uiState by viewModel.uiState.collectAsState()

    CompositionPageContent(
        prompt = uiState.prompt ?: "",
        onPromptChange = { viewModel.handlePromptChange(it) },
        onSendClick = { viewModel.handleSendClick() },
        onStopClick = { viewModel.handleStopClick() },
        keys = uiState.keys,
        instruments = uiState.instruments,
        selectedKey = uiState.selectedKey ?: stringResource(R.string.unknown),
        onKeyChange = { viewModel.handleKeyChange(it) },
        selectedInstrument = uiState.selectedInstrument ?: stringResource(R.string.unknown),
        onInstrumentChange = { viewModel.handleInstrumentChange(it) },
        messages = compactMessages(uiState.messages),
        audioLevel = uiState.audioLevel,
        spectrum = uiState.spectrum
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompositionPageContent(
    modifier: Modifier = Modifier,
    prompt: String = "",
    onPromptChange: (String) -> Unit = {},
    onSendClick: () -> Unit = {},
    onStopClick: () -> Unit = {},
    keys: List<String> = emptyList(),
    instruments: List<String> = emptyList(),
    selectedKey: String = "C Major",
    onKeyChange: (String) -> Unit = {},
    selectedInstrument: String = "钢琴",
    onInstrumentChange: (String) -> Unit = {},
    messages: List<LyriaMessage> = emptyList(),
    audioLevel: Float = 0f,
    spectrum: List<Float> = emptyList()
) {
    var keyExpanded by remember { mutableStateOf(false) }
    var instrumentExpanded by remember { mutableStateOf(false) }

    if (messages.isNotEmpty()) {
        PlayingNowScreen(
            modifier = modifier.fillMaxSize(),
            onStopClick = onStopClick,
            audioLevel = audioLevel,
            spectrum = spectrum
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Spacer(Modifier.height(40.dp))

        Text(
            text = stringResource(R.string.composition_headline),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            value = prompt,
            onValueChange = onPromptChange,
            label = { Text(stringResource(R.string.prompt)) },
            placeholder = { Text(stringResource(R.string.composition_prompt_placeholder)) },
            trailingIcon = {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    FilledIconButton(
                        onClick = { onSendClick() },
                        enabled = prompt.isNotBlank(),
                    ) { Icon(Icons.AutoMirrored.Default.Send, null) }
                }
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = keyExpanded,
                onExpandedChange = { keyExpanded = !keyExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedKey,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.key)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = keyExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.menuAnchor(
                        ExposedDropdownMenuAnchorType.PrimaryEditable,
                        keys.isNotEmpty()
                    ),
                    singleLine = true
                )
                ExposedDropdownMenu(
                    expanded = keyExpanded,
                    onDismissRequest = { keyExpanded = false }
                ) {
                    keys.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                onKeyChange(selectionOption)
                                keyExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = instrumentExpanded,
                onExpandedChange = { instrumentExpanded = !instrumentExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedInstrument,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.instrument)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = instrumentExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.menuAnchor(
                        ExposedDropdownMenuAnchorType.PrimaryEditable,
                        instruments.isNotEmpty()
                    ),
                    singleLine = true
                )
                ExposedDropdownMenu(
                    expanded = instrumentExpanded,
                    onDismissRequest = { instrumentExpanded = false }
                ) {
                    instruments.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                onInstrumentChange(selectionOption)
                                instrumentExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun PlayingNowScreen(
    modifier: Modifier = Modifier,
    onStopClick: () -> Unit = {},
    audioLevel: Float = 0f,
    spectrum: List<Float> = emptyList()
) {
    val level by animateFloatAsState(
        targetValue = audioLevel,
        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
        label = "audioLevel"
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgLight)
            .drawBehind {
                val mainRadius = size.minDimension * 0.95f
                val glowAlpha = 0.18f + level * 0.3f
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AccentAmber.copy(alpha = glowAlpha),
                            Color.Transparent
                        ),
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.22f),
                        radius = mainRadius
                    )
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            PaleOchre.copy(alpha = 0.35f + level * 0.2f),
                            Color.Transparent
                        ),
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.86f),
                        radius = size.minDimension * 0.9f
                    )
                )
            }
    ) {
        NoiseVeil(modifier = Modifier.matchParentSize(), intensity = 0.05f)
        FlowingRibbons(modifier = Modifier.matchParentSize(), audioLevel = level)

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 22.dp)
                .fillMaxWidth()
                .height(360.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Moonlight,
                            PaleOchre.copy(alpha = 0.5f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Ochre.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(vertical = 26.dp, horizontal = 26.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.now_playing),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                    color = InkBrown,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.ai_generating),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSoft,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.weight(1f))
                SpectrumBars(audioLevel = level, spectrum = spectrum)
            }
        }

        CircularPlayButton(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp),
            onClick = onStopClick
        )
    }
}

@Composable
private fun SpectrumBars(
    modifier: Modifier = Modifier,
    barWidth: Dp = 5.dp,
    maxHeight: Dp = 170.dp,
    audioLevel: Float = 0f,
    spectrum: List<Float> = emptyList()
) {
    val transition = rememberInfiniteTransition(label = "spectrum")
    val heights = List(16) { index ->
        transition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 860 + index * 70,
                    delayMillis = index * 40,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar$index"
        )
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        heights.forEachIndexed { index, anim ->
            val band = spectrum.getOrNull(index) ?: audioLevel
            val energy = (0.08f + band * 0.92f).coerceIn(0.08f, 1f)
            val shimmer = 0.55f + 0.45f * anim.value
            val barHeight = (12f + (maxHeight.value - 12f) * energy * shimmer).dp
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(barHeight)
                    .shadow(
                        elevation = (6f + 10f * audioLevel).dp,
                        shape = RoundedCornerShape(50),
                        ambientColor = Bronze.copy(alpha = 0.25f),
                        spotColor = Bronze.copy(alpha = 0.3f)
                    )
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                PaleOchre,
                                Ochre,
                                Bronze
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun FlowingRibbons(
    modifier: Modifier = Modifier,
    audioLevel: Float = 0f
) {
    val transition = rememberInfiniteTransition(label = "ribbons")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase"
    )

    val pulse = 0.8f + audioLevel * 0.5f

    Canvas(modifier = modifier) {
        val yShift = (sin(phase * 2f * PI).toFloat()) * size.height * 0.05f * pulse
        val leftPath = Path().apply {
            moveTo(size.width * 0.05f, size.height * 0.35f + yShift)
            cubicTo(
                size.width * 0.25f, size.height * 0.22f + yShift,
                size.width * 0.45f, size.height * 0.5f + yShift,
                size.width * 0.9f, size.height * 0.38f + yShift
            )
        }
        val rightPath = Path().apply {
            moveTo(size.width * 0.1f, size.height * 0.65f - yShift)
            cubicTo(
                size.width * 0.35f, size.height * 0.85f - yShift,
                size.width * 0.6f, size.height * 0.45f - yShift,
                size.width * 0.95f, size.height * 0.62f - yShift
            )
        }

        drawPath(
            path = leftPath,
            brush = Brush.linearGradient(
                listOf(
                    AccentAmberSoft.copy(alpha = 0.10f + audioLevel * 0.25f),
                    Color.Transparent
                )
            ),
            style = Stroke(width = size.minDimension * 0.012f * pulse, cap = StrokeCap.Round)
        )
        drawPath(
            path = rightPath,
            brush = Brush.linearGradient(
                listOf(
                    Ochre.copy(alpha = 0.10f + audioLevel * 0.22f),
                    Color.Transparent
                )
            ),
            style = Stroke(width = size.minDimension * 0.014f * pulse, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun NoiseVeil(
    modifier: Modifier = Modifier,
    intensity: Float = 0.08f
) {
    Canvas(modifier = modifier) {
        val step = 14f
        val alpha = intensity.coerceIn(0f, 0.2f)
        val color = Ochre.copy(alpha = alpha * 0.6f)
        var y = 0f
        while (y < size.height) {
            var x = 0f
            while (x < size.width) {
                val seed = ((x * 0.13f + y * 0.19f) % 1f)
                if (seed > 0.55f) {
                    drawCircle(
                        color = color,
                        radius = 0.8f,
                        center = androidx.compose.ui.geometry.Offset(x, y)
                    )
                }
                x += step
            }
            y += step
        }
    }
}

@Composable
private fun CircularPlayButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .shadow(8.dp, RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.linearGradient(listOf(Ochre, Bronze))
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Stop,
            contentDescription = stringResource(R.string.stop),
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

/**
 * 将连续的BinaryResponseMessage合并为一个
 */
private fun compactMessages(messages: List<LyriaMessage>): List<LyriaMessage> {
    if (messages.isEmpty()) return emptyList()

    val result = mutableListOf<LyriaMessage>()
    var currentBinary: BinaryResponseMessage? = null

    for (message in messages) {
        if (message is BinaryResponseMessage) {
            if (currentBinary == null) {
                currentBinary = message
            } else {
                // 使用 Buffer 来合并 ByteString
                val combined =
                    Buffer().write(currentBinary.data).write(message.data).readByteString()
                currentBinary =
                    currentBinary.copy(data = combined, timestamp = System.currentTimeMillis())
            }
        } else {
            if (currentBinary != null) {
                result.add(currentBinary)
                currentBinary = null
            }
            result.add(message)
        }
    }

    if (currentBinary != null) {
        result.add(currentBinary)
    }

    return result
}

@Preview(showBackground = true, showSystemUi = true, locale = "en")
@Composable
fun CompositionPageContentPreview() {
    YuefuTheme {
        CompositionPageContent(
            prompt = "你好，生成一首音乐，要求是流行的，包含4个音符",
            onPromptChange = {},
            onSendClick = {},
            messages = listOf(
                PromptMessage("你好，生成一首音乐，要求是流行的，包含4个音符"),
                BinaryResponseMessage(ByteString.EMPTY)
            ),
            audioLevel = 0.6f
        )
    }
}
