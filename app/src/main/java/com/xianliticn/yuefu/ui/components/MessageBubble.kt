package com.xianliticn.yuefu.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xianliticn.yuefu.ui.theme.YuefuTheme

/**
 * 简约风格的对话气泡组件
 *
 * @param label 角色标签，如 "AI" 或 "你"
 * @param message 消息文本内容
 * @param isFromUser 是否为用户发送，决定气泡对齐方向和颜色
 * @param supportingContent 额外支撑内容（如图片、卡片、操作按钮等）
 */
@Composable
fun MessageBubble(
    modifier: Modifier = Modifier,
    label: String = "",
    message: String,
    isFromUser: Boolean = false,
    supportingContent: @Composable () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 12.dp),
        horizontalAlignment = if (isFromUser) Alignment.End else Alignment.Start
    ) {
        // 标签：如 "AI" 或 "你"
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp, end = 4.dp)
            )
        }

        // 气泡主体
        Surface(
            color = if (isFromUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isFromUser) 16.dp else 4.dp,
                bottomEnd = if (isFromUser) 4.dp else 16.dp
            ),
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .widthIn(max = 280.dp) // 限制气泡最大宽度
            ) {
                if (message.isNotEmpty()) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isFromUser) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                // 渲染辅助内容
                Box(modifier = Modifier.padding(top = if (message.isNotEmpty()) 4.dp else 0.dp)) {
                    supportingContent()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MessageBubblePreview() {
    YuefuTheme {
        Column {
            MessageBubble(
                label = "AI 助手",
                message = "你好！我是你的乐府 AI 助手。有什么我可以帮你的吗？",
                isFromUser = false
            )
            MessageBubble(
                label = "我",
                message = "帮我写一首关于秋天的诗。",
                isFromUser = true
            )
            MessageBubble(
                label = "AI 助手",
                message = "没问题，这是为您创作的诗句：",
                isFromUser = false,
                supportingContent = {
                    Text(
                        "落木萧萧下，长天共一色。\n秋风吹不尽，总是玉关情。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            )
        }
    }
}