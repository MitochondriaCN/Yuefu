package com.xianliticn.yuefu.vo

enum class TaskStatus {
    PENDING,    // 等待中/文件处理中
    PROCESSING, // OMR识别中
    COMPLETED,  // 完成
    FAILED      // 失败
}