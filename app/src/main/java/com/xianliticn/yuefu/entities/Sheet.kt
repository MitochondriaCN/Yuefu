package com.xianliticn.yuefu.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Sheet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    /**
     * OMR任务ID（UUID）
     */
    @ColumnInfo(name = "task_id") val taskId: String,
    /**
     * 是否已下载
     */
    @ColumnInfo(name = "is_downloaded") val isDownloaded: Boolean = false,
    /**
     * 文件名
     */
    @ColumnInfo(name = "file_name") val fileName: String? = null,
    /**
     * 乐谱名（标题）
     */
    @ColumnInfo(name = "sheet_name") val sheetName: String? = null,
    /**
     * 创建时间，毫秒时间戳
     */
    @ColumnInfo(name = "create_time") val createTime: Long,
    /**
     * 最后一次打开时间，毫秒时间戳
     */
    @ColumnInfo(name = "last_open_time") val lastOpenTime: Long? = null,
    /**
     * 文件哈希，SHA-256算法
     */
    @ColumnInfo(name = "hash") val hash: String? = null
)