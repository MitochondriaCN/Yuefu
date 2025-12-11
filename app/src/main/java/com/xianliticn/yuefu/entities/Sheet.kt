package com.xianliticn.yuefu.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Sheet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "sheet_name") val sheetName: String,
    /**
     * 最后一次打开时间，毫秒时间戳。
     */
    @ColumnInfo(name = "last_open_time") val lastOpenTime: Long,
    /**
     * 文件哈希，SHA-256算法。
     */
    @ColumnInfo(name = "hash") val hash: String
)