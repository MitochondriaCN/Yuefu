package com.xianliticn.yuefu.pages.composition

import okio.ByteString
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

interface LyriaMessage {
    fun getMessageString(): String
}

data class PromptMessage(
    val prompt: String,
) : LyriaMessage {

    override fun getMessageString(): String {
        return prompt
    }
}

data class BinaryResponseMessage(
    val data: ByteString,
) : LyriaMessage {
    /**
     * 数据大小（自动转换为友好单位）
     */
    override fun getMessageString(): String {
        return formatFileSize(data.size.toLong())
    }

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
    }
}
