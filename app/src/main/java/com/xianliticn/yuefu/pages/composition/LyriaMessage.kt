package com.xianliticn.yuefu.pages.composition

import okio.ByteString
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

interface LyriaMessage {
    fun getMessageString(): String
    fun getTimestampMillis(): Long
}

data class PromptMessage(
    val prompt: String,
    val timestamp: Long = System.currentTimeMillis(),
) : LyriaMessage {

    override fun getMessageString(): String {
        return prompt
    }

    override fun getTimestampMillis(): Long {
        return timestamp
    }
}

data class BinaryResponseMessage(
    val data: ByteString,
    val timestamp: Long = System.currentTimeMillis(),
) : LyriaMessage {
    /**
     * 数据大小（自动转换为友好单位）
     */
    override fun getMessageString(): String {
        return formatFileSize(data.size.toLong())
    }

    override fun getTimestampMillis(): Long {
        return timestamp
    }

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
    }
}
