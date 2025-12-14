package com.xianliticn.yuefu.music

class MidiEvent(
    /**
     * 音高
     */
    val pitch: Int,
    /**
     * 开始时间（ns）
     */
    val timeNano: Long,
    /**
     * 音符类型
     */
    val note: Note,
    /**
     * 是否已发送
     */
    var isSent: Boolean
) {
    fun getMidiData(): ByteArray =
        when (note) {
            Note.PRESS ->
                byteArrayOf(0x90.toByte(), pitch.toByte(), 127.toByte())

            Note.RELEASE ->
                byteArrayOf(0x80.toByte(), pitch.toByte(), 0.toByte())
        }
}