package com.xianliticn.yuefu.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ChoiceGroup(
    modifier: Modifier = Modifier,
    options: List<String>,
    selectedIndex: IntArray, // 选中的索引数组
    onOptionClick: (Int) -> Unit,
    allowOther: Boolean = false,
    onOtherChange: (String) -> Unit = {},
    otherText: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp) // 选项之间的间距
    ) {
        options.forEachIndexed { index, text ->
            val isSelected = selectedIndex.contains(index)
            ChoiceItem(
                text = text,
                isSelected = isSelected,
                onClick = { onOptionClick(index) }
            )
        }

        // 如果允许“其他”选项，可以在这里添加一个特殊的输入框样式
        if (allowOther) {
            ChoiceItem(
                text = otherText ?: "其他...",
                isSelected = false,
                onClick = { /* 处理其他逻辑 */ },
                isEditable = true,
                onTextChange = onOtherChange
            )
        }
    }
}

@Composable
fun ChoiceItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isEditable: Boolean = false,
    onTextChange: (String) -> Unit = {}
) {
    // 动态颜色过渡动画
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.surface,
        label = "containerColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant,
        label = "borderColor"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 1.dp,
        label = "borderWidth"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(borderWidth, borderColor),
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧图标：单选框或勾选框的视觉暗示
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            if (!isEditable)
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 16.sp
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            else
                TextField(
                    value = text,
                    onValueChange = { onTextChange(it) }
                )

        }
    }
}