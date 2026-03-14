package com.xianliticn.yuefu.pages.composition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.xianliticn.yuefu.R
import com.xianliticn.yuefu.ui.theme.YuefuTheme

@Composable
fun CompositionPage(viewModel: CompositionPageViewModel = hiltViewModel()) {

    val uiState by viewModel.uiState.collectAsState()

    CompositionPageContent(
        prompt = uiState.prompt ?: "",
        onPromptChange = { viewModel.handlePromptChange(it) },
        onSendClick = { viewModel.handleSendClick() },
        keys = uiState.keys,
        instruments = uiState.instruments,
        selectedKey = uiState.selectedKey ?: stringResource(R.string.unknown),
        onKeyChange = { viewModel.handleKeyChange(it) },
        selectedInstrument = uiState.selectedInstrument ?: stringResource(R.string.unknown),
        onInstrumentChange = { viewModel.handleInstrumentChange(it) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompositionPageContent(
    modifier: Modifier = Modifier,
    prompt: String = "",
    onPromptChange: (String) -> Unit = {},
    onSendClick: () -> Unit = {},
    keys: List<String> = emptyList(),
    instruments: List<String> = emptyList(),
    selectedKey: String = "C Major",
    onKeyChange: (String) -> Unit = {},
    selectedInstrument: String = "钢琴",
    onInstrumentChange: (String) -> Unit = {},
    playbackMode: Boolean = false
) {
    var keyExpanded by remember { mutableStateOf(false) }
    var instrumentExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = stringResource(R.string.composition_headline),
            style = MaterialTheme.typography.headlineMedium
        )
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
                FilledIconButton(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    onClick = { onSendClick() },
                    enabled = prompt.isNotBlank(),
                ) { Icon(Icons.AutoMirrored.Default.Send, null) }
            }
        )

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
    }
}

@Preview(showBackground = true, showSystemUi = true, locale = "en")
@Composable
fun CompositionPageContentPreview() {
    YuefuTheme {
        CompositionPageContent(
            prompt = "你好，生成一首音乐，要求是流行的，包含4个音符",
            onPromptChange = {},
            onSendClick = {}
        )
    }
}