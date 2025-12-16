package com.xianliticn.yuefu.music

/**
 * 钢琴键盘可视化事件。
 */
data class VisualEvent(
    /**
     * 音高
     */
    val pitch: Int,
    /**
     * 声部
     */
    val part: Int,
    /**
     * 开始时间（ns）
     */
    val timeNano: Long,
    /**
     * 持续时间（ms）
     */
    val duration: Long
)