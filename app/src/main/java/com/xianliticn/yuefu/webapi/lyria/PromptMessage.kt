package com.xianliticn.yuefu.webapi.lyria

import kotlinx.serialization.Serializable

@Serializable
data class PromptMessage(
    val clientContent: ClientContent
)
