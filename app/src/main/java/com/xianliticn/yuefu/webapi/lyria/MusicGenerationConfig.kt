package com.xianliticn.yuefu.webapi.lyria

import kotlinx.serialization.Serializable

@Serializable
data class MusicGenerationConfig(
    val bpm: Int?,
    val scale: Scale?
)