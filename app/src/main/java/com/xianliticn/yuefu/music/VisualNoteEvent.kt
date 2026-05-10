package com.xianliticn.yuefu.music

import androidx.compose.ui.graphics.Color

data class VisualNoteEvent(
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    /**
     * 白键索引，+0.5表示白键右上侧的黑键。钢琴共有52白键，0代表第一个白键A0。
     */
    val keyIndex: Float,
    val color: Color,
    /**
     * 声部标识，用于区分不同声部的纹理样式
     */
    val partId: Int = 0
) {
    /**
     * 音符时长（毫秒）
     */
    val durationMillis: Long get() = endTimeMillis - startTimeMillis

    /**
     * 是否为长音符（≥ 200ms）
     */
    val isLongNote: Boolean get() = durationMillis >= 200
}