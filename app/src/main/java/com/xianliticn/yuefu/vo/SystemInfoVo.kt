package com.xianliticn.yuefu.vo

import kotlinx.serialization.Serializable

@Serializable
data class SystemInfoVo (
    val time: Long? = null,
    val tmpSize: Long? = null
)
