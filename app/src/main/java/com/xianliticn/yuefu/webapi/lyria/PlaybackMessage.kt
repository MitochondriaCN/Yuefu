package com.xianliticn.yuefu.webapi.lyria

import kotlinx.serialization.Serializable

@Serializable
data class PlaybackMessage(
    val playbackControl: PlaybackControl
)
