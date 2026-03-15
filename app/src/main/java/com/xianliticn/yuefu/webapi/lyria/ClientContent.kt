package com.xianliticn.yuefu.webapi.lyria

import kotlinx.serialization.Serializable

@Serializable
data class ClientContent(
    val weightedPrompts: List<WeightedPrompt>
)
