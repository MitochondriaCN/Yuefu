package com.xianliticn.yuefu.webapi.lyria

import kotlinx.serialization.Serializable

@Serializable
data class WeightedPrompt(
    val text: String,
    val weight: Double
)
