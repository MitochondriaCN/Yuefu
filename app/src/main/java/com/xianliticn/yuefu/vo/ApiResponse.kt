package com.xianliticn.yuefu.vo

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val code: Int,
    val msg: String,
    val data: T?
)