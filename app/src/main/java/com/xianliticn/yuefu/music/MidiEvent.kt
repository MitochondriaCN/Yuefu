package com.xianliticn.yuefu.music

data class MidiEvent(
    /**
     * 音高
     */
    val pitch: Int,
    /**
     * 开始时间（ms）
     */
    val time: Long,
    /**
     * 音符类型
     */
    val note: Note
)