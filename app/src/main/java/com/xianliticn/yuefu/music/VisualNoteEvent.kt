package com.xianliticn.yuefu.music

import androidx.compose.ui.graphics.Color

data class VisualNoteEvent(
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    /**
     * 白键索引，+0.5表示白键右上侧的黑键。
     */
    val keyIndex: Float,
    val color: Color
)