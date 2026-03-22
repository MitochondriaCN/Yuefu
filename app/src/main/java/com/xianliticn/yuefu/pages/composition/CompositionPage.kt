package com.xianliticn.yuefu.pages.composition

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Square
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.xianliticn.yuefu.R
import com.xianliticn.yuefu.ui.components.MessageBubble
import com.xianliticn.yuefu.ui.theme.YuefuTheme
import kotlinx.coroutines.delay
import okio.Buffer
import okio.ByteString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    messages: List<LyriaMessage> = emptyList()
) {
    var keyExpanded by remember { mutableStateOf(false) }
    var instrumentExpanded by remember { mutableStateOf(false) }
    val currentTime by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(1000)
            value = System.currentTimeMillis()
        }
    }

    Column(
        modifier = if (messages.isNotEmpty())
            modifier
                .fillMaxSize()
                .animateContentSize()
                .verticalScroll(rememberScrollState())
        else
            modifier
                .fillMaxWidth()
                .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Spacer(Modifier.height(40.dp))

        if (messages.isEmpty())
            Text(
                text = stringResource(R.string.composition_headline),
                style = MaterialTheme.typography.headlineMedium
            )

        for (message in messages) {
            val label = SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(Date(message.getTimestampMillis()))
            val messageText = when (message) {
                is PromptMessage -> message.getMessageString()
                is BinaryResponseMessage -> message.getMessageString()
                else -> stringResource(R.string.unknown)
            }
            val isUserMessage = when (message) {
                is PromptMessage -> true
                else -> false
            }

            MessageBubble(
                label = label,
                message = messageText,
                isFromUser = isUserMessage,
                supportingContent = {
                    // 如果是当前列表中最后一条消息
                    // 就转圈
                    if (messages.last() == message)
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            if (currentTime - message.getTimestampMillis() > 1000 * 10)
                                Text(
                                    text = stringResource(R.string.this_may_take_a_while),
                                    style = MaterialTheme.typography.bodySmall
                                )
                        }
                }
            )
        }

        if (messages.isNotEmpty())
            Spacer(Modifier.weight(1f))

        Spacer(Modifier.height(40.dp))

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
                    if (messages.isNotEmpty())
                        FilledIconButton(
                            onClick = { onStopClick() },
                        ) { Icon(Icons.Default.Square, null) }
                }
            }
        )

        if (messages.isEmpty())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp) // 两个组件之间的间距
            ) {
                // 调式选择
                ExposedDropdownMenuBox(
                    expanded = keyExpanded,
                    onExpandedChange = { keyExpanded = !keyExpanded },
                    modifier = Modifier.weight(1f) // 平分空间
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

                // 乐器选择
                ExposedDropdownMenuBox(
                    expanded = instrumentExpanded,
                    onExpandedChange = { instrumentExpanded = !instrumentExpanded },
                    modifier = Modifier.weight(1f) // 平分空间
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
            )
        )
    }
}
