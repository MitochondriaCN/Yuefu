package com.xianliticn.yuefu.vo

import kotlinx.serialization.Serializable

@Serializable
data class SubmitTaskVo(
    val message: String,
    val taskId: String,
    val status: TaskStatus
)
